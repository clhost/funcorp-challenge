package com.clhost.memes.app.data;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString
public class MemeData {
    private long date; // date in epoch millis
    private String id; // id of an image
    private String url; // image url
}
