package com.clhost.memes.app.integration;

import com.clhost.memes.app.worker.MemeBucket;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.ServiceActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.photos.Photo;
import com.vk.api.sdk.objects.photos.PhotoSizes;
import com.vk.api.sdk.objects.wall.WallpostAttachment;
import com.vk.api.sdk.objects.wall.WallpostAttachmentType;
import com.vk.api.sdk.objects.wall.WallpostFull;
import com.vk.api.sdk.objects.wall.responses.GetResponse;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VkClient {

    private final VkApiClient vkApiClient;
    private final ServiceActor actor;

    public VkClient(@Value("${service.vk.app_id}") int appId,
                    @Value("${service.vk.client_secret}") String clientSecret,
                    @Value("${service.vk.service_key}") String serviceKey) {
        this.vkApiClient = new VkApiClient(HttpTransportClient.getInstance());
        this.actor = new ServiceActor(appId, clientSecret, serviceKey);
    }

    public List<MemeBucket> memes(String source, int count) throws ClientException, ApiException {
        return memes(source, count, 0);
    }

    public List<MemeBucket> memes(String source, int count, int offset) throws ClientException, ApiException {
        GetResponse response = vkApiClient.wall().get(actor)
                .domain(source)
                .count(count)
                .offset(offset)
                .extended(false)
                .execute();

        List<WallpostFull> items = response.getItems();
        if (items == null || items.isEmpty()) return Collections.emptyList();

        List<Attachments> attachments = items.stream()
                .filter(item -> !item.isMarkedAsAds() && !isPinned(item))
                .filter(item -> item.getAttachments() != null && !item.getAttachments().isEmpty())
                .map(item -> Attachments.builder().attachments(item.getAttachments()).text(item.getText()).build())
                .collect(Collectors.toList());

        return attachments.stream()
                .map(this::map)
                .filter(data -> !data.isEmpty())
                .collect(Collectors.toList());
    }

    private boolean isPinned(WallpostFull item) {
        return item.getIsPinned() != null && item.getIsPinned() == 1;
    }

    private MemeBucket map(Attachments attachments) {
        List<Photo> photos = attachments.attachments.stream()
                .filter(attachment -> WallpostAttachmentType.PHOTO.equals(attachment.getType()))
                .map(WallpostAttachment::getPhoto)
                .collect(Collectors.toList());

        if (photos.isEmpty()) return MemeBucket.builder().build();
        List<String> urls = photos.stream()
                .map(this::detectMaxSizePhotoAndGetUrlOfMeme)
                .collect(Collectors.toList());

        return MemeBucket.builder()
                .urls(urls)
                .text(attachments.text.replaceAll("\n", " "))
                .build();
    }

    private String detectMaxSizePhotoAndGetUrlOfMeme(Photo photo) {
        List<PhotoSizes> sizes = photo.getSizes();
        PhotoSizes size = sizes.stream()
                .max(this::compareSizes)
                .get(); // of course, present
        return size.getUrl().toString();
    }

    private int compareSizes(PhotoSizes size1, PhotoSizes size2) {
        return size1.getHeight() * size1.getWidth() - size2.getHeight() * size2.getWidth();
    }

    @Builder
    private static class Attachments {
        private String text;
        private List<WallpostAttachment> attachments;
    }
}
