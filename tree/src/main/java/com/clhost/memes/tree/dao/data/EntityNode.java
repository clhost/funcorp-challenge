package com.clhost.memes.tree.dao.data;

import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Builder
public class EntityNode {
    private String hash;
    private Timestamp date;
}
