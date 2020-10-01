package cc.quarkus.qcc.interpreter;

import cc.quarkus.qcc.type.definition.FieldContainer;

public interface Prototype {
    byte[] getBytecode();
    String getClassName();
    Class<? extends FieldContainer> getPrototypeClass();
    FieldContainer construct();
}
