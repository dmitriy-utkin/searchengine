package searchengine.services;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public interface StatisticsService {

    ResponseEntity<ResponseService> getStatistics();
}
