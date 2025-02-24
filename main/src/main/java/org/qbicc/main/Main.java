package org.qbicc.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import org.jboss.shrinkwrap.resolver.api.ResolutionException;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolvedArtifact;
import org.jboss.shrinkwrap.resolver.api.maven.PackagingType;
import org.jboss.shrinkwrap.resolver.api.maven.ScopeType;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinates;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenDependencies;
import org.qbicc.context.CompilationContext;
import org.qbicc.context.Diagnostic;
import org.qbicc.context.DiagnosticContext;
import org.qbicc.driver.BaseDiagnosticContext;
import org.qbicc.driver.BuilderStage;
import org.qbicc.driver.ClassPathElement;
import org.qbicc.driver.ClassPathItem;
import org.qbicc.driver.Driver;
import org.qbicc.driver.ElementBodyCopier;
import org.qbicc.driver.ElementBodyCreator;
import org.qbicc.driver.ElementInitializer;
import org.qbicc.driver.ElementVisitorAdapter;
import org.qbicc.driver.GraphGenConfig;
import org.qbicc.driver.Phase;
import org.qbicc.driver.plugin.DriverPlugin;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmThread;
import org.qbicc.interpreter.impl.VmImpl;
import org.qbicc.machine.arch.Platform;
import org.qbicc.machine.object.ObjectFileProvider;
import org.qbicc.machine.probe.CProbe;
import org.qbicc.machine.tool.CToolChain;
import org.qbicc.plugin.coreclasses.BasicInitializationBasicBlockBuilder;
import org.qbicc.plugin.coreclasses.BasicInitializationManualInitializer;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.constants.ConstantBasicBlockBuilder;
import org.qbicc.plugin.conversion.LLVMCompatibleBasicBlockBuilder;
import org.qbicc.plugin.conversion.MethodCallFixupBasicBlockBuilder;
import org.qbicc.plugin.conversion.NumericalConversionBasicBlockBuilder;
import org.qbicc.plugin.core.CoreAnnotationTypeBuilder;
import org.qbicc.plugin.correctness.RuntimeChecksBasicBlockBuilder;
import org.qbicc.plugin.dispatch.DevirtualizingBasicBlockBuilder;
import org.qbicc.plugin.dispatch.DispatchTableEmitter;
import org.qbicc.plugin.dispatch.DispatchTableBuilder;
import org.qbicc.plugin.dot.DotGenerator;
import org.qbicc.plugin.gc.nogc.NoGcBasicBlockBuilder;
import org.qbicc.plugin.gc.nogc.NoGcMultiNewArrayBasicBlockBuilder;
import org.qbicc.plugin.gc.nogc.NoGcSetupHook;
import org.qbicc.plugin.gc.nogc.NoGcTypeSystemConfigurator;
import org.qbicc.plugin.instanceofcheckcast.InstanceOfCheckCastBasicBlockBuilder;
import org.qbicc.plugin.instanceofcheckcast.SupersDisplayBuilder;
import org.qbicc.plugin.instanceofcheckcast.SupersDisplayEmitter;
import org.qbicc.plugin.intrinsics.IntrinsicBasicBlockBuilder;
import org.qbicc.plugin.intrinsics.core.CoreIntrinsics;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.llvm.LLVMDefaultModuleCompileStage;
import org.qbicc.plugin.llvm.LLVMIntrinsics;
import org.qbicc.plugin.lowering.LocalVariableFindingBasicBlockBuilder;
import org.qbicc.plugin.lowering.LocalVariableLoweringBasicBlockBuilder;
import org.qbicc.plugin.layout.ObjectAccessLoweringBuilder;
import org.qbicc.plugin.linker.LinkStage;
import org.qbicc.plugin.llvm.LLVMCompileStage;
import org.qbicc.plugin.llvm.LLVMGenerator;
import org.qbicc.plugin.lowering.BooleanAccessBasicBlockBuilder;
import org.qbicc.plugin.lowering.FunctionLoweringElementHandler;
import org.qbicc.plugin.lowering.InvocationLoweringBasicBlockBuilder;
import org.qbicc.plugin.lowering.StaticFieldLoweringBasicBlockBuilder;
import org.qbicc.plugin.lowering.ThrowExceptionHelper;
import org.qbicc.plugin.lowering.ThrowLoweringBasicBlockBuilder;
import org.qbicc.plugin.lowering.VMHelpersSetupHook;
import org.qbicc.plugin.main_method.AddMainClassHook;
import org.qbicc.plugin.main_method.MainMethod;
import org.qbicc.plugin.methodinfo.MethodDataEmitter;
import org.qbicc.plugin.native_.ConstTypeResolver;
import org.qbicc.plugin.native_.ConstantDefiningBasicBlockBuilder;
import org.qbicc.plugin.native_.ExternExportTypeBuilder;
import org.qbicc.plugin.native_.FunctionTypeResolver;
import org.qbicc.plugin.native_.InternalNativeTypeResolver;
import org.qbicc.plugin.native_.NativeBasicBlockBuilder;
import org.qbicc.plugin.native_.NativeBindingMethodConfigurator;
import org.qbicc.plugin.native_.NativeTypeBuilder;
import org.qbicc.plugin.native_.NativeTypeResolver;
import org.qbicc.plugin.native_.PointerBasicBlockBuilder;
import org.qbicc.plugin.native_.PointerTypeResolver;
import org.qbicc.plugin.native_.StructMemberAccessBasicBlockBuilder;
import org.qbicc.plugin.objectmonitor.ObjectMonitorBasicBlockBuilder;
import org.qbicc.plugin.opt.GotoRemovingVisitor;
import org.qbicc.plugin.opt.InitializedStaticFieldBasicBlockBuilder;
import org.qbicc.plugin.opt.LocalMemoryTrackingBasicBlockBuilder;
import org.qbicc.plugin.opt.InliningBasicBlockBuilder;
import org.qbicc.plugin.opt.PhiOptimizerVisitor;
import org.qbicc.plugin.opt.SimpleOptBasicBlockBuilder;
import org.qbicc.plugin.reachability.ReachabilityInfo;
import org.qbicc.plugin.reachability.ReachabilityBlockBuilder;
import org.qbicc.plugin.serialization.ClassObjectSerializer;
import org.qbicc.plugin.serialization.MethodDataStringsSerializer;
import org.qbicc.plugin.serialization.ObjectLiteralSerializingVisitor;
import org.qbicc.plugin.threadlocal.ThreadLocalBasicBlockBuilder;
import org.qbicc.plugin.threadlocal.ThreadLocalTypeBuilder;
import org.qbicc.plugin.trycatch.LocalThrowHandlingBasicBlockBuilder;
import org.qbicc.plugin.trycatch.SynchronizedMethodBasicBlockBuilder;
import org.qbicc.plugin.verification.ClassInitializingBasicBlockBuilder;
import org.qbicc.plugin.verification.ClassLoadingBasicBlockBuilder;
import org.qbicc.plugin.verification.LowerVerificationBasicBlockBuilder;
import org.qbicc.plugin.verification.MemberResolvingBasicBlockBuilder;
import org.qbicc.tool.llvm.LlvmToolChain;
import org.qbicc.type.TypeSystem;
import io.smallrye.common.constraint.Assert;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.LogManager;
import org.jboss.logmanager.Logger;
import picocli.CommandLine;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.ParseResult;

