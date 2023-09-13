package searchengine.services.search;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import searchengine.services.response.ResponseService;

public interface SearchService {
    ResponseEntity<ResponseService> search(String query, String siteUrl, int offset, int limit);
}
