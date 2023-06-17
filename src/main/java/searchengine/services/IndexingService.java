package searchengine.services;

import org.springframework.http.ResponseEntity;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.DBSite;
import searchengine.model.PageRepository;
import searchengine.model.SiteRepository;

public interface IndexingService {
    DBSite getDBSite(Site site);
    ResponseEntity<IndexResponseService> startIndexing(SitesList sitesList,
                                                       SiteRepository siteRepository,
                                                       PageRepository pageRepository);
}
