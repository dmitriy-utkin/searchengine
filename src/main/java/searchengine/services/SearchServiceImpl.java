package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.LemmaDTO;
import searchengine.dto.search.SearchDataItem;
import searchengine.model.DBLemma;
import searchengine.model.DBPage;
import searchengine.model.DBSite;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.*;

@Slf4j
@Service
public class SearchServiceImpl implements SearchService {

    private final int MAX_LEMMA_FREQUENCY = 30;
    private final int MIN_SIZE_OF_QUERY_TO_CLEAN = 3;
    private final String EMPTY_QUERY_SEARCH_ERROR = "Задан пустой поисковый запрос";


    @Override
    public ResponseEntity<ResponseService> search(String query,
                                                  DBSite dbSite,
                                                  int offset,
                                                  int limit,
                                                  LemmaFinder lemmaFinder,
                                                  SiteRepository siteRepository,
                                                  PageRepository pageRepository,
                                                  LemmaRepository lemmaRepository,
                                                  IndexRepository indexRepository) {
        int pageCount = pageRepository.findAll().size();
        if (query.isBlank()) return new ResponseEntity<>(new ResponseServiceImpl.Response.BadRequest(EMPTY_QUERY_SEARCH_ERROR), HttpStatus.BAD_REQUEST);

        //TODO: добавляется лемма, которой нет на сайте (к примеру? мыло)
        Map<String, Integer> queryLemmas = lemmaFinder.collectLemmas(query.toLowerCase().trim());
        List<String> lemmas = new ArrayList<>();
        if (queryLemmas.size() >= MIN_SIZE_OF_QUERY_TO_CLEAN) {
            for (String lemma : queryLemmas.keySet()) {
                //TODO: не отрабатывает проверка, потому что "купить" все равно попадает в выборку. скорее всего нарушена логика в проверке ниже, потому что лемма не дублируется кучу раз, а повышается фреквенси
                if (!((lemmaRepository.findByLemma(lemma).get().size() / pageCount) > MAX_LEMMA_FREQUENCY)) lemmas.add(lemma);
            }
        } else {
            lemmas.addAll(queryLemmas.keySet());
        }
        List<String> preparedQueryLemmas = List.of(sortLemmas(lemmas, lemmaRepository).values().toString());
        List<DBPage> pages = new ArrayList<>();
        List<SearchDataItem> items = collectSearchDataItems(preparedQueryLemmas, pages, pageRepository);

        preparedQueryLemmas.forEach(System.out::println);


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

        return new ResponseEntity<>(new ResponseServiceImpl.Response.SearchSuccessResponseService(items), HttpStatus.OK);
    }

    private static TreeMap<Integer, String> sortLemmas(List<String> lemmas, LemmaRepository lemmaRepository) {
        TreeMap<Integer, String> lemmasMap = new TreeMap<>();
        lemmas.forEach(lemma -> {
            List<DBLemma> dbLemmas = lemmaRepository.findByLemma(lemma).get();
            int totalFrequency = 0;
            for (DBLemma l : dbLemmas) {
                totalFrequency = totalFrequency + l.getFrequency();
            }
            lemmasMap.put(totalFrequency, lemma);
        });

        log.info("LEMMAS: " + lemmasMap);

        return lemmasMap;
    }

    private static List<SearchDataItem> collectSearchDataItems(List<String> lemmas,
                                                               List<DBPage> pages,
                                                               PageRepository pageRepository) {
        List<SearchDataItem> items = new ArrayList<>();
        for (String lemma : lemmas) {

        }

        return items;
    }


}