/**
 * The main entry point, which can be constructed using a builder or directly invoked.
 */
public class Main implements Callable<DiagnosticContext> {
    private final List<ClassPathEntry> bootPaths;
    private final List<ClassPathEntry> appPaths;
    private final Path outputPath;
    private final Consumer<Iterable<Diagnostic>> diagnosticsHandler;
    private final String mainClass;
    private final String gc;
    private final boolean isPie;
    private final GraphGenConfig graphGenConfig;
    private final boolean optMemoryTracking;
    private final boolean optPhis;
    private final boolean optGotos;
    private final boolean optInlining;
    private final Platform platform;
    private final boolean smallTypeIds;

    Main(Builder builder) {
        ArrayList<ClassPathEntry> bootPaths = new ArrayList<>(builder.bootPathsPrepend.size() + 1 + builder.bootPathsAppend.size());
        bootPaths.addAll(builder.bootPathsPrepend);
        bootPaths.add(ClassPathEntry.ofClassLibraries(builder.classLibVersion));
        bootPaths.addAll(builder.bootPathsAppend);
        this.bootPaths = bootPaths;
        appPaths = List.copyOf(builder.appPaths);
        outputPath = builder.outputPath;
        diagnosticsHandler = builder.diagnosticsHandler;
        // todo: this becomes optional
        mainClass = Assert.checkNotNullParam("builder.mainClass", builder.mainClass);
        gc = builder.gc;
        isPie = builder.isPie;
        graphGenConfig = builder.graphGenConfig;
        optMemoryTracking = builder.optMemoryTracking;
        optInlining = builder.optInlining;
        optPhis = builder.optPhis;
        optGotos = builder.optGotos;
        platform = builder.platform;
        smallTypeIds = builder.smallTypeIds;
    }

    public DiagnosticContext call() {
        BaseDiagnosticContext ctxt = new BaseDiagnosticContext();
        try {
            call0(ctxt);
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            ctxt.error(t, "Compilation failed due to an exception");
        }
        diagnosticsHandler.accept(ctxt.getDiagnostics());
        return ctxt;
    }

