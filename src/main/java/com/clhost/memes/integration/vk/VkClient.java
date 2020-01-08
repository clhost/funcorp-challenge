package com.clhost.memes.integration.vk;

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


/**
 * Наверное должен отдать список урлов
 */
@Service
public class VkClient {

    @Value("${service.vk.app_id}")
    private Integer appId = 7271243;

    @Value("${service.vk.client_secret}")
    private String clientSecret = "4oK6WSiUUPpAmONoD2Ut";

    @Value("${service.vk.service_key}")
    private String serviceKey = "d0e8ee62d0e8ee62d0e8ee6293d0861d29dd0e8d0e8ee628efab6ede38fd80a46681715";

    private final VkApiClient vkApiClient;
    private final ServiceActor actor;

    public VkClient() {
        this.vkApiClient = new VkApiClient(HttpTransportClient.getInstance());
        this.actor = new ServiceActor(appId, clientSecret, serviceKey);
    }

    public List<VkBucket> memes(String source, int count) throws ClientException, ApiException {
        return memes(source, count, 0);
    }

    // Буду полагать что source это id группы
    public List<VkBucket> memes(String source, int count, int offset) throws ClientException, ApiException {
        GetResponse response = vkApiClient.wall().get(actor)
                .domain(source)
                .count(count)
                .offset(offset)
                .extended(false)
                .execute();

        List<WallpostFull> items = response.getItems();
        if (items == null || items.isEmpty()) return Collections.emptyList();

        List<Attachments> attachments = items.stream()
                .filter(item -> !item.isMarkedAsAds())
                .filter(item -> item.getAttachments() != null && !item.getAttachments().isEmpty())
                .map(item -> Attachments.builder().attachments(item.getAttachments()).id(String.valueOf(item.getId())).build())
                .collect(Collectors.toList());

        return attachments.stream()
                .map(this::map)
                .filter(data -> data.getId() != null) // грязный костыль, подумать как переделать
                .collect(Collectors.toList());
    }

    private VkBucket map(Attachments attachments) {
        List<Photo> photos = attachments.attachments.stream()
                .filter(attachment -> WallpostAttachmentType.PHOTO.equals(attachment.getType()))
                .map(WallpostAttachment::getPhoto)
                .collect(Collectors.toList());
        if (photos.isEmpty()) return VkBucket.builder().build();

        Photo photo = photos.get(0); // первая фотка (для синглов должно быть норм, а мультиаттачи потом запилю)
        VkData data = detectMaxSizePhotoAndGetVkData(photo);

        return VkBucket.builder()
                .id(attachments.id)
                .data(Collections.singletonList(data))
                .build();
    }

    private VkData detectMaxSizePhotoAndGetVkData(Photo photo) {
        List<PhotoSizes> sizes = photo.getSizes();
        PhotoSizes size = sizes.stream()
                .max(this::compareSizes)
                .get(); // of course, present
        return VkData.builder()
                .id(String.valueOf(photo.getId()))
                .date(photo.getDate())
                .url(size.getUrl().toString())
                .build();
    }

    private int compareSizes(PhotoSizes size1, PhotoSizes size2) {
        return size1.getHeight() * size1.getWidth() - size2.getHeight() * size2.getWidth();
    }

    @Builder
    private static class Attachments {
        private String id;
        private List<WallpostAttachment> attachments;
    }
}
