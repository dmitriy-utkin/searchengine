package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "search-settings")
public class SearchConfig {
    private int maxFrequencyInPercent;
    private int snippetLength;
    private boolean withCache;
    private long cacheTtl;
    private long clearCacheEvery;
}
