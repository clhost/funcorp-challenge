package com.clhost.memes.integration.vk;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString
public class VkData {
    private long date; // date in epoch millis
    private String id; // id of image (specific for vk)
    private String url; // image url
}
