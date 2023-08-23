package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResult;

@Slf4j
@Service
public class SearchServiceImpl implements SearchService {

    private final String EMPTY_QUERY_SEARCH_ERROR = "Задан пустой поисковый запрос";


    @Override
    public ResponseEntity<ResponseService> search(String query) {
        if (query.isBlank()) return new ResponseEntity<>(new ResponseServiceImpl.Response.BadRequest(EMPTY_QUERY_SEARCH_ERROR), HttpStatus.BAD_REQUEST);
        String preparedQuery = query.toLowerCase().trim();
        SearchResult searchResult = new SearchResult();
        return new ResponseEntity<>(new ResponseServiceImpl.Response.SearchSuccessResponseService(searchResult), HttpStatus.OK);
    }
}
