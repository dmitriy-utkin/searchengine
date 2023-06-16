package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexResponse;
import searchengine.dto.indexing.SuccessIndexResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.DBSite;
import searchengine.model.PageRepository;
import searchengine.model.SiteRepository;
import searchengine.model.Status;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

import java.time.LocalDateTime;
import java.util.Date;

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
    public ResponseEntity<IndexResponse> startIndexing() {

        for (Site site : sitesList.getSites()) {
            //TODO: change DB usage from siteRepo to DTO structure
            if (siteRepository.findByUrl(site.getUrl()).isPresent()) {
                siteRepository.deleteByUrl(site.getUrl());
            }
            siteRepository.save(indexingService.getSite(site));
        }

        return new ResponseEntity<>(new SuccessIndexResponse(true), HttpStatus.OK);
    }
}

