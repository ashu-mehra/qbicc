package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.ControlType;

public class RegionNode extends ControlNode<ControlType>  {

    public RegionNode(int maxLocals, int maxStack) {
        super(ControlType.INSTANCE, maxLocals, maxStack);
        new Exception("region creation: " + getId()).printStackTrace();
    }

    @Override
    public String label() {
        return "<region> " + getId();
    }
}

