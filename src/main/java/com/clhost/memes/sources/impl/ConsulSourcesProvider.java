package com.clhost.memes.sources.impl;

import com.clhost.memes.consul.InstanceIdBeanPostProcessor;
import com.clhost.memes.sources.SourceData;
import com.clhost.memes.sources.SourcesProvider;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Этот компонент берет конкретное число источников
 * Сортировка: по createIndex'у в Consul'е
 */
@Service
public class ConsulSourcesProvider implements SourcesProvider {
    //private static final Logger LOGGER = LoggerFactory.getLogger(ConsulInteract.class);

    private final ConsulClient consulClient;
    private final ObjectMapper mapper;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${service.consul.kv_src}")
    private String kvSrc;

    private String instanceId = InstanceIdBeanPostProcessor.instanceId;

    @Autowired
    public ConsulSourcesProvider(ConsulClient consulClient, ObjectMapper mapper) {
        this.consulClient = consulClient;
        this.mapper = mapper;
    }

    @Override
    public List<SourceData> sources() {
        List<CatalogService> services = consulClient.getCatalogService(appName, QueryParams.DEFAULT).getValue();
        List<SourceData> sources = sourcesList();

        if (sources.isEmpty()) return sources;

        Integer position = position(services);
        Integer servicesCount = services.size();

        return selectSources(position, servicesCount, sources);
    }

    private List<SourceData> selectSources(Integer position, Integer servicesCount, List<SourceData> sources) {
        Integer sourcesCount = sources.size();
        List<SourceData> result = new ArrayList<>();

        if (position > servicesCount) throw new IllegalStateException("Consul: position > servicesCount");
        if (sourcesCount == 0 || position > sourcesCount) return Collections.emptyList();

        Integer counter = position;
        while (counter <= sourcesCount) {
            result.add(sources.get(counter - 1));
            counter += servicesCount;
        }
        return result;
    }

    private int position(List<CatalogService> services) {
        List<CatalogService> sortedServices = services.stream()
                .sorted(Comparator.comparing(CatalogService::getCreateIndex))
                .collect(Collectors.toList());

        Optional<CatalogService> first = sortedServices.stream()
                .filter(s -> s.getServiceId().contains(instanceId))
                .findFirst();

        CatalogService service = first.get();
        for (int i = 0; i < sortedServices.size(); i++) {
            if (service.equals(sortedServices.get(i))) return i + 1;
        }
        throw new IllegalStateException("Consul: unexpected end of search position");
    }

    private List<SourceData> sourcesList() {
        try {
            String value = consulClient.getKVValue(kvSrc).getValue().getValue();
            byte[] bytes = Base64.getDecoder().decode(value);
            return mapper.readValue(bytes, new TypeReference<List<SourceData>>() {});
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }
}
