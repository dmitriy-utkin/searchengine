package searchengine.services.cashSearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchDataItem;
import searchengine.model.nosql.CacheSearch;
import searchengine.repository.nosql.CacheSearchRepository;
import searchengine.services.cashSearch.CacheSearchService;

import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheSearchServiceImpl implements CacheSearchService {

    private final CacheSearchRepository cacheSearchRepository;

    @Override
    public void createCacheByQuery(String query, String site, List<SearchDataItem> searchResult) {
        cacheSearchRepository.save(createCacheSearchObject(query, site, searchResult));
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
                .numberOfResult(searchResult.size()).searchDataItems(searchResult).build();
        cacheSearch.populateCreatedAt();
        return cacheSearch;
    }
}
