package searchengine.services;

import org.springframework.http.ResponseEntity;
import searchengine.config.SitesList;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

public interface IndexingService {
    ResponseEntity<ResponseService> startIndexing(SitesList sitesList,
                                                  SiteRepository siteRepository,
                                                  PageRepository pageRepository);

    ResponseEntity<ResponseService> stopIndexing(SiteRepository siteRepository);
    ResponseEntity<ResponseService> indexPage(SiteRepository siteRepository,
                                              PageRepository pageRepository,
                                              LemmaRepository lemmaRepository,
                                              IndexRepository indexRepository,
                                              String url);
}
