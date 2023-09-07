package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.SearchConfig;
import searchengine.dto.search.SearchDataItem;
import searchengine.model.DBIndex;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

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

        List<SearchDataItem> items = collectSearchDataItems(getSearchedPages(preparedLemmas));

        return new ResponseEntity<>(new ResponseServiceImpl.SearchSuccessResponseService(items), HttpStatus.OK);
    }

    private Map<DBPage, Integer> getSearchedPages(Map<String, List<DBLemma>> preparedLemmas) {
        Map<DBPage, Map<Integer, String>> result = new HashMap<>();
        Map<DBPage, String> preResult = new HashMap<>();
        Map<String, List<DBIndex>> indexes = collectIndexesByLemmas(preparedLemmas);
        Map<String, Map<DBPage, Integer>> pages = collectPagesByIndexes(indexes);

        return sortPageByExisted(pages);
    }

    //TODO: поправить костыль с сортировкой (добавлется порядковый номер!!!!!)
    //TODO: возвращается значение без сортировки в дальнейшие методы!
    private Map<String, List<DBLemma>> getPreparedLemmas(String query) {
        float totalNumberOfPages = (float) pageRepository.count();
        Map<Float, String> dbLemmas = new TreeMap<>();
        Map<String, List<DBLemma>> result = new HashMap<>();
        lemmaFinder.collectLemmas(query).keySet().stream().toList().forEach(lemma -> {
            Optional<Integer> totalFrequencyByLemma = lemmaRepository.sumFrequencyByLemma(lemma);
            if (totalFrequencyByLemma.isPresent() && (float) totalFrequencyByLemma.get() / totalNumberOfPages * 100 < searchConfig.getMaxFrequencyInPercent()) dbLemmas.put(setSortedMapKey(dbLemmas.keySet(), (float) totalFrequencyByLemma.get()), lemma);
        });
        AtomicInteger count = new AtomicInteger(1);
        dbLemmas.values().forEach(lemma -> {
            result.put(count + lemma, lemmaRepository.findByLemma(lemma).get());
            count.getAndIncrement();
        });
        return result;
    }

    //TODO: переписать таким образом, чтобы из основного метода был только один вызодв, внутри которого будет вызов остальных методов

    private Map<String, List<DBIndex>> collectIndexesByLemmas(Map<String, List<DBLemma>> lemmaDbLemmaMap) {
        return lemmaDbLemmaMap.keySet().stream().collect(Collectors.toMap(Function.identity(), lemma -> indexRepository.findByDbLemmaIn(lemmaDbLemmaMap.get(lemma))));
    }

    private Map<String, Map<DBPage, Integer>> collectPagesByIndexes(Map<String, List<DBIndex>> lemmaDbIndexMap) {
        return lemmaDbIndexMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream().collect(Collectors.toMap(DBIndex::getDbPage, DBIndex::getRank))));
    }

    //TODO: добавить сортировку страниц по значению Integer (lemma_rank)
    private Map<DBPage, Integer> sortPageByExisted(Map<String, Map<DBPage, Integer>> lemmaDbPageMap) {
        Map<DBPage, Integer> result = new HashMap<>();
        if (lemmaDbPageMap.size() == 1) return lemmaDbPageMap.values().iterator().next();
        if (lemmaDbPageMap.isEmpty()) return null;
        lemmaDbPageMap.keySet().forEach(lemma -> {
            Map<DBPage, Integer> semiFinishedList = lemmaDbPageMap.get(lemma);
            if (result.isEmpty()) result.putAll(semiFinishedList);
            else semiFinishedList.keySet().forEach(page -> {
                if (!result.containsKey(page)) result.remove(page);
                else result.put(page, result.get(page) + semiFinishedList.get(page));
            });
        });

        return result;
    }

    private List<SearchDataItem> collectSearchDataItems(Map<DBPage, Integer> pages) {
        if (pages == null) return null;
        List<SearchDataItem> result = new ArrayList<>();
        pages.keySet().forEach(page -> result.add(SearchDataItem.builder()
                        .relevance(pages.get(page))
                        .title(getTitle("title"))
                        .snippet(createSnippet("snippet"))
                        .uri(page.getPath())
                        .site(page.getDbSite().getUrl())
                        .siteName(page.getDbSite().getName())
                        .build()));
        return result;
    }

    private String createSnippet(String content) {
        return content;
    }

    private String getTitle(String content) {
        return content;
    }

    //TODO: придумать, как поправить костыль. Необходим, чтобы в Map`у попадали Lemmas с одинаковыми значениями frequency
    private float setSortedMapKey(Set<Float> dbLemmas, float newKey) {
        while(true) {
            if (!dbLemmas.contains(newKey)) return newKey;
            else newKey += 0.1F;
        }
    }
}
