package searchengine.services;

import org.springframework.http.ResponseEntity;
import searchengine.config.SitesList;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

public interface IndexingService {
    ResponseEntity<ResponseService> startIndexing(SitesList sitesList,
                                                  SiteRepository siteRepository,
                                                  PageRepository pageRepository);
}
