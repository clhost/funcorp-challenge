package com.clhost.memes.app.sources;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SourceData {
    private String source;
    private String lang;
    private String type;
    private String subSource;

    public String sourceDesc() {
        return source + ":" + type + ":" + subSource;
    }
}
