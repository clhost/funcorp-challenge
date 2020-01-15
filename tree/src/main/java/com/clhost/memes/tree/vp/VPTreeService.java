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

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Это должно быть отдельное однопоточное спринг бут приложение (в консуле его зарегаю)
 */
@Service
public class VPTreeService implements VPTreeInterface {

    @Value("${service.tree.count_of_hashes}")
    private Long countOfHashes;

    @Value("${service.tree.tree_max_count}")
    private Long treeMaxCount;

    @Value("${service.tree.duplicate_threshold}")
    private double duplicateThreshold;

    @Value("${service.tree.cleared_percentage}")
    private double clearedPercentage;

    private VPTree<VPTreeNode, VPTreeNode> tree;

    private final MemesDao dao;
    private final int algorithmId;

    private int bitResolution;
    private int clearedPointsCount;

    @Autowired
    public VPTreeService(MemesDao dao, @Value("${service.tree.bit_resolution}") int bitResolution) {
        this.dao = dao;
        this.bitResolution = bitResolution;
        this.tree = new VPTree<>(distanceFunction(), strategy(), loadLastNHashes());
        this.algorithmId = new PerceptiveHash(bitResolution).algorithmId();
    }

    @PostConstruct
    public void post() {
        this.clearedPointsCount = clearedPointsCount();
    }

    @Override
    public boolean isDuplicate(VPTreeNode node) {
        List<VPTreeNode> nodes = tree.getAllWithinDistance(node, duplicateThreshold);
        return nodes != null && nodes.size() > 0;
    }

    @Override
    public void put(VPTreeNode node) {
        if (isThresholdExceeded()) releaseSomeMemory();
        System.out.println("Put node=" + node);
        tree.add(node);
    }

    @Override
    public void putAll(List<VPTreeNode> nodes) {
        if (isThresholdExceeded()) releaseSomeMemory();
        tree.addAll(nodes);
    }

    // very expensive operation ? (кажется это какая-то хуйня)
    private void releaseSomeMemory() {
        System.err.println("Memory released");
        List<VPTreeNode> nodes = tree.stream()
                .sorted(Comparator.comparing(VPTreeNode::getDate))
                .skip(clearedPointsCount)
                .collect(Collectors.toList());
        tree = new VPTree<>(distanceFunction(), strategy(), nodes);
    }

    private boolean isThresholdExceeded() {
        return tree.size() >= treeMaxCount;
    }

    private List<VPTreeNode> loadLastNHashes() {
        /*List<VPTreeDaoNode> nodes = dao.lastNodes(countOfHashes);
        return nodes == null
                ? new ArrayList<>()
                : nodes.stream().map(this::mapNode).collect(Collectors.toList());*/
        return new ArrayList<>();
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
        return (int) (treeMaxCount * clearedPercentage);
    }
}
