package com.clhost.memes.tree.vptree;

import com.github.kilianB.hash.Hash;

import java.util.List;

public interface MetricSpace {
    boolean isDuplicate(Hash node);
    void put(Hash node);
    void putAll(List<Hash> nodes);
    void load(List<String> nodes);
}
