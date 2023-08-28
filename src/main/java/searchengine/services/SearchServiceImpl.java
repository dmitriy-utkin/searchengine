package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchDataItem;
import searchengine.model.DBIndex;
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

    private static final int MAX_LEMMA_FREQUENCY = 60;
    private static final String EMPTY_QUERY_SEARCH_ERROR = "Задан пустой поисковый запрос";


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

        List<DBLemma> preparedQueryLemmas = convertAndSortQueryToLemmasList(query, lemmaFinder, lemmaRepository, pageRepository);
        List<DBPage> preparedPages = collectSearchDataItems(preparedQueryLemmas, new ArrayList<>(), indexRepository, pageRepository);

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

        return new ResponseEntity<>(new ResponseServiceImpl.Response.SearchSuccessResponseService(items), HttpStatus.OK);
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
                                                               List<DBPage> inputDbPagesList,
                                                               IndexRepository indexRepository,
                                                               PageRepository pageRepository) {
        LinkedHashMap<DBPage, DBIndex> dbPageDbIndexMap = new LinkedHashMap<>();
        LinkedHashMap<DBPage, Float> preparedResultDbPageAndAbsRelRelevant = new LinkedHashMap<>();
        List<DBPage> outputDbPagesList = new ArrayList<>();
        if (inputDbPagesList.size() == 0) {

        }
        //TODO: check how it was found wrong lemma "танец" if on the page it is not exist

        double maxAbs = 0D;
        for (DBLemma lemma : lemmas) {
            List<DBIndex> indexes = indexRepository.findByDbLemma(lemma).get();
            for (DBIndex index : indexes) {
                dbPageDbIndexMap.put(index.getDbPage(), index);
                if (index.getRank() > maxAbs) maxAbs = index.getRank();
                //TODo: проблема с последующим добавлением значений, как правильно получить данные, если уже есть одна зпасить, создастся дубликат? Или это правильно и нужно будет просто по ранжиру сортить вниз списка
                preparedResultDbPageAndAbsRelRelevant.put(index.getDbPage(), index.getRank());
            }
        }
        StringBuilder sb = new StringBuilder();
        int count = 1;
        for (DBPage page : dbPageDbIndexMap.keySet()) {
            sb.append(count).append(": \n");
            sb.append("Page id: ").append(page.getId()).append("; page url: ").append(page.getDbSite().getUrl()).append(page.getPath()).append("\n");
            sb.append("Index (page id): ").append(dbPageDbIndexMap.get(page).getDbPage().getId()).append("; rank: ").append(dbPageDbIndexMap.get(page).getRank()).append("; lemma: ").append(dbPageDbIndexMap.get(page).getDbLemma().getLemma()).append(".\n------\n");
            count++;
        }
        log.info(String.valueOf(sb));
        return outputDbPagesList;
    }





}
