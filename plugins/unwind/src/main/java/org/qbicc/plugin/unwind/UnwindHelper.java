package org.qbicc.plugin.unwind;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.context.ClassContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.MethodElement;

public class UnwindHelper {
    static final AttachmentKey<UnwindHelper> KEY = new AttachmentKey<>();
    private final MethodElement personalityMethod;

    private UnwindHelper(final CompilationContext ctxt) {
        String unwindClass = "org/qbicc/runtime/unwind/Unwind";
        ClassContext classContext = ctxt.getBootstrapClassContext();
        DefinedTypeDefinition unwindDefined = classContext.findDefinedType(unwindClass);
        if (unwindDefined != null) {
            LoadedTypeDefinition unwindValidated = unwindDefined.load();
            int index = unwindValidated.findMethodIndex(e -> e.getName().equals("personality"));
            personalityMethod = unwindValidated.getMethod(index);
        } else {
            personalityMethod = null;
            ctxt.error("Required class \"%s\" is not found on boot classpath", unwindClass);
        }
        DefinedTypeDefinition libUnwind = classContext.findDefinedType("org/qbicc/runtime/unwind/LibUnwind");
        if (libUnwind != null) {
            libUnwind.load();
        }
    }
    public static UnwindHelper get(CompilationContext ctxt) {
        UnwindHelper helper = ctxt.getAttachment(KEY);
        if (helper == null) {
            helper = new UnwindHelper(ctxt);
            UnwindHelper appearing = ctxt.putAttachmentIfAbsent(KEY, helper);
            if (appearing != null) {
                helper = appearing;
            }
        }
        return helper;
    }

    public MethodElement getPersonalityMethod() {
        return personalityMethod;
    }
}
