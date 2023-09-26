package searchengine.services.cashSearch;

import searchengine.dto.search.SearchDataItem;
import searchengine.model.nosql.CacheSearch;

import java.util.List;

public interface CacheSearchService {
    void createCacheByQuery(String query, String site, List<SearchDataItem> searchResult);
    boolean existsByQueryAndSite(String query, String site);
    CacheSearch getByQueryAndSite(String query, String site);
}
