package com.clhost.memes.app.controller;

import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Builder
public class ContentData {
    private String contentId;
    private String url;
    private Timestamp contentPubDate;
}
