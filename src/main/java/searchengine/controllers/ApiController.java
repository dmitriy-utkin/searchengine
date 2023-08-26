package searchengine.controllers;

import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.hibernate.annotations.Parameter;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.SitesList;
import searchengine.model.DBSite;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.services.*;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;

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
    private final LemmaFinder lemmaFinder;
    private final SearchService searchService;

    public ApiController(StatisticsService statisticsService,
                         SitesList sitesList,
                         SiteRepository siteRepository,
                         PageRepository pageRepository,
                         IndexRepository indexRepository,
                         LemmaRepository lemmaRepository,
                         IndexingService indexingService,
                         SearchService searchService) throws IOException {
        this.statisticsService = statisticsService;
        this.sitesList = sitesList;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.indexRepository = indexRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexingService = indexingService;
        this.lemmaFinder = new LemmaFinder(new RussianLuceneMorphology());
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<ResponseService> statistics() {
        return statisticsService.getStatistics(siteRepository, pageRepository, lemmaRepository, indexRepository);
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<ResponseService> startIndexing() {
        return indexingService.startIndexing(sitesList, siteRepository, pageRepository, lemmaRepository, indexRepository, lemmaFinder);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<ResponseService> stopIndexing() {
        return indexingService.stopIndexing(siteRepository);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<ResponseService> indexPage(@RequestParam String url) {
        return indexingService.indexPage(siteRepository, pageRepository, lemmaRepository, indexRepository, url, lemmaFinder);
    }

    @GetMapping("/search")
    public ResponseEntity<ResponseService> search(@RequestParam String query,
                                                  @RequestParam(required = false) DBSite dbSite,
                                                  @RequestParam(required = false) Integer offset,
                                                  @RequestParam(required = false) Integer limit) {
        if (offset == null) offset = 0;
        if (limit == null) limit = 20;

        //TODO: проконтроллировать условие, что сайт идет без "/" в конце
        return searchService.search(query, dbSite, offset, limit, lemmaFinder, siteRepository, pageRepository, lemmaRepository, indexRepository);
    }
}

