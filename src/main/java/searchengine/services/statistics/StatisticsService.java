package searchengine.services.statistics;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.services.response.ResponseService;

@Service
public interface StatisticsService {
    ResponseEntity<ResponseService> getStatistics();
}
