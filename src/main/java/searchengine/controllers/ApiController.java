package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.SitesList;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.services.ResponseService;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexingService indexingService;

    public ApiController(StatisticsService statisticsService,
                         SitesList sitesList,
                         SiteRepository siteRepository,
                         PageRepository pageRepository,
                         IndexRepository indexRepository,
                         LemmaRepository lemmaRepository,
                         IndexingService indexingService) {
        this.statisticsService = statisticsService;
        this.sitesList = sitesList;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.indexRepository = indexRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexingService = indexingService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<ResponseService> startIndexing() {
        return indexingService.startIndexing(sitesList, siteRepository, pageRepository);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<ResponseService> stopIndexing() {
        return indexingService.stopIndexing(siteRepository);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<ResponseService> indexPage(@RequestParam String url) {
        return indexingService.indexPage(siteRepository, pageRepository, lemmaRepository, indexRepository, url);
    }
}

