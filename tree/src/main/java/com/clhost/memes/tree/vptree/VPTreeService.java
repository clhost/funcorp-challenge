package com.clhost.memes.tree.vptree;

import com.eatthepath.jvptree.DistanceFunction;
import com.eatthepath.jvptree.ThresholdSelectionStrategy;
import com.eatthepath.jvptree.VPTree;
import com.eatthepath.jvptree.util.SamplingMedianDistanceThresholdSelectionStrategy;
import com.github.kilianB.hash.Hash;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VPTreeService implements MetricSpace {
    private static final Logger LOGGER = LogManager.getLogger(VPTreeService.class);

    @Value("${service.tree.duplicate_threshold}")
    private double duplicateThreshold;

    private int bitResolution;
    private VPTree<Hash, Hash> tree;

    private final int algorithmId;
    private final Counter sizeCounter;
    private final Counter duplicatesCounter;

    @Autowired
    public VPTreeService(@Value("${service.tree.bit_resolution}") int bitResolution,
                         @Value("${service.tree.is_normalized_distance}") boolean isNormalized, MeterRegistry registry) {
        this.bitResolution = bitResolution;
        this.algorithmId = new PerceptiveHash(bitResolution).algorithmId();
        this.sizeCounter = sizeCounter(registry);
        this.duplicatesCounter = duplicatesCounter(registry);
        this.tree = new VPTree<>(distanceFunction(isNormalized), strategy(bitResolution));
        LOGGER.info("Size of vp-tree: " + tree.size());
    }

    @Override
    public boolean isDuplicate(Hash node) {
        List<Hash> nodes = tree.getAllWithinDistance(node, duplicateThreshold);
        boolean isDuplicate = nodes != null && nodes.size() > 0;
        if (isDuplicate) { duplicatesCounter.increment(); return true; } else { return false; }
    }

    @Override
    public void put(Hash node) {
        tree.add(node);
        sizeCounter.increment();
    }

    @Override
    public void putAll(List<Hash> nodes) {
        tree.addAll(nodes);
        sizeCounter.increment(nodes.size());
    }

    @Override
    public void load(List<String> nodes) {
        if (nodes == null || nodes.isEmpty()) return;
        putAll(nodes.stream().map(h -> mapHash(h, algorithmId)).collect(Collectors.toList()));
    }

    private DistanceFunction<Hash> distanceFunction(boolean isNormalized) {
        return isNormalized ? Hash::normalizedHammingDistance : Hash::hammingDistance;
    }

    private ThresholdSelectionStrategy<Hash, Hash> strategy(int bitResolution) {
        return new SamplingMedianDistanceThresholdSelectionStrategy<>(bitResolution);
    }

    private Hash mapHash(String hash, int algorithmId) {
        return new Hash(new BigInteger(hash), bitResolution, algorithmId);
    }

    private Counter sizeCounter(MeterRegistry registry) {
        return Counter.builder("memes.counter.size")
                .tag("type", "size")
                .description("The counter of nodes in metric tree")
                .register(registry);
    }

    private Counter duplicatesCounter(MeterRegistry registry) {
        return Counter.builder("memes.counter.duplicates")
                .tag("type", "duplicates")
                .description("The counter of found duplicates of memes")
                .register(registry);
    }
}
