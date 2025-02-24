package org.qbicc.plugin.main_method;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.driver.Phase;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.StaticMethodElementHandle;
import org.qbicc.graph.Value;
import org.qbicc.plugin.intrinsics.Intrinsics;
import org.qbicc.context.ClassContext;
import org.qbicc.plugin.intrinsics.StaticIntrinsic;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 *
 */
public class UserMainIntrinsic implements StaticIntrinsic {
    private final MethodElement realMain;

    public UserMainIntrinsic(final MethodElement realMain) {
        this.realMain = realMain;
    }

    @Override
    public Value emitIntrinsic(BasicBlockBuilder builder, StaticMethodElementHandle target, List<Value> arguments) {
        return builder.call(builder.staticMethod(realMain, realMain.getDescriptor(), realMain.getType()), arguments);
    }

    public static void register(CompilationContext ctxt, MethodElement mainMethod) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        TypeDescriptor runtimeMainDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/main/Main");
        MethodDescriptor runtimeMainMethodDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V,
            List.of(ArrayTypeDescriptor.of(classContext, ClassTypeDescriptor.synthesize(classContext, "java/lang/String"))));
        intrinsics.registerIntrinsic(Phase.ADD, runtimeMainDesc, "userMain", runtimeMainMethodDesc, new UserMainIntrinsic(mainMethod));
    }
}
