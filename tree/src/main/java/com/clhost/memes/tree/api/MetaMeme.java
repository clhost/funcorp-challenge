package com.clhost.memes.tree.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MetaMeme {
    private String text;
    private String lang;
    private String source;
    private List<String> urls;
}
