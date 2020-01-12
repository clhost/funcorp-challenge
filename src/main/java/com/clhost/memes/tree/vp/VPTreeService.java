package com.clhost.memes.tree.vp;

import com.clhost.memes.tree.MemesDao;
import com.clhost.memes.tree.data.VPTreeDaoNode;
import com.eatthepath.jvptree.DistanceFunction;
import com.eatthepath.jvptree.ThresholdSelectionStrategy;
import com.eatthepath.jvptree.VPTree;
import com.github.kilianB.hash.Hash;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Это должно быть отдельное однопоточное спринг бут приложение (в консуле его зарегаю)
 */
@Service
public class VPTreeService implements VPTreeInterface {

    @Value("${}")
    private Long countOfHashes;

    @Value("${}")
    private Long treePointsThreshold;

    @Value("${}")
    public int bitResolution;

    @Value("${}")
    public double duplicateThreshold;

    @Value("${}")
    private double clearedPercentage;

    private VPTree<VPTreeNode, VPTreeNode> tree;
    private final MemesDao dao;
    private final int algorithmId;
    private final int clearedPointsCount;

    @Autowired
    public VPTreeService(MemesDao dao) {
        this.dao = dao;
        this.tree = new VPTree<>(distanceFunction(), strategy(), loadLastNHashes());
        this.algorithmId = new PerceptiveHash(bitResolution).algorithmId();
        this.clearedPointsCount = clearedPointsCount();
    }

    @Override
    public boolean isDuplicate(VPTreeNode node) {
        return tree.getAllWithinDistance(node, duplicateThreshold).size() > 0;
    }

    @Override
    public void put(VPTreeNode node) {
        if (isThresholdExceeded()) releaseSomeMemory();
        tree.add(node);
    }

    @Override
    public void putAll(List<VPTreeNode> nodes) {
        if (isThresholdExceeded()) releaseSomeMemory();
        tree.addAll(nodes);
    }

    // very expensive operation ?
    private void releaseSomeMemory() {
        List<VPTreeNode> nodes = tree.stream()
                .sorted(Comparator.comparing(VPTreeNode::getDate))
                .skip(clearedPointsCount)
                .collect(Collectors.toList());
        tree = new VPTree<>(distanceFunction(), strategy(), nodes);
    }

    private boolean isThresholdExceeded() {
        return tree.size() >= treePointsThreshold;
    }

    private List<VPTreeNode> loadLastNHashes() {
        return dao.lastNodes(countOfHashes).stream()
                .map(this::mapNode)
                .collect(Collectors.toList());
    }

    private DistanceFunction<VPTreeNode> distanceFunction() {
        return (firstPoint, secondPoint) -> firstPoint.getHash().normalizedHammingDistance(secondPoint.getHash());
    }

    private ThresholdSelectionStrategy<VPTreeNode, VPTreeNode> strategy() {
        return (points, origin, distanceFunction) -> duplicateThreshold;
    }

    private VPTreeNode mapNode(VPTreeDaoNode node) {
        return new VPTreeNode(
                new Hash(BigInteger.valueOf(Long.parseLong(node.getHash())),
                bitResolution, algorithmId), node.getDate());
    }

    private int clearedPointsCount() {
        clearedPercentage = clearedPercentage < 1.0d ? clearedPercentage : 0.2d;
        return (int) (treePointsThreshold * clearedPercentage);
    }
}
