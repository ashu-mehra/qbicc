package org.qbicc.graph.schedule;

import java.util.BitSet;
import java.util.Map;

import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.Terminator;
import io.smallrye.common.constraint.Assert;

final class BlockInfo {
    final BasicBlock block;
    final int index;
    int dominator;
    int domDepth = -1;

    // dominator finder fields
    final BitSet pred = new BitSet();
    final BitSet succ = new BitSet();
    final BitSet bucket = new BitSet();
    int parent;
    int ancestor;
    int child;
    int vertex;
    int label;
    int semi;
    int size;

    BlockInfo(final BasicBlock block, final int index) {
        this.block = Assert.checkNotNullParam("block", block);
        this.index = index;
    }

    void computeIndices(final Map<BasicBlock, BlockInfo> blockInfos, int[] holder) {
        blockInfos.put(block, this);
        Terminator terminator = block.getTerminator();
        int cnt = terminator.getSuccessorCount();
        for (int i = 0; i < cnt; i ++) {
            BasicBlock block = terminator.getSuccessor(i);
            processBlock(blockInfos, holder, block);
            succ.set(blockInfos.get(block).index - 1);
        }
    }

    private void processBlock(final Map<BasicBlock, BlockInfo> blockInfos, int[] holder, BasicBlock block) {
        if (! blockInfos.containsKey(block)) {
            new BlockInfo(block, holder[0]++).computeIndices(blockInfos, holder);
        }
    }

    int findDomDepths(final BlockInfo[] infos) {
        int domDepth = this.domDepth;
        if (domDepth == -1) {
            domDepth = this.domDepth = dominator == 0 ? 0 : infos[dominator - 1].findDomDepths(infos) + 1;
        }
        return domDepth;
    }
}
