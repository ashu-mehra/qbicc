package org.qbicc.interpreter.impl;

/**
 *
 */
final class VmShortArrayImpl extends VmArrayImpl {

    VmShortArrayImpl(VmImpl vm, int size) {
        super(vm.shortArrayClass, size);
    }

    VmShortArrayImpl(final VmShortArrayImpl original) {
        super(original);
    }

    @Override
    public int getArrayElementOffset(int index) {
        return getVmClass().getVm().shortArrayContentOffset + (index << 1);
    }

    @Override
    protected VmShortArrayImpl clone() {
        return new VmShortArrayImpl(this);
    }
}
