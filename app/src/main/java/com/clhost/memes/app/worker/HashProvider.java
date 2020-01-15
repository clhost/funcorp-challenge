package com.clhost.memes.app.worker;

import org.apache.commons.codec.binary.Base64;
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
    private static final String ALGORITHM = "HmacSHA256";
    private static final String SECRET = "memes";

    public String hash(List<String> urls) {
        if (urls.size() == 1) return hash(urls.get(0));
        String joined = StringUtils.join(urls.stream().map(this::prepareUrl).sorted().collect(Collectors.toList()), null);
        return hash(joined);
    }

    private String hash(String url) {
        try {
            Mac mac = mac();
            SecretKeySpec key = secret();
            mac.init(key);
            return hash(mac, prepareUrl(url));
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
        return Base64.encodeBase64String(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }
}
