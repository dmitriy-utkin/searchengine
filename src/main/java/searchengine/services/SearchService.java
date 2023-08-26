package searchengine.services;

import org.springframework.http.ResponseEntity;
import searchengine.model.DBSite;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

public interface SearchService {
    ResponseEntity<ResponseService> search(String query,
                                           DBSite dbSite,
                                           int offset,
                                           int limit,
                                           LemmaFinder lemmaFinder,
                                           SiteRepository siteRepository,
                                           PageRepository pageRepository,
                                           LemmaRepository lemmaRepository,
                                           IndexRepository indexRepository);
}
