package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
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

    private static final int MAX_LEMMA_FREQUENCY = 60;
    private static final String EMPTY_QUERY_SEARCH_ERROR = "Задан пустой поисковый запрос";


    @Override
    public ResponseEntity<ResponseService> search(String query, DBSite dbSite, int offset, int limit) {
        if (query.isBlank()) return new ResponseEntity<>(new ResponseServiceImpl.BadRequest(EMPTY_QUERY_SEARCH_ERROR), HttpStatus.BAD_REQUEST);

        List<DBLemma> preparedQueryLemmas = convertAndSortQueryToLemmasList(query, lemmaFinder, lemmaRepository, pageRepository);
        List<DBPage> preparedPages = collectSearchDataItems(preparedQueryLemmas, 0, new ArrayList<>(), indexRepository, pageRepository);
        StringBuilder sb = new StringBuilder();
        preparedPages.forEach(page -> {
            sb.append("ID: ").append(page.getId()).append("; path: ").append(page.getPath()).append(";\n");
        });
        log.info(sb.toString());

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

    private static List <DBLemma> convertAndSortQueryToLemmasList(String query,
                                                                 LemmaFinder lemmaFinder,
                                                                 LemmaRepository lemmaRepository,
                                                                 PageRepository pageRepository) {
        List<String> lemmas = lemmaFinder.collectLemmas(query.toLowerCase().trim()).keySet().stream().toList();
        TreeMap<Integer, List<DBLemma>> lemmasMap = new TreeMap<>();
        List<DBLemma> dbLemmasList = new ArrayList<>();
        lemmas.forEach(lemma -> {
            int totalFrequency = 0;
            List<DBLemma> dbLemmas = lemmaRepository.findByLemma(lemma).isPresent() ? lemmaRepository.findByLemma(lemma).get() : null;
            for (DBLemma dbLemma : dbLemmas) {
                totalFrequency += dbLemma.getFrequency();
            }
            if ((totalFrequency / (double) pageRepository.findAll().size() * 100 < MAX_LEMMA_FREQUENCY) && totalFrequency > 0) lemmasMap.put(totalFrequency, dbLemmas);
        });
        for (int key : lemmasMap.keySet()) {
            dbLemmasList.addAll(lemmasMap.get(key));
        }
        return dbLemmasList;
    }

    private static List<DBPage> collectSearchDataItems(List<DBLemma> lemmas,
                                                       int lemmaIndex,
                                                       List<DBPage> result,
                                                       IndexRepository indexRepository,
                                                       PageRepository pageRepository) {

        if (lemmaIndex >= lemmas.size() - 1) return result;
        List<DBPage> pages = new ArrayList<>();
        indexRepository.findByDbLemma(lemmas.get(lemmaIndex)).get().forEach(index -> pages.add(index.getDbPage()));
        if (result.size() == 0) result.addAll(pages);
        else pages.forEach(page -> {
            if (!result.contains(page)) result.remove(page);
        });
        collectSearchDataItems(lemmas, lemmaIndex + 1, result, indexRepository, pageRepository);
        return result;
    }





}
