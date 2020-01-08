package com.clhost.memes.consul;

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
public class ConsulInteract {
    //private static final Logger LOGGER = LoggerFactory.getLogger(ConsulInteract.class);

    private final ConsulClient consulClient;
    private final ObjectMapper mapper;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${service.consul.kv_src}")
    private String kvSrc;

    private String instanceId = InstanceIdBeanPostProcessor.getInstanceId();

    @Autowired
    public ConsulInteract(ConsulClient consulClient, ObjectMapper mapper) {
        this.consulClient = consulClient;
        this.mapper = mapper;
    }

    public List<ConsulData> sources() {
        List<CatalogService> services = consulClient.getCatalogService(appName, QueryParams.DEFAULT).getValue();
        List<ConsulData> sources = sourcesList();

        if (sources.isEmpty()) return sources;

        Integer position = position(services);
        Integer servicesCount = services.size();

        return selectSources(position, servicesCount, sources);
    }

    private static List<ConsulData> selectSources(Integer position, Integer servicesCount, List<ConsulData> sources) {
        Integer sourcesCount = sources.size();
        List<ConsulData> result = new ArrayList<>();

        if (position > servicesCount) throw new IllegalStateException("position > servicesCount");
        if (sourcesCount == 0) return Collections.emptyList();
        if (position > sourcesCount) return Collections.emptyList();

        Integer counter = position;
        while (counter <= sourcesCount) {
            result.add(sources.get(counter - 1));
            counter += servicesCount;
        }
        return result;
    }

    // должен отдавать позицию, начиная с 1
    private int position(List<CatalogService> services) {
        List<CatalogService> sortedServices = services.stream()
                .sorted(Comparator.comparing(CatalogService::getCreateIndex))
                .collect(Collectors.toList());

        Optional<CatalogService> first = sortedServices.stream()
                .filter(s -> s.getServiceId().contains(instanceId))
                .findFirst();

        if (!first.isPresent()) throw new IllegalStateException("!first.isPresent()"); // подумать чо тут делать
        CatalogService service = first.get();

        for (int i = 0; i < sortedServices.size(); i++) {
            if (service.equals(sortedServices.get(i))) return i + 1;
        }

        throw new IllegalStateException("Default end"); // подумать чо тут делать
    }

    private List<ConsulData> sourcesList() {
        try {
            String value = consulClient.getKVValue(kvSrc).getValue().getValue();
            byte[] bytes = Base64.getDecoder().decode(value);
            return mapper.readValue(bytes, new TypeReference<List<ConsulData>>() {});
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }
}
