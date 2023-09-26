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
    private int defaultOffset;
    private int defaultLimit;
    private int snippetLength;
    private int maxQueryLengthToSkipChecking;
    protected boolean withCache;
}
