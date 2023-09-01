package searchengine.services.indexing;

import org.springframework.http.ResponseEntity;
import searchengine.services.response.ResponseService;

public interface IndexingService {
    ResponseEntity<ResponseService> startIndexing();
    ResponseEntity<ResponseService> stopIndexing();
    ResponseEntity<ResponseService> indexPage(String url);
}
