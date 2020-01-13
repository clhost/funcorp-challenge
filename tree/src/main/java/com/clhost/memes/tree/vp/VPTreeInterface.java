package com.clhost.memes.tree.vp;

import java.util.List;

public interface VPTreeInterface {
    boolean isDuplicate(VPTreeNode node);
    void put(VPTreeNode node);
    void putAll(List<VPTreeNode> nodes);
}
