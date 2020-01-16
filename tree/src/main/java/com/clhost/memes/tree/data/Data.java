package com.clhost.memes.tree.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.sql.Timestamp;

@lombok.Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Data {
    private String contentId;
    private String url;
    private String hash;
    private Timestamp pubDate;
}
