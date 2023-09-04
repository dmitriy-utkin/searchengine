package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.SearchConfig;
import searchengine.dto.search.SearchDataItem;
import searchengine.model.DBLemma;
import searchengine.model.DBPage;
import searchengine.model.DBSite;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.indexing.LemmaFinder;
import searchengine.services.response.ResponseService;
import searchengine.services.response.ResponseServiceImpl;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaFinder lemmaFinder;
    private final SearchConfig searchConfig;

    private static final String EMPTY_QUERY_SEARCH_ERROR = "Задан пустой поисковый запрос";


    @Override
    public ResponseEntity<ResponseService> search(String query, DBSite dbSite, int offset, int limit) {
        if (query.isBlank()) return new ResponseEntity<>(new ResponseServiceImpl.BadRequest(EMPTY_QUERY_SEARCH_ERROR), HttpStatus.BAD_REQUEST);
        if (offset == 0) offset = searchConfig.getDefaultOffset();
        if (limit == 0) limit = searchConfig.getDefaultLimit();
        Map<String, List<DBLemma>> preparedLemmas = getPreparedLemmas(query.toLowerCase().trim());
        getSearchedPages(preparedLemmas);
//        List<DBPage> preparedPages = collectSearchDataItems(preparedLemmas, 0, new ArrayList<>(), indexRepository, pageRepository);
//        StringBuilder sb = new StringBuilder();
//        preparedPages.forEach(page -> {
//            sb.append("ID: ").append(page.getId()).append("; path: ").append(page.getPath()).append(";\n");
//        });
//        log.info(sb.toString());

//        for (int i = 1; i <= 100; i++) {
//            SearchDataItem item = SearchDataItem.builder()
//                    .site("http://test" + i + ".ru")
//                    .siteName("Test " + i)
//                    .uri("/newPage" + i)
//                    .title("Title " + i)
//                    .snippet("что-то связанное с поисковым запросом " + i)
//                    .relevance(i)
//                    .build();
//            items.add(item);
//        }

        List<SearchDataItem> items = new ArrayList<>();

        return new ResponseEntity<>(new ResponseServiceImpl.SearchSuccessResponseService(items), HttpStatus.OK);
    }

    //TODO: проблема с хранением данных в Map (TreeMap dblemmas) -> не сохраняются леммы с такими же ключами (значение, к примеру freq=2 for A and B, B will replace A.
    private Map<String, List<DBLemma>> getPreparedLemmas(String query) {
        double totalNumberOfPages = (double) pageRepository.count();
        Map<Integer, String> dbLemmas = new TreeMap<>();
        Map<String, List<DBLemma>> result = new HashMap<>();
        lemmaFinder.collectLemmas(query).keySet().stream().toList().forEach(lemma -> {
            if (lemmaRepository.findByLemma(lemma).get().stream().mapToInt(DBLemma::getFrequency).sum() / totalNumberOfPages * 100 < searchConfig.getMaxFrequencyInPercent()) {
                dbLemmas.put(lemmaRepository.findByLemma(lemma).get().stream().mapToInt(DBLemma::getFrequency).sum(), lemma);
            }
        });
        dbLemmas.values().forEach(lemma -> result.put(lemma, lemmaRepository.findByLemma(lemma).get()));
        return result;
    }

    private Map<DBPage, Map<String, Integer>> getSearchedPages(Map<String, List<DBLemma>> preparedLemmas) {
        Map<DBPage, Map<Integer, String>> result = new HashMap<>();
        preparedLemmas.values().forEach(dbLemma -> {
            dbLemma.forEach(lemma -> {
                indexRepository.findByDbLemma(lemma).get().stream().forEach(dbIndex -> {
                    result.put(dbIndex.getDbPage(), Map.of((int)dbIndex.getRank(), dbIndex.getDbLemma().getLemma()));
                });
            });
        });

        StringBuilder sb = new StringBuilder();
//        .append(key.getId()).append(" - ").append(key.getDbSite().getUrl()).append(key.getPath()).
        result.keySet().forEach(key -> sb.append("Page: id ").append(key.getId()).append("; ").append(result.get(key)));
        log.info(sb.toString());
        return null;
    }

    private List<DBPage> collectSearchDataItems() {

        return null;
    }





}
