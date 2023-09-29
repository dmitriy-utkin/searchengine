package searchengine.services.search.tools;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.SearchConfig;
import searchengine.dto.search.SearchDataItem;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchCacheEngine {

    private final SearchConfig searchConfig;

    private static boolean isCacheClearingLaunched = false;
    private final Map<String, SearchResultItem> cache = new HashMap<>();
    public SearchResultItem getSearchResultItem(String query, String site) {
        return cache.get(prepareSearchCacheKey(query, site));
    }
    
    public void updateCache(String query, String site, List<SearchDataItem> searchDataItems) {
        String key = prepareSearchCacheKey(query, site);
        cache.put(key, createSearchCacheItem(searchDataItems));
        if (!isCacheClearingLaunched) {new Thread(this::clearCache).start(); isCacheClearingLaunched = true;}
    }

    private String prepareSearchCacheKey(String query, String site) {
        return query.toLowerCase(Locale.ROOT).trim() + ((site == null) ? "" : " - " + site);
    }

    private SearchResultItem createSearchCacheItem(List<SearchDataItem> searchDataItems) {
        return SearchResultItem.builder().createdAt(Date.from(Instant.now()))
                .resultCount(searchDataItems.size()).searchResults(searchDataItems).build();
    }
    
    private void clearCache() {
        log.info("Cache clearing module was launched");
        try {
            do {
                Thread.sleep(searchConfig.getClearCacheEvery() * 1_000);
                Date checkPoint = new Date(Date.from(Instant.now()).getTime() - (searchConfig.getCacheTtl() * 1_000));
                Set<String> keysToRemove = cache.keySet().stream()
                        .filter(key -> cache.get(key).getCreatedAt().before(checkPoint))
                        .collect(Collectors.toSet());
                keysToRemove.forEach(cache::remove);
                if (!keysToRemove.isEmpty())log.info("Caches expired: " + keysToRemove.size() + " " + keysToRemove);
            } while (cache.size() > 0);
            isCacheClearingLaunched = false;
            log.info("Cache clearing module was stopped.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
