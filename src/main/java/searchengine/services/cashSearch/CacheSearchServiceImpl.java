package searchengine.services.cashSearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Service;
import searchengine.config.SearchConfig;
import searchengine.dto.search.SearchDataItem;
import searchengine.model.nosql.CacheSearch;
import searchengine.repository.nosql.CacheSearchRepository;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheSearchServiceImpl implements CacheSearchService {

    private final CacheSearchRepository cacheSearchRepository;
    private final MongoTemplate mongoTemplate;
    private final SearchConfig searchConfig;

    @Override
    public void createCacheByQuery(String query, String site, List<SearchDataItem> searchResult) {
        cacheSearchRepository.save(createCacheSearchObject(query, site, searchResult));
        mongoTemplate.indexOps(CacheSearch.class).ensureIndex(new Index("createdAt", Sort.Direction.ASC)
                        .expire(searchConfig.getCacheLongTtl(), TimeUnit.SECONDS));
    }

    @Override
    public boolean existsByQueryAndSite(String query, String site) {
        return cacheSearchRepository.existsById(query.toLowerCase(Locale.ROOT).trim() + site);
    }

    @Override
    public CacheSearch getByQueryAndSite(String query, String site) {
        return cacheSearchRepository.findById(query.toLowerCase(Locale.ROOT).trim() + site).orElse(null);
    }


    private CacheSearch createCacheSearchObject(String query, String site, List<SearchDataItem> searchResult) {
        CacheSearch cacheSearch = CacheSearch.builder().searchResultId(query.toLowerCase(Locale.ROOT).trim() + site)
                .numberOfResults(searchResult.size()).searchDataItems(searchResult).build();
        cacheSearch.recordCreatedAt();
        return cacheSearch;
    }
}
