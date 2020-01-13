package com.clhost.memes.tree.vp;

import com.github.kilianB.hash.Hash;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VPTreeNode {
    private Hash hash;
    private Timestamp date;
}
