package com.clhost.memes.integration.vk;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@Builder
/**
 * Бакет из фотографий, относящихся одному посту (мем из одной картинки или целый комикс)
 * Бакет == вк айтем
 */
@ToString
public class VkBucket {
    private String id;
    private String source; // view: vk:group:germameme
    private String lang; // meme lang
    private List<VkData> data;
}
