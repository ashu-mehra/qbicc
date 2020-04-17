package cc.quarkus.qcc.machine.llvm.op;

/**
 *
 */
public enum OrderingConstraint {
    unordered,
    monotonic,
    acquire,
    release,
    acq_rel,
    seq_cst,
    ;
}
