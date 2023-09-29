package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.CacheConfig;
import searchengine.config.ErrorOptionConfig;
import searchengine.config.SearchConfig;
import searchengine.dto.search.SearchDataItem;
import searchengine.dto.search.SearchQueryResult;
import searchengine.dto.search.SearchQueryObject;
import searchengine.dto.search.SearchQueryPage;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.indexing.tools.LemmaFinder;
import searchengine.services.response.ResponseService;
import searchengine.services.response.ResponseServiceImpl;
import searchengine.services.search.tools.SearchCacheEngine;
import searchengine.services.search.tools.SearchResultItem;

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
    private final ErrorOptionConfig errorOptionConfig;
    private final SearchCacheEngine searchCacheEngine;
    private final CacheConfig cacheConfig;

    @Override
    public ResponseEntity<ResponseService> search(String query, String site, int offset, int limit) {
        try {
            if (query.isBlank()) return new ResponseEntity<>(new ResponseServiceImpl
                    .ErrorResponse(errorOptionConfig.getEmptyQuerySearchError()), HttpStatus.BAD_REQUEST);
            if (searchConfig.isWithCache()) {
                SearchResultItem result = searchCacheEngine.getSearchResultItem(query, site);
                if (result != null) {
                    return new ResponseEntity<>(new ResponseServiceImpl
                            .SearchSuccessResponse(getResultPage(result, offset, limit)), HttpStatus.OK);
                }
            }
            Page<SearchDataItem> items = collectSearchDataItems(query, site, offset, limit);
            return new ResponseEntity<>(new ResponseServiceImpl.SearchSuccessResponse(items), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(new ResponseServiceImpl
                    .ErrorResponse(errorOptionConfig.getInternalServerError()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Page<SearchDataItem> collectSearchDataItems(String query, String site, int offset, int limit) {
        Set<String> lemmas = lemmaFinder.collectLemmas(query).keySet();
        List<SearchQueryResult> queryResultList = collectResultPages(lemmas, site);
        new Thread(() -> saveToCacheAllSearchDataItems(query, site, queryResultList)).start();
        if (queryResultList.isEmpty()) return new PageImpl<>(new ArrayList<>());
        int endIndex = Math.min(offset + limit, queryResultList.size());
        List<SearchDataItem> result = new ArrayList<>();
        queryResultList.subList(offset, endIndex).forEach(resultItem -> result.add(createSearchDataItem(resultItem)));
        return getResultPage(result, offset, limit, queryResultList.size());
    }

    private Page<SearchDataItem> getResultPage(List<SearchDataItem> result, int offset, int limit, int size) {
        return new PageImpl<>(result, PageRequest.of(getPageNumber(offset, limit), limit), size);
    }

    private Page<SearchDataItem> getResultPage(SearchResultItem searchResultItem, int offset, int limit) {
        int endIndex = Math.min(offset + limit, searchResultItem.getSearchResults().size());
        return new PageImpl<>(searchResultItem.getSearchResults().subList(offset, endIndex),
                PageRequest.of(getPageNumber(offset, limit), limit), searchResultItem.getResultCount());
    }

    private int getPageNumber(int offset, int limit) {
        return (offset / limit);
    }

    private void saveToCacheAllSearchDataItems(String query, String site, List<SearchQueryResult> queryResultList) {
        if (searchConfig.isWithCache()) {
            List<SearchDataItem> result = queryResultList.stream().map(this::createSearchDataItem).toList();
            searchCacheEngine.updateCache(query, site, result);
        }
    }

    private SearchDataItem createSearchDataItem(SearchQueryResult page) {
        return SearchDataItem.builder().relevance(page.getRelRel()).title(getTitle(page.getDbPage().getContent()))
                .snippet(createSnippet(page.getDbPage().getContent(), page.getLemmas()))
                .uri(page.getDbPage().getPath()).site(page.getDbPage().getSite().getUrl())
                .siteName(page.getDbPage().getSite().getName()).build();
    }

    private List<SearchQueryResult> collectResultPages(Set<String> lemmas, String siteUrl) {
        return getSearchQueryResult(convertLemmaToSearchQueryObj(lemmas, siteUrl));
    }

    private List<SearchQueryObject> convertLemmaToSearchQueryObj(Set<String> searchWords, String siteUrl) {
        Site site = siteRepository.findByUrl(siteUrl).orElse(null);
        long pagesNumber = getTotalPagesNumber(site);
        List<SearchQueryObject> result = new ArrayList<>();
        searchWords.forEach(lemma -> {
            Float sumFrequencyByLemma = getSumFrequencyByLemma(site, lemma);
            if (isCorrectLemma(sumFrequencyByLemma, pagesNumber))
                result.add(createSearchQueryObjectByLemma(lemma, sumFrequencyByLemma.intValue(), site));
        });
        result.forEach(this::collectIndexes);
        result.forEach(this::collectSearchQueryPages);
        return result;
    }

    private long getTotalPagesNumber(Site site) {
        if (site == null) return pageRepository.count();
        return pageRepository.countBySite(site);
    }

    private Float getSumFrequencyByLemma(Site site, String lemma) {
        if (site == null) return lemmaRepository.sumFrequencyByLemma(lemma).orElse(null);
        return lemmaRepository.sumFrequencyBySiteAndLemma(site, lemma).orElse(null);
    }

    private boolean isCorrectLemma(Float sumFrequencyByLemma, long totalPagesNumber) {
        return sumFrequencyByLemma != null && totalPagesNumber > 0
                && (sumFrequencyByLemma / (float) totalPagesNumber * 100 < searchConfig.getMaxFrequencyInPercent());
    }

    private SearchQueryObject createSearchQueryObjectByLemma(String lemma, int frequencyByLemma, Site site) {
        List<Lemma> lemmas;
        if (site == null) lemmas = lemmaRepository.findByLemma(lemma).orElse(null);
        else lemmas = lemmaRepository.findBySiteAndLemma(site, lemma).orElse(null);
        if (lemmas == null) return null;
        return SearchQueryObject.builder().lemma(lemma).totalFrequency(frequencyByLemma).dbLemmaList(lemmas).build();
    }

    private void collectIndexes(SearchQueryObject searchQueryObject) {
        searchQueryObject.setDbIndexList(indexRepository.findByLemmaIn(searchQueryObject.getDbLemmaList()));
    }

    private void collectSearchQueryPages(SearchQueryObject searchQueryObject) {
        List<SearchQueryPage> pages = new ArrayList<>();
        searchQueryObject.getDbIndexList().forEach(index -> pages.add(getSearchQueryPage(index)));
        searchQueryObject.setSearchQueryPageList(pages);
    }

    private SearchQueryPage getSearchQueryPage(Index index) {
        return SearchQueryPage.builder().dbPage(index.getPage()).rank(index.getRank()).build();
    }

    private List<SearchQueryResult> filterPagesByExisted(List<SearchQueryObject> list) {
        if (list.isEmpty()) return new ArrayList<>();
        List<SearchQueryResult> result = createSearchResultList(list.get(0).getSearchQueryPageList());
        if (list.size() == 1) return result;
        List<SearchQueryPage> pages = list.get(0).getSearchQueryPageList();
        for (int i = 0; i < list.size() - 1; i++) {
            pages.retainAll(list.get(i + 1).getSearchQueryPageList());
            if (pages.isEmpty()) return new ArrayList<>();
            updateSearchQueryResult(result, pages);
        }
        return result;
    }

    private List<SearchQueryResult> getSearchQueryResult(List<SearchQueryObject> objects) {
        List<SearchQueryResult> result = filterPagesByExisted(objects);
        if (result.isEmpty()) return new ArrayList<>();
        int maxAbsRel = getMaxAbsRelevance(result);
        result.forEach(finalPage -> finalPage.setRelRel((double) finalPage.getAbsRel() / maxAbsRel));
        Set<String> lemmas = objects.stream().map(SearchQueryObject::getLemma).collect(Collectors.toSet());
        result.forEach(searchResult -> searchResult.setLemmas(lemmas));
        Collections.sort(result);
        return result;
    }

    private int getMaxAbsRelevance(List<SearchQueryResult> result) {
        return result.stream().map(SearchQueryResult::getAbsRel).max(Comparator.comparingInt(Integer::intValue))
                .orElse(1);
    }

    private List<SearchQueryResult> createSearchResultList(List<SearchQueryPage> searchQueryPages) {
        if (searchQueryPages.isEmpty()) return new ArrayList<>();
        return searchQueryPages.stream().map(this::getSearchResultWithoutRelRelevance).collect(Collectors.toList());
    }

    private SearchQueryResult getSearchResultWithoutRelRelevance(SearchQueryPage searchPage) {
        return SearchQueryResult.builder().dbPage(searchPage.getDbPage()).absRel(searchPage.getRank()).build();
    }

    private void updateSearchQueryResult(List<SearchQueryResult> searchResults,List<SearchQueryPage> updatedPages) {
        searchResults.removeIf(result -> !updatedPages.stream().map(SearchQueryPage::getDbPage)
                                        .toList().contains(result.getDbPage()));
        searchResults.forEach(result -> updatedPages.stream()
                .filter(page -> page.getDbPage().equals(result.getDbPage())).findFirst()
                .ifPresent(updatedPage -> result.setAbsRel(result.getAbsRel() + updatedPage.getRank())));
    }

    private String createSnippet(String content, Set<String> query) {
        Map<String, String> equalsWords = lemmaFinder.collectNormalInitialForms(content, query);
        String text = lemmaFinder.convertHtmlToText(content).trim();
        String textToLower = text.toLowerCase();
        StringBuilder sb = new StringBuilder();
        int plusMinusLength = searchConfig.getSnippetLength() / query.size() / 2;
        int count = 0;
        for (String word : query) {
            int index = textToLower.indexOf(equalsWords.get(word));
            int start = index == 0 ? 0 : Math.max(textToLower.indexOf(" ", index - plusMinusLength), 0);
            int fromIndexForEnd = query.size() > 1 ? index + plusMinusLength : start + searchConfig.getSnippetLength();
            int end = Math.min(textToLower.indexOf(" ", fromIndexForEnd), text.length());
            try {
                sb.append(count == 0 ? "..." : "").append(text, start, index)
                        .append("<b>").append(equalsWords.get(word)).append("</b>")
                        .append(text, index + equalsWords.get(word).length(), end == -1 ? text.length() : end)
                        .append("...");
            } catch (Exception e) {
                e.printStackTrace();
                sb.append(count == 0 ? "..." : "").append("<b>").append(equalsWords.get(word)).append("</b>...");
            }
            if (sb.length() >= searchConfig.getSnippetLength()) break;
            count++;
        }
        return sb.toString();
    }

    private String getTitle(String content) {
        try {
            return Jsoup.parse(content).title();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

}
