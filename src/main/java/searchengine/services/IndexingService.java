package searchengine.services;

import org.springframework.http.ResponseEntity;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.DBSite;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.concurrent.ConcurrentSkipListSet;

public interface IndexingService {
    ConcurrentSkipListSet<String> launchSiteParser(SiteParser siteParser);
    DBSite getDBSite(Site site);
    ResponseEntity<IndexResponseService> startIndexing(SitesList sitesList,
                                                       SiteRepository siteRepository,
                                                       PageRepository pageRepository) throws IOException;
}
