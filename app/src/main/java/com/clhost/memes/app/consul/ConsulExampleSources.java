package com.clhost.memes.app.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetBinaryValue;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Component
public class ConsulExampleSources {
    private static final Logger LOGGER = LogManager.getLogger(ConsulExampleSources.class);

    private final ConsulClient consulClient;

    @Value("${service.consul.kv_src}")
    private String kvSrc;

    @Autowired
    public ConsulExampleSources(ConsulClient consulClient) {
        this.consulClient = consulClient;
    }

    @PostConstruct
    public void configureExampleSources() {
        try {
            Response<GetBinaryValue> kvBinaryValue = consulClient.getKVBinaryValue(kvSrc);
            if (kvBinaryValue.getValue() == null) pushExampleSources();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void pushExampleSources() throws IOException {
        byte[] exampleSources = readExampleSources();
        consulClient.setKVBinaryValue(kvSrc, exampleSources);
        LOGGER.info("Consul: push example-sources was complete");
    }

    private byte[] readExampleSources() throws IOException {
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("example-sources.json");
        if (stream == null) throw new IllegalStateException("Can't find example-sources-json");
        return IOUtils.toByteArray(stream);
    }
}
