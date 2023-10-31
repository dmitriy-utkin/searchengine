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

import java.util.*;
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
    private final ErrorOptionConfig errorOptionConfig;

    @Override
    public ResponseEntity<ResponseService> search(String query, String site, int offset, int limit) {
        try {
            if (query.isBlank()) {
                return new ResponseEntity<>(new ResponseServiceImpl
                    .ErrorResponse(errorOptionConfig.getEmptyQuerySearchError()), HttpStatus.BAD_REQUEST);
            }
            Page<SearchDataItem> items = collectSearchDataItems(query, site, offset, limit);
            log.info("Found " + items.getTotalElements() + " pages by search query \"" + query.trim() + "\".");
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
        if (queryResultList.isEmpty()) {return new PageImpl<>(new ArrayList<>());}
        int endIndex = Math.min(offset + limit, queryResultList.size());
        List<SearchDataItem> result = new ArrayList<>();
        queryResultList.subList(offset, endIndex).forEach(resultItem -> result.add(createSearchDataItem(resultItem)));
        return getResultPage(result, offset, limit, queryResultList.size());
    }

    private List<SearchQueryResult> collectResultPages(Set<String> lemmas, String siteUrl) {
        return getSearchQueryResult(convertLemmaToSearchQueryObj(lemmas, siteUrl));
    }

    private List<SearchQueryObject> convertLemmaToSearchQueryObj(Set<String> searchWords, String siteUrl) {
        Site site = siteRepository.findByUrl(siteUrl).orElse(null);
        List<SearchQueryObject> result = new ArrayList<>();
        searchWords.forEach(word -> {
            SearchQueryObject object = createSearchQueryObjectByLemma(word, site);
            if (object != null) {result.add(object);}
        });
        return result;
    }

    private SearchQueryPage createSearchQueryPage(Index index) {
        return SearchQueryPage.builder().dbPage(index.getPage()).rank(index.getRank()).build();
    }

    private SearchQueryObject createSearchQueryObjectByLemma(String lemma, Site site) {
        List<Lemma> lemmas;
        if (site == null) {lemmas = lemmaRepository.findByLemma(lemma).orElse(null);}
        else lemmas = lemmaRepository.findBySiteAndLemma(site, lemma).orElse(null);
        if (lemmas == null) {return null;}
        Map<Site, Long> pagesBySites = siteRepository.findAll().stream()
                .collect(Collectors.toMap(Function.identity(), pageRepository::countBySiteWithCache));
        lemmas = lemmas.stream().filter(checkedLemma -> isCorrectLemma(checkedLemma, pagesBySites)).toList();
        if (lemmas.isEmpty()) {return null;}
        List<SearchQueryPage> pages = indexRepository.findByLemmaIn(lemmas).stream()
                .map(this::createSearchQueryPage).toList();
        return SearchQueryObject.builder().lemma(lemma)
                .totalFrequency(lemmas.stream().mapToInt(Lemma::getFrequency).sum())
                .dbLemmaList(lemmas).searchQueryPageList(pages).build();
    }

    private List<SearchQueryResult> getSearchQueryResult(List<SearchQueryObject> objects) {
        List<SearchQueryResult> result = filterPagesByExisted(objects);
        if (result.isEmpty()) {return new ArrayList<>();}
        int maxAbsRel = getMaxAbsRelevance(result);
        result.forEach(finalPage -> finalPage.setRelRel((double) finalPage.getAbsRel() / maxAbsRel));
        Set<String> lemmas = objects.stream().map(SearchQueryObject::getLemma).collect(Collectors.toSet());
        result.forEach(searchResult -> searchResult.setLemmas(lemmas));
        Collections.sort(result);
        return result;
    }

    private List<SearchQueryResult> filterPagesByExisted(List<SearchQueryObject> list) {
        if (list.isEmpty()) {return new ArrayList<>();}
        List<SearchQueryResult> result = createSearchResultList(list.get(0).getSearchQueryPageList());
        if (list.size() == 1) {return result;}
        List<SearchQueryPage> pages = new ArrayList<>(list.get(0).getSearchQueryPageList());
        for (int i = 0; i < list.size() - 1; i++) {
            List<SearchQueryPage> currentPages = list.get(i + 1).getSearchQueryPageList();
            List<SearchQueryPage> tempPages = new ArrayList<>(pages);
            tempPages.retainAll(currentPages);
            if (pages.isEmpty()) {return new ArrayList<>();}
            updateSearchQueryResult(result, tempPages);
            pages = tempPages;
        }
        return result;
    }

    private void updateSearchQueryResult(List<SearchQueryResult> searchResults,List<SearchQueryPage> updatedPages) {
        searchResults.removeIf(result -> !updatedPages.stream().map(SearchQueryPage::getDbPage)
                .toList().contains(result.getDbPage()));
        searchResults.forEach(result -> updatedPages.stream()
                .filter(page -> page.getDbPage().equals(result.getDbPage())).findFirst()
                .ifPresent(updatedPage -> result.setAbsRel(result.getAbsRel() + updatedPage.getRank())));
    }

    private List<SearchQueryResult> createSearchResultList(List<SearchQueryPage> searchQueryPages) {
        if (searchQueryPages.isEmpty()) {return new ArrayList<>();}
        return searchQueryPages.stream().map(this::createSearchResultWithoutRelRelevance).collect(Collectors.toList());
    }

    private SearchQueryResult createSearchResultWithoutRelRelevance(SearchQueryPage searchPage) {
        return SearchQueryResult.builder().dbPage(searchPage.getDbPage()).absRel(searchPage.getRank()).build();
    }

    private int getMaxAbsRelevance(List<SearchQueryResult> result) {
        return result.stream().map(SearchQueryResult::getAbsRel).max(Comparator.comparingInt(Integer::intValue))
                .orElse(1);
    }

    private Page<SearchDataItem> getResultPage(List<SearchDataItem> result, int offset, int limit, int size) {
        return new PageImpl<>(result, PageRequest.of(getPageNumber(offset, limit), limit), size);
    }

    private int getPageNumber(int offset, int limit) {
        return (offset / limit);
    }

    private SearchDataItem createSearchDataItem(SearchQueryResult page) {
        return SearchDataItem.builder().relevance(page.getRelRel()).title(createTitle(page.getDbPage().getContent()))
                .snippet(createSnippet(page.getDbPage().getContent(), page.getLemmas()))
                .uri(page.getDbPage().getPath()).site(page.getDbPage().getSite().getUrl())
                .siteName(page.getDbPage().getSite().getName()).build();
    }

    private boolean isCorrectLemma(Lemma lemma, Map<Site, Long> pagesBySite) {
        return ((float) lemma.getFrequency() / (float) pagesBySite.get(lemma.getSite()) * 100
                < searchConfig.getMaxFrequencyInPercent());
    }

    private String createSnippet(String content, Set<String> query) {
        int targetLength = searchConfig.getSnippetLength();
        String text = lemmaFinder.convertHtmlToText(content).trim();
        String lowerCaseText = text.toLowerCase();
        TreeMap<Integer, String> indexOfSearchedWords = lemmaFinder.collectNormalInitialForms(lowerCaseText, query);
        StringBuilder sb = new StringBuilder();
        int appendix = targetLength / query.size() / 2;
        indexOfSearchedWords.keySet().forEach(key -> {
            try {
                int start = Math.max(lowerCaseText.indexOf(" ", key - appendix), 0);
                int wordEnd = lowerCaseText.indexOf(" ", key);
                int end = Math.min(lowerCaseText.indexOf(" ", key + appendix), text.length());
                sb.append(sb.isEmpty() ? "..." : "");
                sb.append(text, Math.min(start, key), key);
                sb.append("<b>");
                sb.append(text, key, wordEnd);
                sb.append("</b>");
                sb.append(text, wordEnd, end == -1 ? text.length() : end);
                sb.append("...");
            } catch (Exception e) {
                log.error(e.getMessage());
                sb.append("...<b>");
                sb.append(key);
                sb.append("</b...");
            }
        });
        return sb.toString();
    }

    private String createTitle(String content) {
        try {
            return Jsoup.parse(content).title();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

}
