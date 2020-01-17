package com.clhost.memes.app.worker;

import com.clhost.memes.app.sources.SourceData;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Load's memes from specific sources
 */
public interface MemeLoader {

    /**
     * Accept or not this sourceData for current meme loader
     */
    boolean isAccepted(SourceData sourceData);

    /**
     * Should invoke when memes in storage for {@link SourceData#sourceDesc()} doesn't exists
     */
    List<Future<List<MemeBucket>>> onStartup(SourceData source);

    /**
     * Should invoke when memes exists in storage and needs to get next meme buckets
     */
    Future<List<MemeBucket>> onRegular(SourceData source);
}
