package com.clhost.memes.tree.data;

import lombok.*;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Bucket {
    private String bucketId;
    private String text;
    private String lang;
    private String source;
    private Timestamp pubDate;
    private List<com.clhost.memes.tree.data.Data> images;
}
