package com.clhost.memes.tree.service;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Building id for bucket based on it's urls
 */
@Service
public class HashProvider {
    private static final Logger LOGGER = LogManager.getLogger(HashProvider.class);

    private static final String ALGORITHM = "HmacSHA1";
    private static final String SECRET = "memes";

    public String bucketId(List<String> urls) {
        if (urls.size() == 1) return hash(StringUtils.reverse(prepareUrl(urls.get(0))));
        String joined = StringUtils.join(urls.stream()
                .map(s -> StringUtils.reverse(prepareUrl(s)))
                .sorted()
                .collect(Collectors.toList()), null);
        return hash(joined);
    }

    public String contentId(String url) {
        return hash(prepareUrl(url));
    }

    private String hash(String url) {
        try {
            Mac mac = mac();
            SecretKeySpec key = secret();
            mac.init(key);
            return hash(mac, url);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            LOGGER.error(e.getMessage());
            throw new IllegalStateException();
        }
    }

    private String prepareUrl(String url) {
        return url.replaceFirst("http://", "").replaceFirst("https://", "");
    }

    private Mac mac() throws NoSuchAlgorithmException {
        return Mac.getInstance(ALGORITHM);
    }

    private SecretKeySpec secret() {
        return new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    private String hash(Mac mac, String body) {
        byte[] bytes = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte element : bytes) {
            result.append(Integer.toString((element & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }
}
