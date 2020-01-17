package com.clhost.memes.tree.vptree;

import com.github.kilianB.hash.Hash;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.sql.Timestamp;

@Data
@ToString
@AllArgsConstructor
public class VPTreeNode {
    private Hash hash;
    private Timestamp date;
}
