package org.qbicc.context;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.InterfaceObjectType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.annotation.type.TypeAnnotationList;
import org.qbicc.type.definition.DefineFailedException;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.DescriptorTypeResolver;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.MethodSignature;
import org.qbicc.type.generic.TypeParameterContext;
import org.qbicc.type.generic.TypeSignature;
import io.smallrye.common.constraint.Assert;

/**
 * A class and interface context, which can either be standalone (static) or can be integrated with an interpreter.  An
 * interpreter should have one instance per class loader.
 */
public interface ClassContext extends DescriptorTypeResolver {
    CompilationContext getCompilationContext();

    /**
     * Get the class loader object for this context.  The bootstrap class loader is {@code null}.
     *
     * @return the class loader object for this context
     */
    VmClassLoader getClassLoader();

    DefinedTypeDefinition findDefinedType(String typeName);

    DefinedTypeDefinition.Builder newTypeBuilder();

    String deduplicate(ByteBuffer buffer, int offset, int length);

    String deduplicate(String original);

    TypeSystem getTypeSystem();

    LiteralFactory getLiteralFactory();

    BasicBlockBuilder newBasicBlockBuilder(ExecutableElement element);

    void defineClass(String name, DefinedTypeDefinition definition);

    ValueType resolveTypeFromDescriptor(TypeDescriptor descriptor, TypeParameterContext paramCtxt, TypeSignature signature, TypeAnnotationList visibleAnnotations, final TypeAnnotationList invisibleAnnotations);

    FunctionType resolveMethodFunctionType(MethodDescriptor descriptor, TypeParameterContext paramCtxt, MethodSignature signature, final TypeAnnotationList returnTypeVisible, List<TypeAnnotationList> visibleAnnotations, final TypeAnnotationList returnTypeInvisible, final List<TypeAnnotationList> invisibleAnnotations);

    /**
     * Create a basic class context which can be used to produce type definitions.
     *
     * @param jarPaths the JAR paths to search
     * @return the class context
     * @throws IOException if one of the JAR paths could not be opened
     */
    @Deprecated
    static Basic createBasic(List<Path> jarPaths) throws IOException {
        List<JarFile> jarFiles = new ArrayList<>(jarPaths.size());
        int i = 0;
        try {
            while (i < jarPaths.size()) {
                Path path = jarPaths.get(i);
                JarFile jarFile = new JarFile(path.toFile(), false, ZipFile.OPEN_READ, JarFile.runtimeVersion());
                jarFiles.add(i++, jarFile);
            }
        } catch (Throwable t) {
            while (i > 0) {
                try {
                    jarFiles.get(--i).close();
                } catch (IOException e) {
                    t.addSuppressed(e);
                }
            }
            throw t;
        }

        return new Basic() {
            final ConcurrentHashMap<String, DefinedTypeDefinition> loaded = new ConcurrentHashMap<>();
            final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
            final TypeSystem ts = TypeSystem.builder().build();
            final LiteralFactory lf = LiteralFactory.create(ts);

            public void close() {
                for (JarFile jarFile : jarFiles) {
                    try {
                        jarFile.close();
                    } catch (Throwable ignored) {
                    }
                }
            }

            public CompilationContext getCompilationContext() {
                return null;
            }

            public VmClassLoader getClassLoader() {
                return null;
            }

            public DefinedTypeDefinition findDefinedType(final String typeName) {
                return loaded.computeIfAbsent(typeName, name -> {
                    String nameStr = name + ".class";
                    for (JarFile jarFile : jarFiles) {
                        JarEntry entry = jarFile.getJarEntry(nameStr);
                        if (entry != null) try {
                            byte[] content = jarFile.getInputStream(entry).readAllBytes();
                            DefinedTypeDefinition.Builder builder = DefinedTypeDefinition.Builder.basic();
                            ClassFile.of(this, ByteBuffer.wrap(content)).accept(builder);
                            return builder.build();
                        } catch (IOException e) {
                            throw new DefineFailedException(e);
                        }
                    }
                    return null;
                });
            }

            public DefinedTypeDefinition resolveDefinedTypeLiteral(final ObjectType typeId) {
                if (typeId instanceof ClassObjectType) {
                    return ((ClassObjectType) typeId).getDefinition();
                } else if (typeId instanceof InterfaceObjectType) {
                    return ((InterfaceObjectType) typeId).getDefinition();
                } else {
                    throw new IllegalArgumentException("Invalid defined type");
                }
            }

            public String deduplicate(final ByteBuffer buffer, final int offset, final int length) {
                byte[] b = new byte[length];
                int p = buffer.position();
                try {
                    buffer.position(offset);
                    buffer.get(b);
                } finally {
                    buffer.position(p);
                }
                return deduplicate(new String(b, StandardCharsets.UTF_8));
            }

            public String deduplicate(final String original) {
                return cache.computeIfAbsent(original, Function.identity());
            }

            public TypeSystem getTypeSystem() {
                return ts;
            }

            public LiteralFactory getLiteralFactory() {
                return lf;
            }

            public BasicBlockBuilder newBasicBlockBuilder(final ExecutableElement element) {
                return BasicBlockBuilder.simpleBuilder(ts, element);
            }

            public void defineClass(final String name, final DefinedTypeDefinition definition) {
                throw Assert.unsupported();
            }

            public ValueType resolveTypeFromClassName(final String packageName, final String internalName) {
                throw Assert.unsupported();
            }

            public ValueType resolveTypeFromDescriptor(final TypeDescriptor descriptor, TypeParameterContext paramCtxt, final TypeSignature signature, final TypeAnnotationList visibleAnnotations, final TypeAnnotationList invisibleAnnotations) {
                throw Assert.unsupported();
            }

            public ArrayObjectType resolveArrayObjectTypeFromDescriptor(final TypeDescriptor descriptor, TypeParameterContext paramCtxt, final TypeSignature signature, final TypeAnnotationList visible, final TypeAnnotationList invisible) {
                throw Assert.unsupported();
            }

            public FunctionType resolveMethodFunctionType(final MethodDescriptor descriptor, TypeParameterContext paramCtxt, final MethodSignature signature, final TypeAnnotationList returnTypeVisible, final List<TypeAnnotationList> visibleAnnotations, final TypeAnnotationList returnTypeInvisible, final List<TypeAnnotationList> invisibleAnnotations) {
                throw Assert.unsupported();
            }

            @Override
            public DefinedTypeDefinition.Builder newTypeBuilder() {
                return null;
            }

            public ValueType resolveTypeFromMethodDescriptor(final TypeDescriptor descriptor, TypeParameterContext paramCtxt, final TypeSignature signature, final TypeAnnotationList visibleAnnotations, final TypeAnnotationList invisibleAnnotations) {
                throw Assert.unsupported();
            }
        };
    }

    interface Basic extends ClassContext, AutoCloseable {
        void close();
    }
}
