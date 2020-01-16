package com.clhost.memes.app.controller;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PreviewItem {
    private String id;
    private String text;
    private List<String> urls;
}
