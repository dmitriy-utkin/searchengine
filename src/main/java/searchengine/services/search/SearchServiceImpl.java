package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.MultiCollectorManager;
import org.jsoup.Jsoup;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.SearchConfig;
import searchengine.dto.search.SearchDataItem;
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
        List<SearchDataItem> items = collectSearchDataItems(query);
        return new ResponseEntity<>(new ResponseServiceImpl.SearchSuccessResponseService(items), HttpStatus.OK);
    }

    //TODO: поправить костыль с сортировкой (добавлется порядковый номер!!!!!)
    //TODO: возвращается значение без сортировки в дальнейшие методы!
    private  Map<DBPage, Integer> preparePageList(String query) {
        float totalNumberOfPages = (float) pageRepository.count();
        List<SearchEngineObject> result = new ArrayList<>();
        lemmaFinder.collectLemmas(query).keySet().forEach(lemma -> {
            Optional<Integer> totalFrequencyByLemma = lemmaRepository.sumFrequencyByLemma(lemma);
            if (totalFrequencyByLemma.isPresent() && (float) totalFrequencyByLemma.get() / totalNumberOfPages * 100 < searchConfig.getMaxFrequencyInPercent()) result.add(SearchEngineObject.builder().lemma(lemma).totalFrequency(totalFrequencyByLemma.get()).dbLemmaList(lemmaRepository.findByLemma(lemma).get()).build());
        });
        if (result.isEmpty() || result == null) return null;
        result.forEach(this::collectIndexes);
        result.forEach(this::collectSearchEnginePages);
        return filterPagesByExisted(result);
    }

    //TODO: переписать таким образом, чтобы из основного метода был только один вызодв, внутри которого будет вызов остальных методов


    private void collectIndexes(SearchEngineObject searchEngineObject) {
        searchEngineObject.setDbIndexList(indexRepository.findByDbLemmaIn(searchEngineObject.getDbLemmaList()));
    }

    private void collectSearchEnginePages(SearchEngineObject searchEngineObject) {
        List<SearchEnginePage> pages = new ArrayList<>();
        searchEngineObject.getDbIndexList().forEach(index -> pages.add(SearchEnginePage.builder().dbPage(index.getDbPage()).rank(index.getRank()).build()));
        searchEngineObject.setSearchEnginePageList(pages);
    }

    private Map<DBPage, Integer> filterPagesByExisted(List<SearchEngineObject> list) {
        if (list.isEmpty()) return null;
        Collections.sort(list);
        Map<DBPage, Integer> dbPageRankMap = list.get(0).getSearchEnginePageList().stream().collect(Collectors.toMap(SearchEnginePage::getDbPage, SearchEnginePage::getRank));
        list.remove(0);
        list.forEach(searchEngineObject -> {
            Map<DBPage, Integer> semiResult = new HashMap<>();
            searchEngineObject.getSearchEnginePageList().forEach(searchEnginePage -> {
                if (dbPageRankMap.containsKey(searchEnginePage.getDbPage())) {
                    semiResult.put(searchEnginePage.getDbPage(), dbPageRankMap.get(searchEnginePage.getDbPage()) + searchEnginePage.getRank());
                }
            });
            dbPageRankMap.clear();
            dbPageRankMap.putAll(semiResult);
        });
        return dbPageRankMap.isEmpty() ? null : dbPageRankMap;
    }

    private List<SearchDataItem> collectSearchDataItems(String query) {
        Map<DBPage, Integer> pages = preparePageList(query);
        List<SearchDataItem> result = new ArrayList<>();
//        Collections.sort(pages);
        if (pages == null) return null;
        pages.keySet().forEach(page -> result.add(SearchDataItem.builder()
                        .relevance(pages.get(page))
                        .title(getTitle(page.getContent()))
                        .snippet(createSnippet(page.getContent(), List.of(query.toLowerCase().trim().split(" "))))
                        .uri(page.getPath())
                        .site(page.getDbSite().getUrl())
                        .siteName(page.getDbSite().getName())
                        .build()));
        return result;
//        return null;
    }

    //TODO: вылетает за пределы текста, сделать проверку
    private String createSnippet(String content, List<String> searchWords) {
        try {
            int mid;
            int endSearchedWord;
            int start;
            int end;
            StringBuilder snippet = new StringBuilder();
            int count = 0;
            for (String word : searchWords) {
                String text = Jsoup.parse(content).text();
                mid = text.toLowerCase().indexOf(word);
                start = text.indexOf(" ",mid - searchConfig.getSnippetLength()) + 1;
                end = text.indexOf(" ", mid + searchConfig.getSnippetLength());
                endSearchedWord = text.indexOf(" ", mid);
                snippet.append(count > 0 ? "" : "...").append(text, start, mid).append("<b>").append(text, mid, endSearchedWord).append("</b>").append(text, endSearchedWord, end).append("...");
                count++;
            }
            return snippet.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private String getTitle(String content) {
        try {
            return Jsoup.parse(content).title();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    //TODO: придумать, как поправить костыль. Необходим, чтобы в Map`у попадали Lemmas с одинаковыми значениями frequency
    private float setSortedMapKey(Set<Float> dbLemmas, float newKey) {
        while(true) {
            if (!dbLemmas.contains(newKey)) return newKey;
            else newKey += 0.1F;
        }
    }
}
