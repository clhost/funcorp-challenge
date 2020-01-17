package com.clhost.memes.app.api.model;

import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
@Builder
public class PreviewItem {
    private String id;
    private String text;
    private List<String> urls;
    private Timestamp time;
}
