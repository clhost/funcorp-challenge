package com.clhost.memes.app.sources;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SourceData {
    private String source;
    private String lang;
    private String type;
    private String subSource;
    private boolean skipText;

    public String sourceDesc() {
        return source + ":" + type + ":" + subSource;
    }
}
