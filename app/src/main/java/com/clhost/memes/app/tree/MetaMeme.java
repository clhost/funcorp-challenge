package com.clhost.memes.app.tree;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MetaMeme {
    private String lang;
    private String source;
    private String bucketId;
    private List<String> urls;
}