    void call0(BaseDiagnosticContext initialContext) {
        final Driver.Builder builder = Driver.builder();
        builder.setInitialContext(initialContext);
        boolean nogc = gc.equals("none");
        int errors = initialContext.errors();
        if (errors == 0) {
            builder.setOutputDirectory(outputPath);
            HashSet<MavenCoordinate> addedCoordinates = new HashSet<>();
            // process the class paths
            try {
                resolveClassPath(builder::addBootClassPathItem, bootPaths, addedCoordinates);
                // add core things
                addCoreComponent(builder, addedCoordinates, "qbicc-runtime-unwind");
                addCoreComponent(builder, addedCoordinates, "qbicc-runtime-main");
                if (nogc) {
                    addCoreComponent(builder, addedCoordinates, "qbicc-runtime-gc-nogc");
                }
            } catch (IOException e) {
                // todo: close class path items?
                return;
            }
            // first, probe the target platform
            Platform target = platform;
            builder.setTargetPlatform(target);
            Optional<ObjectFileProvider> optionalProvider = ObjectFileProvider.findProvider(target.getObjectType(), Main.class.getClassLoader());
            if (optionalProvider.isEmpty()) {
                initialContext.error("No object file provider found for %s", target.getObjectType());
            } else {
                ObjectFileProvider objectFileProvider = optionalProvider.get();
                Iterator<CToolChain> toolChains = CToolChain.findAllCToolChains(target, t -> true, Main.class.getClassLoader()).iterator();
                if (! toolChains.hasNext()) {
                    initialContext.error("No working C compiler found");
                } else {
                    CToolChain toolChain = toolChains.next();
                    builder.setToolChain(toolChain);
                    // probe the basic system sizes
                    CProbe.Builder probeBuilder = CProbe.builder();
                    probeBuilder.include("<stdint.h>");
                    probeBuilder.include("<limits.h>");
                    // size and signedness of char
                    CProbe.Type char_t = CProbe.Type.builder().setName("char").build();
                    probeBuilder.probeType(char_t);
                    // int sizes
                    CProbe.Type int8_t = CProbe.Type.builder().setName("int8_t").build();
                    probeBuilder.probeType(int8_t);
                    CProbe.Type int16_t = CProbe.Type.builder().setName("int16_t").build();
                    probeBuilder.probeType(int16_t);
                    CProbe.Type int32_t = CProbe.Type.builder().setName("int32_t").build();
                    probeBuilder.probeType(int32_t);
                    CProbe.Type int64_t = CProbe.Type.builder().setName("int64_t").build();
                    probeBuilder.probeType(int64_t);
                    // float sizes
                    CProbe.Type float_t = CProbe.Type.builder().setName("float").build();
                    probeBuilder.probeType(float_t);
                    CProbe.Type double_t = CProbe.Type.builder().setName("double").build();
                    probeBuilder.probeType(double_t);
                    // bool
                    CProbe.Type _Bool = CProbe.Type.builder().setName("_Bool").build();
                    probeBuilder.probeType(_Bool);
                    // pointer
                    CProbe.Type void_p = CProbe.Type.builder().setName("void *").build();
                    probeBuilder.probeType(void_p);
                    // number of bits in char
                    probeBuilder.probeConstant("CHAR_BIT");
                    // max alignment
                    CProbe.Type max_align_t = CProbe.Type.builder().setName("max_align_t").build();
                    probeBuilder.probeType(max_align_t);
                    // execute
                    CProbe probe = probeBuilder.build();
                    try {
                        CProbe.Result probeResult = probe.run(toolChain, objectFileProvider, initialContext);
                        if (probeResult == null) {
                            initialContext.error("Type system probe compiler execution failed");
                        } else {
                            long charSize = probeResult.getTypeInfo(char_t).getSize();
                            if (charSize != 1) {
                                initialContext.error("Unexpected size of `char`: %d", Long.valueOf(charSize));
                            }
                            TypeSystem.Builder tsBuilder = TypeSystem.builder();
                            tsBuilder.setBoolSize((int) probeResult.getTypeInfo(_Bool).getSize());
                            tsBuilder.setBoolAlignment((int) probeResult.getTypeInfo(_Bool).getAlign());
                            tsBuilder.setByteBits(probeResult.getConstantInfo("CHAR_BIT").getValueAsInt());
                            tsBuilder.setInt8Size((int) probeResult.getTypeInfo(int8_t).getSize());
                            tsBuilder.setInt8Alignment((int) probeResult.getTypeInfo(int8_t).getAlign());
                            tsBuilder.setInt16Size((int) probeResult.getTypeInfo(int16_t).getSize());
                            tsBuilder.setInt16Alignment((int) probeResult.getTypeInfo(int16_t).getAlign());
                            tsBuilder.setInt32Size((int) probeResult.getTypeInfo(int32_t).getSize());
                            tsBuilder.setInt32Alignment((int) probeResult.getTypeInfo(int32_t).getAlign());
                            tsBuilder.setInt64Size((int) probeResult.getTypeInfo(int64_t).getSize());
                            tsBuilder.setInt64Alignment((int) probeResult.getTypeInfo(int64_t).getAlign());
                            tsBuilder.setFloat32Size((int) probeResult.getTypeInfo(float_t).getSize());
                            tsBuilder.setFloat32Alignment((int) probeResult.getTypeInfo(float_t).getAlign());
                            tsBuilder.setFloat64Size((int) probeResult.getTypeInfo(double_t).getSize());
                            tsBuilder.setFloat64Alignment((int) probeResult.getTypeInfo(double_t).getAlign());
                            tsBuilder.setPointerSize((int) probeResult.getTypeInfo(void_p).getSize());
                            tsBuilder.setPointerAlignment((int) probeResult.getTypeInfo(void_p).getAlign());
                            tsBuilder.setMaxAlignment((int) probeResult.getTypeInfo(max_align_t).getAlign());
                            // todo: function alignment probe
                            // for now, references == pointers
                            tsBuilder.setReferenceSize((int) probeResult.getTypeInfo(void_p).getSize());
                            tsBuilder.setReferenceAlignment((int) probeResult.getTypeInfo(void_p).getAlign());
                            CProbe.Type type_id_type = smallTypeIds ? int16_t : int32_t;
                            tsBuilder.setTypeIdSize((int) probeResult.getTypeInfo(type_id_type).getSize());
                            tsBuilder.setTypeIdAlignment((int) probeResult.getTypeInfo(type_id_type).getAlign());
                            tsBuilder.setEndianness(probeResult.getByteOrder());
                            if (nogc) {
                                new NoGcTypeSystemConfigurator().accept(tsBuilder);
                            }
                            builder.setTypeSystem(tsBuilder.build());
                            // add additional manual initializers by chaining `.andThen(...)`
                            builder.setVmFactory(cc -> VmImpl.create(cc,
                                new BasicInitializationManualInitializer(cc)
                            ));
                            builder.setObjectFileProvider(objectFileProvider);
                            ServiceLoader<DriverPlugin> loader = ServiceLoader.load(DriverPlugin.class);
                            Iterator<DriverPlugin> iterator = loader.iterator();
                            for (;;) try {
                                if (! iterator.hasNext()) {
                                    break;
                                }
                                DriverPlugin plugin = iterator.next();
                                plugin.accept(builder);
                            } catch (ServiceConfigurationError error) {
                                initialContext.error(error, "Failed to load plugin");
                            }
                            errors = initialContext.errors();
                            if (errors == 0) {
                                Iterator<LlvmToolChain> llvmTools = LlvmToolChain.findAllLlvmToolChains(target, t -> true, Main.class.getClassLoader()).iterator();
                                LlvmToolChain llvmToolChain = null;
                                while (llvmTools.hasNext()) {
                                    llvmToolChain = llvmTools.next();
                                    if (llvmToolChain.compareVersionTo("12") >= 0) {
                                        break;
                                    }
                                    llvmToolChain = null;
                                }
                                if (llvmToolChain == null) {
                                    initialContext.error("No working LLVM toolchain found");
                                    errors = initialContext.errors();
                                } else {
                                    builder.setLlvmToolChain(llvmToolChain);
                                }
                            }
                            if (errors == 0) {
                                assert mainClass != null; // else errors would be != 0
                                // keep it simple to start with
                                builder.setMainClass(mainClass.replace('.', '/'));

                                builder.addTypeBuilderFactory(ExternExportTypeBuilder::new);
                                builder.addTypeBuilderFactory(NativeTypeBuilder::new);
                                builder.addTypeBuilderFactory(ThreadLocalTypeBuilder::new);
                                builder.addTypeBuilderFactory(CoreAnnotationTypeBuilder::new);

                                builder.addNativeMethodConfiguratorFactory(NativeBindingMethodConfigurator::new);

                                builder.addResolverFactory(ConstTypeResolver::new);
                                builder.addResolverFactory(FunctionTypeResolver::new);
                                builder.addResolverFactory(PointerTypeResolver::new);
                                builder.addResolverFactory(InternalNativeTypeResolver::new);
                                builder.addResolverFactory(NativeTypeResolver::new);

                                builder.addTaskWrapperFactory(Phase.ADD, next -> (wrapper, ctxt) -> {
                                    Vm vm = ctxt.getVm();
                                    vm.doAttached(vm.newThread(Thread.currentThread().getName(), vm.getMainThreadGroup(), false, Thread.currentThread().getPriority()), () -> wrapper.accept(ctxt));
                                });
                                builder.addPreHook(Phase.ADD, LLVMIntrinsics::register);
                                builder.addPreHook(Phase.ADD, CoreIntrinsics::register);
                                builder.addPreHook(Phase.ADD, CoreClasses::get);
                                builder.addPreHook(Phase.ADD, ThrowExceptionHelper::get);
                                builder.addPreHook(Phase.ADD, new VMHelpersSetupHook());
                                builder.addPreHook(Phase.ADD, compilationContext -> {
                                    Vm vm = compilationContext.getVm();
                                    VmThread initThread = vm.newThread("initialization", vm.getMainThreadGroup(), false,  Thread.currentThread().getPriority());
                                    vm.doAttached(initThread, vm::initialize);
                                });
                                builder.addPreHook(Phase.ADD, new AddMainClassHook());
                                if (nogc) {
                                    builder.addPreHook(Phase.ADD, new NoGcSetupHook());
                                }
                                builder.addPreHook(Phase.ADD, ReachabilityInfo::forceCoreClassesReachable);
                                builder.addElementHandler(Phase.ADD, new ElementBodyCreator());
                                builder.addElementHandler(Phase.ADD, new ElementVisitorAdapter(new DotGenerator(Phase.ADD, graphGenConfig)));
                                builder.addElementHandler(Phase.ADD, new ElementInitializer());
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, IntrinsicBasicBlockBuilder::createForAddPhase);
                                if (nogc) {
                                    builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, NoGcMultiNewArrayBasicBlockBuilder::new);
                                }
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, ClassLoadingBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, NativeBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, MemberResolvingBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, StructMemberAccessBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, PointerBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, ClassInitializingBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, ConstantDefiningBasicBlockBuilder::createIfNeeded);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, ConstantBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, BasicInitializationBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, MethodCallFixupBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, DevirtualizingBasicBlockBuilder::new);
                                if (optMemoryTracking) {
                                    // TODO: breaks addr_of; should only be done in ANALYZE and then only if addr_of wasn't taken (alias)
                                    // builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, LocalMemoryTrackingBasicBlockBuilder::new);
                                }
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.CORRECT, RuntimeChecksBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.CORRECT, LocalThrowHandlingBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.CORRECT, SynchronizedMethodBasicBlockBuilder::createIfNeeded);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.OPTIMIZE, SimpleOptBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.INTEGRITY, ReachabilityBlockBuilder::new);
                                builder.addPostHook(Phase.ADD, ReachabilityInfo::reportStats);
                                builder.addPostHook(Phase.ADD, ReachabilityInfo::clear);

                                builder.addPreHook(Phase.ANALYZE, new VMHelpersSetupHook());
                                builder.addPreHook(Phase.ANALYZE, ReachabilityInfo::forceCoreClassesReachable);
                                builder.addElementHandler(Phase.ANALYZE, new ElementBodyCopier());
                                builder.addElementHandler(Phase.ANALYZE, new ElementVisitorAdapter(new DotGenerator(Phase.ANALYZE, graphGenConfig)));
                                if (optGotos) {
                                    builder.addCopyFactory(Phase.ANALYZE, GotoRemovingVisitor::new);
                                }
                                if (optPhis) {
                                    builder.addCopyFactory(Phase.ANALYZE, PhiOptimizerVisitor::new);
                                }
                                builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.TRANSFORM, IntrinsicBasicBlockBuilder::createForAnalyzePhase);
                                builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.TRANSFORM, InitializedStaticFieldBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.TRANSFORM, BasicInitializationBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.TRANSFORM, ThreadLocalBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.TRANSFORM, DevirtualizingBasicBlockBuilder::new);
                                if (optMemoryTracking) {
                                    builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.TRANSFORM, LocalMemoryTrackingBasicBlockBuilder::new);
                                }
                                builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.CORRECT, NumericalConversionBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.OPTIMIZE, SimpleOptBasicBlockBuilder::new);
                                if (optInlining) {
                                    builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.OPTIMIZE, InliningBasicBlockBuilder::new);
                                }
                                builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.INTEGRITY, ReachabilityBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.INTEGRITY, LocalVariableFindingBasicBlockBuilder::new);
                                builder.addPostHook(Phase.ANALYZE, ReachabilityInfo::reportStats);
                                // todo: restore when adapted for run time initializers
                                //builder.addPostHook(Phase.ANALYZE, new ClassInitializerRegister());
                                builder.addPostHook(Phase.ANALYZE, new DispatchTableBuilder());
                                builder.addPostHook(Phase.ANALYZE, new SupersDisplayBuilder());

                                builder.addPreHook(Phase.LOWER, Layout::unlock);
                                builder.addPreHook(Phase.LOWER, new ClassObjectSerializer());
                                builder.addElementHandler(Phase.LOWER, new FunctionLoweringElementHandler());
                                builder.addElementHandler(Phase.LOWER, new ElementVisitorAdapter(new DotGenerator(Phase.LOWER, graphGenConfig)));
                                if (optGotos) {
                                    builder.addCopyFactory(Phase.LOWER, GotoRemovingVisitor::new);
                                }
                                if (optPhis) {
                                    builder.addCopyFactory(Phase.LOWER, PhiOptimizerVisitor::new);
                                }
                                builder.addCopyFactory(Phase.LOWER, ObjectLiteralSerializingVisitor::new);

                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, ThrowLoweringBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, DevirtualizingBasicBlockBuilder::new);
                                if (nogc) {
                                    builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, NoGcBasicBlockBuilder::new);
                                }
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, IntrinsicBasicBlockBuilder::createForLowerPhase);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, InvocationLoweringBasicBlockBuilder::new);
                                // BooleanAccessBasicBlockBuilder must come before object and static field access lowering
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, BooleanAccessBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, LocalVariableLoweringBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, StaticFieldLoweringBasicBlockBuilder::new);
                                // InstanceOfCheckCastBB must come before ObjectAccessLoweringBuilder or typeIdOf won't be lowered correctly
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, InstanceOfCheckCastBasicBlockBuilder::new);
                                // todo: restore when adapted for run time initializers
                                //builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, LowerClassInitCheckBlockBuilder::new);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, ObjectAccessLoweringBuilder::new);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, ObjectMonitorBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, LLVMCompatibleBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.OPTIMIZE, SimpleOptBasicBlockBuilder::new);
                                if (optMemoryTracking) {
                                    builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, LocalMemoryTrackingBasicBlockBuilder::new);
                                }
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.INTEGRITY, LowerVerificationBasicBlockBuilder::new);
                                // MethodDataStringsSerializer should be the last BBB in the list
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, MethodDataStringsSerializer::new);

                                builder.addPreHook(Phase.GENERATE, new SupersDisplayEmitter());
                                builder.addPreHook(Phase.GENERATE, new DispatchTableEmitter());
                                builder.addPreHook(Phase.GENERATE, new LLVMGenerator(isPie ? 2 : 0, isPie ? 2 : 0));

                                builder.addPostHook(Phase.GENERATE, new DotGenerator(Phase.GENERATE, graphGenConfig));
                                builder.addPostHook(Phase.GENERATE, new LLVMCompileStage(isPie));
                                builder.addPostHook(Phase.GENERATE, new MethodDataEmitter());
                                builder.addPostHook(Phase.GENERATE, new LLVMDefaultModuleCompileStage(isPie));
                                builder.addPostHook(Phase.GENERATE, new LinkStage(isPie));

                                CompilationContext ctxt;
                                try (Driver driver = builder.build()) {
                                    ctxt = driver.getCompilationContext();
                                    MainMethod.get(ctxt).setMainClass(mainClass);
                                    driver.execute();
                                }
                            }
                        }
                    } catch (IOException e) {
                        initialContext.error(e, "Failed to probe system types from tool chain");
                    }
                }
            }
        }
        return;
    }

    private void addCoreComponent(final Driver.Builder builder, final HashSet<MavenCoordinate> addedCoordinates, final String artifact) throws IOException {
        resolveClassPath(builder::addBootClassPathItem, List.of(ClassPathEntry.of(MavenCoordinates.createCoordinate("org.qbicc", artifact, MainProperties.QBICC_VERSION, PackagingType.JAR, null))), addedCoordinates);
    }

    private void resolveClassPath(Consumer<ClassPathItem> classPathItemConsumer, final List<ClassPathEntry> bootPaths, Set<MavenCoordinate> addedCoordinates) throws IOException {
        for (ClassPathEntry bootPath : bootPaths) {
            if (bootPath instanceof ClassPathEntry.FilePath fp) {
                classPathItemConsumer.accept(new ClassPathItem(fp.getPath().toString(), List.of(ClassPathElement.forDirectory(fp.getPath())), List.of()));
            } else if (bootPath instanceof ClassPathEntry.MavenArtifact ma) {
                processCoordinate(classPathItemConsumer, addedCoordinates, ma.getArtifact());
            } else if (bootPath instanceof ClassPathEntry.ClassLibraries cl) {
                processCoordinate(classPathItemConsumer, addedCoordinates, MavenCoordinates.createCoordinate("org.qbicc.rt", "qbicc-rt", cl.getVersion(), PackagingType.POM, null));
            }
        }
    }

    private void processCoordinate(final Consumer<ClassPathItem> classPathItemConsumer, final Set<MavenCoordinate> addedCoordinates, final MavenCoordinate mavenCoordinate) throws IOException {
        try {
            // todo: work offline switch
            MavenResolvedArtifact[] artifacts = Maven.configureResolver().withMavenCentralRepo(true).addDependency(MavenDependencies.createDependency(mavenCoordinate, null, false)).resolve().withTransitivity().asResolvedArtifact();
            for (MavenResolvedArtifact artifact : artifacts) {
                // try to avoid duplication to a reasonable extent
                MavenCoordinate coordinate = artifact.getCoordinate();
                if (addedCoordinates.add(coordinate)) {
                    // try to get the source artifact for debug info
                    List<ClassPathElement> sourceList;
                    MavenCoordinate sourceCoordinate = MavenCoordinates.createCoordinate(coordinate.getGroupId(), coordinate.getArtifactId(), coordinate.getVersion(), PackagingType.JAVA_SOURCE, coordinate.getClassifier());
                    try {
                        MavenResolvedArtifact sourceArtifact = Maven.resolver().addDependency(MavenDependencies.createDependency(sourceCoordinate, ScopeType.COMPILE, false)).resolve().withoutTransitivity().asSingleResolvedArtifact();
                        sourceList = List.of(ClassPathElement.forJarFile(sourceArtifact.asFile()));
                    } catch (ResolutionException ignored) {
                        // no source item
                        sourceList = List.of();
                    }
                    File file = artifact.asFile();
                    // skip non-JAR things like POMs
                    if (file.getName().endsWith(".jar")) {
                        classPathItemConsumer.accept(new ClassPathItem(coordinate.toCanonicalForm(), List.of(ClassPathElement.forJarFile(file)), sourceList));
                    }
                }
            }
        } catch (ResolutionException e) {
            System.err.printf("Failed to resolve Maven artifact %s: ", mavenCoordinate.toCanonicalForm());
            e.printStackTrace(System.err);
            throw new IOException(e);
        }
    }

    public static void main(String[] args) {
        System.setProperty("java.util.logging.manager", LogManager.class.getName());
        CommandLineProcessor optionsProcessor = new CommandLineProcessor();
        CmdResult result = optionsProcessor.process(args);
        if (result != CmdResult.CMD_RESULT_OK) {
            return;
        }
        Builder mainBuilder = builder();
        mainBuilder.appendBootPaths(List.of(ClassPathEntry.ofClassLibraries(optionsProcessor.rtVersion)))
            .appendBootPaths(optionsProcessor.appendedBootPathEntries)
            .prependBootPaths(optionsProcessor.prependedBootPathEntries)
            .addAppPaths(optionsProcessor.appPathEntries)
            .setOutputPath(optionsProcessor.outputPath)
            .setMainClass(optionsProcessor.mainClass)
            .setDiagnosticsHandler(diagnostics -> {
                for (Diagnostic diagnostic : diagnostics) {
                    try {
                        diagnostic.appendTo(System.err);
                    } catch (IOException e) {
                        // just give up
                        break;
                    }
                }
            })
            .setGc(optionsProcessor.gc.toString())
            .setIsPie(optionsProcessor.isPie)
            .setOptMemoryTracking(optionsProcessor.optArgs.optMemoryTracking)
            .setOptInlining(optionsProcessor.optArgs.optInlining)
            .setOptGotos(optionsProcessor.optArgs.optGotos)
            .setOptPhis(optionsProcessor.optArgs.optPhis)
            .setSmallTypeIds(optionsProcessor.smallTypeIds)
            .setGraphGenConfig(optionsProcessor.graphGenConfig);
        Platform platform = optionsProcessor.platform;
        if (platform != null) {
            mainBuilder.setPlatform(platform);
        }

        Main main = mainBuilder.build();
        DiagnosticContext context = main.call();
        int errors = context.errors();
        int warnings = context.warnings();
        if (errors > 0) {
            if (warnings > 0) {
                System.err.printf("Compilation failed with %d error(s) and %d warning(s)%n", Integer.valueOf(errors), Integer.valueOf(warnings));
            } else {
                System.err.printf("Compilation failed with %d error(s)%n", Integer.valueOf(errors));
            }
        } else if (warnings > 0) {
            System.err.printf("Compilation completed with %d warning(s)%n", Integer.valueOf(warnings));
        }
        System.exit(errors == 0 ? 0 : 1);
    }

    private enum CmdResult {
        CMD_RESULT_HELP,
        CMD_RESULT_OK,
        CMD_RESULT_ERROR,
        ;
    }

    @CommandLine.Command(versionProvider = VersionProvider.class, mixinStandardHelpOptions = true)
    private static final class CommandLineProcessor {
        private enum GCType {
            NONE("none"),
            ;
            private final String gcType;

            GCType(String type) {
                this.gcType = type;
            }
            public String toString() {
                return gcType;
            }
        }

        @CommandLine.Option(names = "--boot-path-prepend-artifact", converter = ClassPathEntry.MavenArtifact.Converter.class)
        void prependBootPathArtifact(ClassPathEntry.MavenArtifact artifact) {
            prependedBootPathEntries.add(artifact);
        }
        @CommandLine.Option(names = "--boot-path-prepend-file", converter = ClassPathEntry.FilePath.Converter.class)
        void prependBootPathFile(ClassPathEntry.FilePath filePath) {
            prependedBootPathEntries.add(filePath);
        }
        private final List<ClassPathEntry> prependedBootPathEntries = new ArrayList<>();

        @CommandLine.Option(names = "--boot-path-append-artifact", converter = ClassPathEntry.MavenArtifact.Converter.class)
        void appendBootPathArtifact(ClassPathEntry.MavenArtifact artifact) {
            appendedBootPathEntries.add(artifact);
        }
        @CommandLine.Option(names = "--boot-path-append-file", converter = ClassPathEntry.FilePath.Converter.class)
        void appendBootPathFile(ClassPathEntry.FilePath filePath) {
            appendedBootPathEntries.add(filePath);
        }
        private final List<ClassPathEntry> appendedBootPathEntries = new ArrayList<>();

        @CommandLine.Option(names = "--rt-version")
        private String rtVersion = MainProperties.CLASSLIB_DEFAULT_VERSION;

        @CommandLine.Option(names = "--app-path-artifact", converter = ClassPathEntry.MavenArtifact.Converter.class)
        void addAppPathArtifact(ClassPathEntry.MavenArtifact artifact) {
            appPathEntries.add(artifact);
        }
        @CommandLine.Option(names = "--app-path-file", converter = ClassPathEntry.FilePath.Converter.class)
        void addAppPathFile(ClassPathEntry.FilePath filePath) {
            appPathEntries.add(filePath);
        }
        private List<ClassPathEntry> appPathEntries = new ArrayList<>();

        @CommandLine.Option(names = "--output-path", description = "Specify directory where the executable is placed")
        private Path outputPath;
        @CommandLine.Option(names = "--debug")
        private boolean debug;
        @CommandLine.Option(names = "--debug-vtables")
        private boolean debugVTables;
        @CommandLine.Option(names = "--dispatch-stats")
        private boolean dispatchStats;
        @CommandLine.Option(names = "--debug-reachability")
        private boolean debugReachability;
        @CommandLine.Option(names = "--debug-supers")
        private boolean debugSupers;
        @CommandLine.Option(names = "--debug-devirt")
        private boolean debugDevirt;
        @CommandLine.Option(names = "--debug-interpreter")
        private boolean debugInterpreter;
        @CommandLine.Option(names = "--gc", defaultValue = "none", description = "Type of GC to use. Valid values: ${COMPLETION-CANDIDATES}")
        private GCType gc;
        @CommandLine.Option(names = "--method-data-stats")
        private boolean methodDataStats;
        @CommandLine.Option(names = "--pie", negatable = true, defaultValue = "false", description = "[Disable|Enable] generation of position independent executable")
        private boolean isPie;
        @CommandLine.Option(names = "--platform", converter = PlatformConverter.class)
        private Platform platform;
        @CommandLine.Option(names = "--string-pool-stats")
        private boolean stringPoolStats;

        @CommandLine.Option(names = "--small-type-ids", negatable = true, defaultValue = "false", description = "Use narrow (16-bit) type ID values if true, wide (32-bit) type ID values if false")
        private boolean smallTypeIds;

        @CommandLine.Parameters(index="0", arity="1", description = "Application main class")
        private String mainClass;

        @CommandLine.ArgGroup(exclusive = false, multiplicity = "0..1", heading = "Options for controlling generation of graphs for methods%n")
        private GraphGenArgs graphGenArgs;

        @CommandLine.ArgGroup(exclusive = false, heading = "Options for controlling optimizations%n")
        private OptArgs optArgs = new OptArgs();

        private GraphGenConfig graphGenConfig = new GraphGenConfig();

        private static class GraphGenArgs {
            @CommandLine.Option(names = { "-g", "--gen-graph"}, required = true, description = "Enable generation of graphs")
            boolean genGraph;
            @CommandLine.ArgGroup(exclusive=false, multiplicity = "0..*")
            List<GraphGenMethodsPhases> methodsAndPhases;
        }

        private static class GraphGenMethodsPhases {
            @CommandLine.Option(names = { "-m", "--methods"}, required = false, split = ",", defaultValue = GraphGenConfig.ALL_METHODS,
                                description = "List of methods separated by comma. Default: ${DEFAULT-VALUE}")
            List<String> methods;
            @CommandLine.Option(names = { "-p", "--phases" }, required = false, split = ",", defaultValue = GraphGenConfig.ALL_PHASES,
                                description = "List of phases separated by comma. Default: ${DEFAULT-VALUE}")
            List<String> phases;
        }

        static class OptArgs {
            @CommandLine.Option(names = "--opt-memory-tracking", negatable = true, defaultValue = "false", description = "Enable/disable redundant store/load tracking and elimination")
            boolean optMemoryTracking;
            @CommandLine.Option(names = "--opt-inlining", negatable = true, defaultValue = "false", description = "Enable/disable inliner")
            boolean optInlining;
            @CommandLine.Option(names = "--opt-phis", negatable = true, defaultValue = "true", description = "Enable/disable `phi` elimination")
            boolean optPhis;
            @CommandLine.Option(names = "--opt-gotos", negatable = true, defaultValue = "true", description = "Enable/disable `goto` elimination")
            boolean optGotos;
        }

        public CmdResult process(String[] args) {
            try {
                CommandLine commandLine = new CommandLine(this);
                ParseResult parseResult = commandLine.parseArgs(args);
                if (CommandLine.printHelpIfRequested(parseResult)) {
                    return CmdResult.CMD_RESULT_HELP;
                }
            } catch (ParameterException ex) { // command line arguments could not be parsed
                System.err.println(ex.getMessage());
                ex.getCommandLine().usage(System.err);
                return CmdResult.CMD_RESULT_ERROR;
            }

            if (debug) {
                Logger.getLogger("").setLevel(Level.DEBUG);
            }
            if (debugVTables) {
                Logger.getLogger("org.qbicc.plugin.dispatch.tables").setLevel(Level.DEBUG);
            }
            if (dispatchStats) {
                Logger.getLogger("org.qbicc.plugin.dispatch.stats").setLevel(Level.DEBUG);
            }
            if (debugReachability) {
                Logger.getLogger("org.qbicc.plugin.reachability").setLevel(Level.DEBUG);
            }
            if (debugSupers) {
                Logger.getLogger("org.qbicc.plugin.instanceofcheckcast.supers").setLevel(Level.DEBUG);
            }
            if (debugDevirt) {
                Logger.getLogger("org.qbicc.plugin.dispatch.devirt").setLevel(Level.DEBUG);
            }
            if (debugInterpreter) {
                Logger.getLogger("org.qbicc.interpreter").setLevel(Level.DEBUG);
            }
            if (methodDataStats) {
                Logger.getLogger("org.qbicc.plugin.methodinfo.stats").setLevel(Level.DEBUG);
            }
            if (stringPoolStats) {
                Logger.getLogger("org.qbicc.plugin.stringpool.stats").setLevel(Level.DEBUG);
            }
            if (outputPath == null) {
                outputPath = Path.of(System.getProperty("java.io.tmpdir"), "qbicc-output-" + Integer.toHexString(ThreadLocalRandom.current().nextInt()));
            }

            if (graphGenArgs != null && graphGenArgs.genGraph) {
                if (graphGenArgs.methodsAndPhases == null) {
                    graphGenConfig.addMethodAndPhase(GraphGenConfig.ALL_METHODS, GraphGenConfig.ALL_PHASES);
                } else {
                    for (GraphGenMethodsPhases option : graphGenArgs.methodsAndPhases) {
                        for (String method : option.methods) {
                            for (String phase : option.phases) {
                                graphGenConfig.addMethodAndPhase(method, phase);
                            }
                        }
                    }
                }
            }
            return CmdResult.CMD_RESULT_OK;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<ClassPathEntry> bootPathsPrepend = new ArrayList<>();
        private final List<ClassPathEntry> bootPathsAppend = new ArrayList<>();
        private final List<ClassPathEntry> appPaths = new ArrayList<>();
        private String classLibVersion = MainProperties.CLASSLIB_DEFAULT_VERSION;
        private Path outputPath;
        private Consumer<Iterable<Diagnostic>> diagnosticsHandler = diagnostics -> {};
        private Platform platform = Platform.HOST_PLATFORM;
        private String mainClass;
        private String gc = "none";
        // TODO Detect whether the system uses PIEs by default and match that if possible
        private boolean isPie = false;
        private boolean optMemoryTracking = false;
        private boolean optInlining = false;
        private boolean optPhis = true;
        private boolean optGotos = true;
        private GraphGenConfig graphGenConfig;
        private boolean smallTypeIds = false;

        Builder() {}

        public Builder appendBootPath(ClassPathEntry entry) {
            Assert.checkNotNullParam("entry", entry);
            bootPathsAppend.add(entry);
            return this;
        }

        public Builder appendBootPaths(List<ClassPathEntry> entry) {
            Assert.checkNotNullParam("entry", entry);
            bootPathsAppend.addAll(entry);
            return this;
        }

        public Builder prependBootPath(ClassPathEntry entry) {
            Assert.checkNotNullParam("entry", entry);
            bootPathsPrepend.add(entry);
            return this;
        }

        public Builder prependBootPaths(List<ClassPathEntry> entry) {
            Assert.checkNotNullParam("entry", entry);
            bootPathsPrepend.addAll(entry);
            return this;
        }

        public Builder setClassLibVersion(String classLibVersion) {
            Assert.checkNotNullParam("classLibVersion", classLibVersion);
            this.classLibVersion = classLibVersion;
            return this;
        }

        public Builder addAppPath(ClassPathEntry entry) {
            Assert.checkNotNullParam("entry", entry);
            appPaths.add(entry);
            return this;
        }

        public Builder addAppPaths(List<ClassPathEntry> entry) {
            Assert.checkNotNullParam("entry", entry);
            appPaths.addAll(entry);
            return this;
        }

        public Builder setOutputPath(Path path) {
            Assert.checkNotNullParam("path", path);
            this.outputPath = path;
            return this;
        }

        public Builder setPlatform(Platform platform) {
            Assert.checkNotNullParam("platform", platform);
            this.platform = platform;
            return this;
        }

        public Builder setDiagnosticsHandler(Consumer<Iterable<Diagnostic>> handler) {
            Assert.checkNotNullParam("handler", handler);
            diagnosticsHandler = handler;
            return this;
        }

        public Builder setMainClass(String mainClass) {
            Assert.checkNotNullParam("mainClass", mainClass);
            this.mainClass = mainClass;
            return this;
        }

        public Builder setGc(String gc) {
            this.gc = Assert.checkNotNullParam("gc", gc);
            return this;
        }

        public Builder setIsPie(boolean isPie) {
            this.isPie = isPie;
            return this;
        }

        public Builder setGraphGenConfig(GraphGenConfig graphGenConfig) {
            Assert.checkNotNullParam("graphGenConfig", graphGenConfig);
            this.graphGenConfig = graphGenConfig;
            return this;
        }

        public Builder setOptMemoryTracking(boolean optMemoryTracking) {
            this.optMemoryTracking = optMemoryTracking;
            return this;
        }

        public Builder setOptInlining(boolean optInlining) {
            this.optInlining = optInlining;
            return this;
        }

        public Builder setOptPhis(boolean optPhis) {
            this.optPhis = optPhis;
            return this;
        }

        public Builder setOptGotos(boolean optGotos) {
            this.optGotos = optGotos;
            return this;
        }

        public Builder setSmallTypeIds(boolean smallTypeIds) {
            this.smallTypeIds = smallTypeIds;
            return this;
        }

        public Main build() {
            return new Main(this);
        }
    }

    static class MainProperties {
        static final String CLASSLIB_DEFAULT_VERSION;

        static final String QBICC_VERSION;

        static {
            Properties properties = new Properties();
            InputStream inputStream = MainProperties.class.getClassLoader().getResourceAsStream("main.properties");
            if (inputStream == null) {
                throw new Error("Missing main.properties");
            } else try (inputStream) {
                try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                    try (BufferedReader br = new BufferedReader(reader)) {
                        properties.load(br);
                    }
                }
            } catch (IOException e) {
                throw new IOError(e);
            }
            CLASSLIB_DEFAULT_VERSION = properties.getProperty("classlib.default-version");

            QBICC_VERSION = properties.getProperty("qbicc.version");
        }
    }

    static class VersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            return new String[] { "Qbicc version " + MainProperties.QBICC_VERSION };
        }
    }
}
