package com.clhost.memes.tree.dao.data;

import lombok.AllArgsConstructor;
import lombok.ToString;

import java.sql.Timestamp;

@ToString
@lombok.Data
@AllArgsConstructor
public class Data {
    private String contentId;
    private String hash;
    private String url;
    private Timestamp pubDate;
}
