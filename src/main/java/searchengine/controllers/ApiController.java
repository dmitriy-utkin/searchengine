package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.SitesList;
import searchengine.services.ResponseService;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexingService indexingService;

    public ApiController(StatisticsService statisticsService,
                         SitesList sitesList,
                         SiteRepository siteRepository,
                         PageRepository pageRepository,
                         IndexingService indexingService) {
        this.statisticsService = statisticsService;
        this.sitesList = sitesList;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
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
}

