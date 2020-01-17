package com.clhost.memes.tree.vptree;

import com.clhost.memes.tree.dao.MemesDao;
import com.eatthepath.jvptree.DistanceFunction;
import com.eatthepath.jvptree.ThresholdSelectionStrategy;
import com.eatthepath.jvptree.VPTree;
import com.eatthepath.jvptree.util.SamplingMedianDistanceThresholdSelectionStrategy;
import com.github.kilianB.hash.Hash;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VPTreeService implements MetricSpace {
    private static final Logger LOGGER = LogManager.getLogger(VPTreeService.class);

    @Value("${service.tree.duplicate_threshold}")
    private double duplicateThreshold;

    private int bitResolution;
    private VPTree<Hash, Hash> tree;

    private final MemesDao dao;

    @Autowired
    public VPTreeService(MemesDao dao,
                         @Value("${service.tree.bit_resolution}") int bitResolution,
                         @Value("${service.tree.is_normalized_distance}") boolean isNormalized,
                         @Value("${service.tree.count_of_hashes}") long countOfHashes) {
        this.dao = dao;
        this.bitResolution = bitResolution;
        this.tree = new VPTree<>(
                distanceFunction(isNormalized),
                strategy(bitResolution),
                loadLastNHashes(countOfHashes, new PerceptiveHash(bitResolution).algorithmId()));
        LOGGER.info("Size of vp-tree: " + tree.size());
    }

    @Override
    public boolean isDuplicate(Hash node) {
        List<Hash> nodes = tree.getAllWithinDistance(node, duplicateThreshold);
        return nodes != null && nodes.size() > 0;
    }

    @Override
    public void put(Hash node) {
        tree.add(node);
    }

    @Override
    public void putAll(List<Hash> nodes) {
        tree.addAll(nodes);
    }

    private List<Hash> loadLastNHashes(long countOfHashes, int algorithmId) {
        List<String> hashes = dao.lastNodes(countOfHashes);
        return hashes == null
                ? new ArrayList<>()
                : hashes.stream().map(h -> mapHash(h, algorithmId)).collect(Collectors.toList());
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
}
