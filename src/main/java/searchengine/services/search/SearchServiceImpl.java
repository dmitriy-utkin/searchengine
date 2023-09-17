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
import searchengine.config.ErrorOptionConfig;
import searchengine.config.SearchConfig;
import searchengine.dto.search.SearchDataItem;
import searchengine.dto.search.SearchQueryResult;
import searchengine.dto.search.SearchQueryObject;
import searchengine.dto.search.SearchQueryPage;
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
    private final ErrorOptionConfig errorOptionConfig;

    //TODO: проверить в кач-ве мер по ускорению - обработка только той части списка, которая будет отражена на фронте
    //TODO: неправильно отрабатывает отображение постранично. На фронте добавляются страницы после перелистывания. Актуально для списков от 10+ результатов поиска

    @Override
    public ResponseEntity<ResponseService> search(String query, String site, int offset, int limit) {
        try {
            if (query.isBlank()) return new ResponseEntity<>(new ResponseServiceImpl.ErrorResponse(errorOptionConfig.getEmptyQuerySearchError()), HttpStatus.BAD_REQUEST);
            Page<SearchDataItem> items = collectSearchDataItems(query, site, offset, limit);
            return new ResponseEntity<>(new ResponseServiceImpl.SearchSuccessResponseService(items), HttpStatus.OK);
        } catch (Exception exception) {
            log.error("Error in method \".search(String query, String site, int offset, int limit)\":" + exception.getMessage());
            return new ResponseEntity<>(new ResponseServiceImpl.ErrorResponse(errorOptionConfig.getInternalServerError()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Page<SearchDataItem> collectSearchDataItems(String query, String site, int offset, int limit) {
        Set<String> lemmas = lemmaFinder.collectLemmas(query).keySet();
        List<SearchQueryResult> pages = collectResultPages(lemmas, site);
        if (pages.isEmpty()) return new PageImpl<>(new ArrayList<>());
        int endIndex = Math.min(offset + limit, pages.size());
        List<SearchDataItem> result = new ArrayList<>();
        Collections.sort(pages);
        pages.subList(offset, endIndex).forEach(page -> result.add(SearchDataItem.builder()
                .relevance(page.getRelRel())
                .title(getTitle(page.getDbPage().getContent()))
                .snippet(createSnippet(page.getDbPage().getContent(), page.getLemmas()))
                .uri(page.getDbPage().getPath())
                .site(page.getDbPage().getDbSite().getUrl())
                .siteName(page.getDbPage().getDbSite().getName())
                .build()));
        return new PageImpl<>(result, PageRequest.of(offset, limit), pages.size());
    }

    private List<SearchQueryResult> collectResultPages(Set<String> lemmas, String siteUrl) {
        List<SearchQueryObject> searchQueryObjects = siteRepository.findByUrl(siteUrl).isPresent() ?
                collectSortedLemmasAsSearchQueryObj(lemmas, siteRepository.findByUrl(siteUrl).get()) : collectSortedLemmasAsSearchQueryObj(lemmas);
        searchQueryObjects.forEach(this::collectIndexes);
        searchQueryObjects.forEach(this::collectSearchQueryPages);
        List<SearchQueryResult> result = getPreparedSearchQueryResultWithRelRelevance(searchQueryObjects.size() == 1 ?
                createSearchQueryResultWithoutRelRelevance(searchQueryObjects.get(0).getSearchQueryPageList()) : filterPagesByExisted(searchQueryObjects));
        result.forEach(searchResult -> searchResult.setLemmas(searchQueryObjects.stream()
                .map(SearchQueryObject::getLemma)
                .collect(Collectors.toSet())));
        return result;
    }

    private List<SearchQueryObject> collectSortedLemmasAsSearchQueryObj(Set<String> searchWords) {
        List<SearchQueryObject> result = new ArrayList<>();
        searchWords.forEach(lemma -> {
            float totalPagesNumber = (float) pageRepository.count();
            Float sumFrequencyByLemma = lemmaRepository.sumFrequencyByLemma(lemma);
            boolean isCorrectLemma = sumFrequencyByLemma != null && (sumFrequencyByLemma / totalPagesNumber * 100 < searchConfig.getMaxFrequencyInPercent());
            if (isCorrectLemma) result.add(createSearchQueryObject(lemma, sumFrequencyByLemma.intValue()));
        });
        return result;
    }

    private List<SearchQueryObject> collectSortedLemmasAsSearchQueryObj(Set<String> searchWords, DBSite site) {
        List<SearchQueryObject> result = new ArrayList<>();
        searchWords.forEach(lemma -> {
            Float totalPagesNumberBySite = pageRepository.countByDbSite(site);
            Float sumFrequencyByDbSiteAndLemma = lemmaRepository.sumFrequencyByDbSiteAndLemma(site, lemma);
            boolean isCorrectLemma = totalPagesNumberBySite != null
                    && sumFrequencyByDbSiteAndLemma != null
                    && (sumFrequencyByDbSiteAndLemma / totalPagesNumberBySite * 100 < searchConfig.getMaxFrequencyInPercent());
            if (isCorrectLemma) result.add(createSearchQueryObject(lemma, sumFrequencyByDbSiteAndLemma.intValue(), site));
        });
        return result;
    }

    private SearchQueryObject createSearchQueryObject(String lemma, int totalFrequencyByLemma, DBSite site) {
        if (lemmaRepository.findByDbSiteAndLemma(site, lemma).isEmpty()) return null;
        return SearchQueryObject.builder()
                .lemma(lemma)
                .totalFrequency(totalFrequencyByLemma)
                .dbLemmaList(List.of(lemmaRepository.findByDbSiteAndLemma(site, lemma).get()))
                .build();
    }

    private SearchQueryObject createSearchQueryObject(String lemma, int totalFrequencyByLemma) {
        return SearchQueryObject.builder()
                .lemma(lemma)
                .totalFrequency(totalFrequencyByLemma)
                .dbLemmaList(lemmaRepository.findByLemma(lemma).get())
                .build();
    }

    private void collectIndexes(SearchQueryObject searchQueryObject) {
        searchQueryObject.setDbIndexList(indexRepository.findByDbLemmaIn(searchQueryObject.getDbLemmaList()));
    }

    private void collectSearchQueryPages(SearchQueryObject searchQueryObject) {
        List<SearchQueryPage> pages = new ArrayList<>();
        searchQueryObject.getDbIndexList().forEach(index -> pages.add(SearchQueryPage.builder()
                                                                                    .dbPage(index.getDbPage())
                                                                                    .rank(index.getRank())
                                                                                    .build()));
        searchQueryObject.setSearchQueryPageList(pages);
    }

    private List<SearchQueryResult> filterPagesByExisted(List<SearchQueryObject> list) {
        if (list.isEmpty()) return new ArrayList<>();
        Collections.sort(list);
        List<SearchQueryResult> result = createSearchQueryResultWithoutRelRelevance(list.get(0).getSearchQueryPageList());
        List<SearchQueryPage> pages = list.get(0).getSearchQueryPageList();
        for (int i = 0; i < list.size() - 1; i++) {
            pages.retainAll(list.get(i + 1).getSearchQueryPageList());
            if (pages.isEmpty()) return new ArrayList<>();
            result = updateSearchQueryResult(result, pages);
        }
        return result;
    }

    private List<SearchQueryResult> getPreparedSearchQueryResultWithRelRelevance(List<SearchQueryResult> preparedPages) {
        if (preparedPages.isEmpty()) return new ArrayList<>();
        int maxAbsRel = preparedPages.stream()
                .map(SearchQueryResult::getAbsRel)
                .max(Comparator.comparingInt(Integer::intValue)).orElse(1);
        preparedPages.forEach(finalPage -> finalPage.setRelRel((double) finalPage.getAbsRel() / maxAbsRel));
        return preparedPages;
    }

    private List<SearchQueryResult> createSearchQueryResultWithoutRelRelevance(List<SearchQueryPage> searchQueryPages) {
        if (searchQueryPages.isEmpty()) return new ArrayList<>();
        return searchQueryPages.stream()
                .map(this::createSearchQueryResultWithoutRelRelevance).collect(Collectors.toList());
    }

    private SearchQueryResult createSearchQueryResultWithoutRelRelevance(SearchQueryPage searchQueryPage) {
        return SearchQueryResult.builder()
                .dbPage(searchQueryPage.getDbPage())
                .absRel(searchQueryPage.getRank()).build();
    }

    private List<SearchQueryResult> updateSearchQueryResult(List<SearchQueryResult> searchQueryResults, List<SearchQueryPage> updatedPages) {
        searchQueryResults.removeIf(result -> !updatedPages.stream()
                .map(SearchQueryPage::getDbPage)
                .toList().contains(result.getDbPage()));
        searchQueryResults.forEach(result -> updatedPages.stream()
                .filter(page -> page.getDbPage().equals(result.getDbPage()))
                .findFirst().ifPresent(updatedPage -> result.setAbsRel(result.getAbsRel() + updatedPage.getRank())));
        return searchQueryResults;
    }

    private String createSnippet(String content, Set<String> query) {
        Map<String, String> equalsWords = lemmaFinder.collectNormalInitialForms(content, query);
        String text = lemmaFinder.convertHtmlToText(content).toLowerCase().trim();
        StringBuilder sb = new StringBuilder();
        int plusAndMinusSnippetLength = searchConfig.getSnippetLength() / query.size() / 2;
        int count = 0;
        for (String word : query) {
            int index = text.indexOf(equalsWords.get(word));
            int start = index == 0 ? 0 : Math.max(text.indexOf(" ", index - plusAndMinusSnippetLength), 0);
            int end = Math.min(text.indexOf(" ", index + plusAndMinusSnippetLength), text.length());
            sb.append(count == 0 ? "..." : "").append(text, start, index).append("<b>").append(equalsWords.get(word)).append("</b>")
                    .append(text, index + equalsWords.get(word).length(), end == -1 ? text.length() : end).append("...");
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
