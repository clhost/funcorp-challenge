package com.clhost.memes.sources;

import java.util.List;

/**
 * Provides sources from any configuration for memes loading
 */
public interface SourcesProvider {

    /**
     * @return sources represented by {@link SourceData}
     */
    List<SourceData> sources();
}
