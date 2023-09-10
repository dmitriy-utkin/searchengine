package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.ErrorOptionConfig;
import searchengine.config.SearchConfig;
import searchengine.dto.search.SearchDataItem;
import searchengine.dto.search.SearchEngineFinalPage;
import searchengine.dto.search.SearchEngineObject;
import searchengine.dto.search.SearchEnginePage;
import searchengine.model.DBPage;
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
    private final ErrorOptionConfig errorOptionConfig;

    @Override
    public ResponseEntity<ResponseService> search(String query, String site, int offset, int limit) {
        if (query.isBlank()) return new ResponseEntity<>(new ResponseServiceImpl.BadRequest(errorOptionConfig.getEmptyQuerySearchError()), HttpStatus.BAD_REQUEST);
        if (offset == 0) offset = searchConfig.getDefaultOffset();
        if (limit == 0) limit = searchConfig.getDefaultLimit();
        List<SearchDataItem> items = collectSearchDataItems(query, site);
        return new ResponseEntity<>(new ResponseServiceImpl.SearchSuccessResponseService(items), HttpStatus.OK);
    }

    private  List<SearchEngineFinalPage> preparePageList(Set<String> searchWords, String siteUrl) {
        float totalNumberOfPages = (float) pageRepository.count();
        List<SearchEngineObject> result = new ArrayList<>();
        searchWords.forEach(lemma -> {
                Optional<Integer> totalFrequencyByLemma = lemmaRepository.sumFrequencyByLemma(lemma);
                float percent = (float) totalFrequencyByLemma.get() / totalNumberOfPages * 100;
                if (totalFrequencyByLemma.isPresent() && (float) totalFrequencyByLemma.get() / totalNumberOfPages * 100 < searchConfig.getMaxFrequencyInPercent()) {
                    result.add(SearchEngineObject.builder()
                            .lemma(lemma)
                            .totalFrequency(totalFrequencyByLemma.get())
                            //TODO: если передавать сайт и по нему не будет найдено 0 лемм, ошибка - no value present
                            .dbLemmaList(siteRepository.findByUrl(siteUrl).isPresent() ? List.of(lemmaRepository.findByDbSiteAndLemma(siteRepository.findByUrl(siteUrl).get(), lemma).get()) : lemmaRepository.findByLemma(lemma).get())
                            .build());
                }
            });
//        if (result.isEmpty() || result == null) return null;
        result.forEach(this::collectIndexes);
        result.forEach(this::collectSearchEnginePages);
        return filterPagesByExisted(result);
    }

    private void collectIndexes(SearchEngineObject searchEngineObject) {
        searchEngineObject.setDbIndexList(indexRepository.findByDbLemmaIn(searchEngineObject.getDbLemmaList()));
    }

    private void collectSearchEnginePages(SearchEngineObject searchEngineObject) {
        List<SearchEnginePage> pages = new ArrayList<>();
        searchEngineObject.getDbIndexList().forEach(index -> pages.add(SearchEnginePage.builder().dbPage(index.getDbPage()).rank(index.getRank()).build()));
        searchEngineObject.setSearchEnginePageList(pages);
    }

    private List<SearchEngineFinalPage> filterPagesByExisted(List<SearchEngineObject> list) {
        if (list.isEmpty()) return null;
        Collections.sort(list);
        List<SearchEngineFinalPage> finalPageList = new ArrayList<>();
        list.get(0).getSearchEnginePageList().forEach(searchEnginePage -> finalPageList.add(SearchEngineFinalPage.builder().dbPage(searchEnginePage.getDbPage()).absRel(searchEnginePage.getRank()).build()));
        List<DBPage> dbPagesToCheck = new ArrayList<>(finalPageList.stream().map(SearchEngineFinalPage::getDbPage).toList());
//        list.remove(0);
        //TODO: переписать метод, чтобы не использовать список DBPage, заменить на простой стрим со сбором страниц из finalEnginePage
        //TODO: проверить, как должен отрабатывать метод, если лист пустой. Может, изменить выдачу на "false"?

        for (SearchEngineObject searchEngineObject : list) {
            List<SearchEngineFinalPage> semiFinalPageList = new ArrayList<>();
            List<DBPage> semiDbPagesToCheck = new ArrayList<>();
            for (SearchEnginePage searchEnginePage : searchEngineObject.getSearchEnginePageList()) {
                if (dbPagesToCheck.contains(searchEnginePage.getDbPage())) {
                    semiFinalPageList.add(SearchEngineFinalPage.builder().dbPage(searchEnginePage.getDbPage()).absRel(searchEnginePage.getRank()).build());
                    semiDbPagesToCheck.add(searchEnginePage.getDbPage());
                }
            }
            //TODO: заменить метод установки maxAbsRel -> чтобы просто stream вычленл максимум по списку оставшихся страниц
            finalPageList.clear();
            finalPageList.addAll(semiFinalPageList);
            dbPagesToCheck.clear();
            dbPagesToCheck.addAll(semiDbPagesToCheck);
        }
        if (finalPageList.isEmpty()) return null;
        int maxAbsRel = finalPageList.stream().map(SearchEngineFinalPage::getAbsRel).max(Comparator.comparingInt(Integer::intValue)).orElse(1);
        //TODO: неправильно заполняется "максимальная релевантность ABS, поправить
        finalPageList.forEach(finalPage -> finalPage.setRelRel((double) finalPage.getAbsRel() / maxAbsRel));
        return finalPageList;
    }

    //TODO: затестить поиск на большом резульатею к примеру на "вебинары" выдает релевантность, равную > 1
    private List<SearchDataItem> collectSearchDataItems(String query, String site) {
        Set<String> searchWords = lemmaFinder.collectLemmas(query).keySet();
        List<SearchEngineFinalPage> pages = preparePageList(searchWords, site);
        if (pages == null) return null;
        List<SearchDataItem> result = new ArrayList<>();
        Collections.sort(pages);
        pages.forEach(page -> result.add(SearchDataItem.builder()
                        .relevance(page.getRelRel())
                        .title(getTitle(page.getDbPage().getContent()))
                        .snippet(createSnippet(page.getDbPage().getContent(), searchWords))
                        .uri(page.getDbPage().getPath())
                        .site(page.getDbPage().getDbSite().getUrl())
                        .siteName(page.getDbPage().getDbSite().getName())
                        .build()));
        return result;
    }

    //TODO: вылетает за пределы текста, сделать проверку
    private String createSnippet(String content, Set<String> searchWords) {
        try {
            String text = Jsoup.parse(content).text();
            int mid;
            int endSearchedWord;
            int start;
            int end;
            StringBuilder snippet = new StringBuilder("<p>");
            int count = 0;
            for (String word : searchWords) {
                mid = text.toLowerCase().indexOf(word);
                start = text.indexOf(" ",mid - searchConfig.getSnippetLength()) + 1;
                end = text.indexOf(" ", mid + searchConfig.getSnippetLength());
                endSearchedWord = text.indexOf(" ", mid);
                snippet.append(count > 0 ? "" : "...").append(text, start, mid).append("<b>").append(text, mid, endSearchedWord).append("</b>").append(text, endSearchedWord, end).append("...");
                count++;
            }
            snippet.append("</p>");
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
