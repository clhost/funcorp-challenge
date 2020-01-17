package com.clhost.memes.tree.vptree;

import java.util.List;

public interface MetricSpace {
    boolean isDuplicate(VPTreeNode node);
    void put(VPTreeNode node);
    void putAll(List<VPTreeNode> nodes);
}
