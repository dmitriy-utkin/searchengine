package searchengine.services;

import org.springframework.http.ResponseEntity;

public interface IndexingService {
    ResponseEntity<ResponseService> startIndexing();
    ResponseEntity<ResponseService> stopIndexing();
    ResponseEntity<ResponseService> indexPage(String url);
}
