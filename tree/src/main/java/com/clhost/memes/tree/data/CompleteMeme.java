package com.clhost.memes.tree.data;

import lombok.*;

import java.sql.Timestamp;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CompleteMeme {
    private String url;
    private String lang;
    private String source;
    private String hash;
    private Timestamp date;
    private String bucketId;
}
