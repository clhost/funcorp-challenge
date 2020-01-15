package com.clhost.memes.app.data;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@Builder
@ToString
public class MemeBucket {
    private String source; // example: vk:group:germameme
    private String lang;
    private List<String> urls;

    public boolean isEmpty() {
        return source == null && lang == null && (urls == null || urls.isEmpty());
    }
}
