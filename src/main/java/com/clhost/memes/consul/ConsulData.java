package com.clhost.memes.consul;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsulData {
    private String source;
    private String lang;
    private String type;
    private String src;

    public String sourceDesc() {
        return src + ":" + type + ":" + source;
    }
}
