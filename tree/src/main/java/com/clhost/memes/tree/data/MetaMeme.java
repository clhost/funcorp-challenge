package com.clhost.memes.tree.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetaMeme {
    private String lang;
    private String source;
    private String bucketId;
    private List<String> urls;
}
