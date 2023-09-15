package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.services.indexing.IndexingService;
import searchengine.services.response.ResponseService;
import searchengine.services.search.SearchService;
import searchengine.services.statistics.StatisticsService;

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
                                                  @RequestParam(required = false) String site,
                                                  @RequestParam(defaultValue = "0") Integer offset,
                                                  @RequestParam(defaultValue = "20") Integer limit) {
        return searchService.search(query, site, offset, limit);
    }
}

