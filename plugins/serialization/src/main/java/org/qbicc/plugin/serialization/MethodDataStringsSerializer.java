package org.qbicc.plugin.serialization;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.Call;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmString;
import org.qbicc.object.ProgramModule;
import org.qbicc.object.Section;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FunctionElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.MethodElement;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

public class MethodDataStringsSerializer extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;
    //private final NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> delegate;

    public MethodDataStringsSerializer(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
        //this.delegate = delegate;
    }

    /*public NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> getDelegateNodeVisitor() {
        return delegate;
    }*/

/*
    public Value visit(final Node.Copier param, final Call node) {
        Vm vm = ctxt.getVm();
        BuildtimeHeap heap = BuildtimeHeap.get(ctxt);
        ExecutableElement element = node.getElement();

        String methodName = "";
        if (element instanceof ConstructorElement) {
            methodName = "<init>";
        } else if (element instanceof InitializerElement) {
            methodName = "<clinit>";
        } else if (element instanceof MethodElement) {
            methodName = ((MethodElement)element).getName();
        } else if (element instanceof FunctionElement) {
            methodName = ((FunctionElement)element).getName();
        }

        String fileName = element.getSourceFileName();
        String className = element.getEnclosingType().getInternalName();
        String methodDesc = element.getDescriptor().toString();

        heap.serializeVmObject(vm.intern(fileName));
        heap.serializeVmObject(vm.intern(className));
        heap.serializeVmObject(vm.intern(methodName));
        heap.serializeVmObject(vm.intern(methodDesc));

        return getDelegateValueVisitor().visit(param, node);
    }
*/
    public Value call(ValueHandle target, List<Value> arguments) {
        Call node = (Call)super.call(target, arguments);
        ExecutableElement element = node.getElement();
        Vm vm = ctxt.getVm();
        BuildtimeHeap heap = BuildtimeHeap.get(ctxt);

        String methodName = "";
        if (element instanceof ConstructorElement) {
            methodName = "<init>";
        } else if (element instanceof InitializerElement) {
            methodName = "<clinit>";
        } else if (element instanceof MethodElement) {
            methodName = ((MethodElement)element).getName();
        } else if (element instanceof FunctionElement) {
            methodName = ((FunctionElement)element).getName();
        }

        String fileName = element.getSourceFileName();
        String className = element.getEnclosingType().getInternalName();
        String methodDesc = element.getDescriptor().toString();

        if (fileName != null) {
            heap.serializeVmObject(vm.intern(fileName));
        }
        heap.serializeVmObject(vm.intern(className));
        heap.serializeVmObject(vm.intern(methodName));
        heap.serializeVmObject(vm.intern(methodDesc));
        return node;
    }
}
