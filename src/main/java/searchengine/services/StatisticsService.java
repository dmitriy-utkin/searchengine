package searchengine.services;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

@Service
public interface StatisticsService {

    ResponseEntity<ResponseService> getStatistics(SiteRepository siteRepository,
                                                  PageRepository pageRepository,
                                                  LemmaRepository lemmaRepository,
                                                  IndexRepository indexRepository);
}
