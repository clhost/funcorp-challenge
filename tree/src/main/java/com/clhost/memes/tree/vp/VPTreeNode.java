package com.clhost.memes.tree.vp;

import com.github.kilianB.hash.Hash;
import lombok.*;

import java.sql.Timestamp;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class VPTreeNode {
    private Hash hash;
    private Timestamp date;
}
