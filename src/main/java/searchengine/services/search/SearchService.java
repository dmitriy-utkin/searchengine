package searchengine.services.search;

import org.springframework.http.ResponseEntity;
import searchengine.model.DBSite;
import searchengine.services.response.ResponseService;

public interface SearchService {
    ResponseEntity<ResponseService> search(String query, DBSite dbSite, int offset, int limit);
}
