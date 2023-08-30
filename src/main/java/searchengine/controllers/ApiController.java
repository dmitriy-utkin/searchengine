package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.model.DBSite;
import searchengine.services.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    @GetMapping("/statistics")
    public ResponseEntity<ResponseService> statistics() {
        return statisticsService.getStatistics();
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<ResponseService> startIndexing() {
        return indexingService.startIndexing();
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<ResponseService> stopIndexing() {
        return indexingService.stopIndexing();
    }

    @PostMapping("/indexPage")
    public ResponseEntity<ResponseService> indexPage(@RequestParam String url) {
        return indexingService.indexPage(url);
    }

    @GetMapping("/search")
    public ResponseEntity<ResponseService> search(@RequestParam String query,
                                                  @RequestParam(required = false) DBSite dbSite,
                                                  @RequestParam(required = false) Integer offset,
                                                  @RequestParam(required = false) Integer limit) {
        if (offset == null) offset = 0;
        if (limit == null) limit = 20;

        //TODO: проконтроллировать условие, что сайт идет без "/" в конце
        return searchService.search(query, dbSite, offset, limit);
    }
}

