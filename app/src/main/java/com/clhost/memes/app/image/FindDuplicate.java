package com.clhost.memes.app.image;

import com.eatthepath.jvptree.DistanceFunction;
import com.eatthepath.jvptree.ThresholdSelectionStrategy;
import com.github.kilianB.hash.Hash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;


// duplicate only if hamming distance is less than 0.05 (или 0.03)
@Service
public class FindDuplicate {
    public double isDuplicate(File f1, File f2) throws IOException {
        /*SingleImageMatcher matcher = new SingleImageMatcher();
        matcher.addHashingAlgorithm(new PerceptiveHash(32), 0.2);
        matcher.addHashingAlgorithm(new RotPHash(32), 0.2);
        return matcher.checkSimilarity(f1, f2) ? 1 : 0;*/
        HashingAlgorithm algorithm = new PerceptiveHash(65); // todo: как то надо это подбирать
        Hash h1 = algorithm.hash(f1);
        Hash h2 = algorithm.hash(f2);

        System.out.println(h1.getHashValue().toString(2));
        System.out.println(h2.getHashValue().toString(2));
        System.out.println(h1.hammingDistance(h2));

        // 1474 1039
        return h1.normalizedHammingDistance(h2);
    }

    // think: need implement
    public FindDuplicateResult isDuplicate(String hash, int w, int h, byte[] image) throws IOException {
        BufferedImage bufImage = ImageIO.read(new ByteArrayInputStream(image));
        HashingAlgorithm algorithm = new PerceptiveHash(256); // должен быть константен
        return null;
    }

    @Data
    @Builder
    public static class FindDuplicateResult {
        private boolean isDuplicate;
        private String perceptiveHash;
    }

    /*public static void main(String[] args) throws IOException {
        List<Hash> list = new ArrayList<>();
        HashingAlgorithm algorithm = new PerceptiveHash(256);
        list.add(hash("/home/clhost/IdeaProjects/memes/m/abc_up.jpeg", algorithm));

        VPTree<Hash, Hash> tree = new VPTree<>(distanceFunction(), strategy(), list);
        System.out.println(tree.getAllWithinDistance(hash("/home/clhost/IdeaProjects/memes/m/abc_down.jpeg", algorithm), 0.2).size());
    }*/

    private static DistanceFunction<Hash> distanceFunction() {
        return Hash::normalizedHammingDistance;
    }

    private static ThresholdSelectionStrategy<Hash, Hash> strategy() {
        return (points, origin, distanceFunction) -> 0.2d;
    }

    private static Hash hash(String path, HashingAlgorithm algorithm) throws IOException {
        File file = new File(path);
        return algorithm.hash(file);
    }

    /*public static void main(String[] args) throws IOException {
        *//*File f1 = new File("/home/clhost/IdeaProjects/memes/m/abc_up.jpeg");
        File f2 = new File("/home/clhost/IdeaProjects/memes/m/abc_down.jpeg");
        File f3 = new File("/home/clhost/IdeaProjects/memes/m/abc_up_and_down.jpeg");
        File f4 = new File("/home/clhost/IdeaProjects/memes/m/original.png");
        File f5 = new File("/home/clhost/IdeaProjects/memes/m/abc_up_and_down_water.jpeg");
        File f6 = new File("/home/clhost/IdeaProjects/memes/m/another.png");
        File f7 = new File("/home/clhost/IdeaProjects/memes/m/cde_up.jpeg");
        File f8 = new File("/home/clhost/IdeaProjects/memes/m/cdf_up.jpeg");
        File f9 = new File("/home/clhost/IdeaProjects/memes/m/:.jpeg");*//*
        File b1 = new File("/home/clhost/IdeaProjects/memes/m/4ch_130.jpg");
        File b2 = new File("/home/clhost/IdeaProjects/memes/m/4ch_320.jpg");

        FindDuplicate duplicate = new FindDuplicate();
        *//*System.out.println("f1, f1: " + duplicate.isDuplicate(f1, f1));
        System.out.println("f1, f2: " + duplicate.isDuplicate(f1, f2));
        System.out.println("f1, f3: " + duplicate.isDuplicate(f1, f3));
        System.out.println("f1, f4: " + duplicate.isDuplicate(f1, f4));
        System.out.println("f2, f2: " + duplicate.isDuplicate(f2, f2));
        System.out.println("f2, f3: " + duplicate.isDuplicate(f2, f3));
        System.out.println("f2, f4: " + duplicate.isDuplicate(f2, f4));
        System.out.println("f3, f3: " + duplicate.isDuplicate(f3, f3));
        System.out.println("f4, f4: " + duplicate.isDuplicate(f4, f4));
        System.out.println("f4, f5: " + duplicate.isDuplicate(f4, f5));
        System.out.println("f4, f6: " + duplicate.isDuplicate(f4, f6));
        System.out.println("f7, f8: " + duplicate.isDuplicate(f7, f8));
        System.out.println("f4, f9: " + duplicate.isDuplicate(f4, f9));*//*
        System.out.println("b1, b2: " + duplicate.isDuplicate(b1, b2));
    }*/
}
