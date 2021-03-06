package com.clhost.memes.app.worker;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@Builder
@ToString
public class MemeBucket {
    private String text;
    private String lang;
    private String source;
    private List<String> urls;

    public boolean isEmpty() {
        return source == null && lang == null && (urls == null || urls.isEmpty());
    }
}
