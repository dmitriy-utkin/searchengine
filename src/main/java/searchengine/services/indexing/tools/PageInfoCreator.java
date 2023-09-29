package searchengine.services.indexing.tools;

import lombok.Getter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.config.JsoupConfig;
import searchengine.dto.indexing.ExistedLemma;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.LemmaRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Getter
public class PageInfoCreator {
    public PageInfoCreator(Site site, String url, JsoupConfig config,
                           LemmaFinder lemmaFinder, LemmaRepository lemmaRepository) {
        this.site = site;
        this.url = url;
        Connection.Response response = getResponse(url, config);
        this.doc = getDoc(response);
        this.page = createPage(url, site, response, doc);
        this.lemmas = new ArrayList<>();
        this.indexes = new ArrayList<>();
        collectLemmasAndIndexes(isCorrectCodeCheck(page), lemmaFinder, lemmaRepository);
    }

    private final Document doc;
    private final String url;
    private final Page page;
    private final Site site;
    private List<Lemma> lemmas;
    private List<Index> indexes;

    private Connection.Response getResponse(String url, JsoupConfig config) {
        try {
            return Jsoup.connect(url).userAgent(config.getUserAgent()).referrer(config.getReferrer())
                    .timeout(config.getTimeout()).ignoreHttpErrors(config.isIgnoreHttpErrors())
                    .followRedirects(config.isRedirect()).execute();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Document getDoc(Connection.Response response) {
        try {
            if (response == null) return null;
            return response.parse();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Page createPage(String url, Site site, Connection.Response response, Document doc) {
        if (response == null || doc == null) return null;
        return Page.builder().path(url.replace(site.getUrl(), "")).site(site).code(response.statusCode())
                .content(doc.outerHtml()).build();
    }

    private void collectLemmasAndIndexes(boolean isCorrectCode, LemmaFinder lemmaFinder,
                                         LemmaRepository lemmaRepository) {
        if (!isCorrectCode) {lemmas = null; indexes = null; return;}
        Map<String, Integer> lemmasMap = lemmaFinder.collectLemmas(page.getContent());
        Optional<List<Lemma>> dbLemmas = lemmaRepository.findAllBySite(site);
        final List<ExistedLemma> existedBySiteLemmas = new ArrayList<>();
        dbLemmas.ifPresent(dbLemmaList -> existedBySiteLemmas.addAll(dbLemmaList.stream()
                .map(ExistedLemma::new).toList()));
        lemmasMap.keySet().forEach(lemma -> {
            ExistedLemma existedLemma = existedBySiteLemmas.stream()
                    .filter(l -> l.getDbLemma().getLemma().equals(lemma)).findFirst().orElse(null);
            if (existedLemma != null) {
                updateLemmaAndIndexesLists(updateLemmaFrequency(existedLemma.getDbLemma()), lemmasMap);
            } else {
                updateLemmaAndIndexesLists(createLemmaEntry(site, lemma), lemmasMap);
            }
        });
    }

    private boolean isCorrectCodeCheck(Page page) {
        if (page == null) return false;
        return !String.valueOf(page.getCode()).startsWith("4") || !String.valueOf(page.getCode()).startsWith("5");
    }

    private void updateLemmaAndIndexesLists(Lemma dbLemma, Map<String, Integer> lemmasMap) {
        lemmas.add(dbLemma);
        indexes.add(createIndexEntry(page, dbLemma, lemmasMap.get(dbLemma.getLemma())));
    }

    private Lemma updateLemmaFrequency(Lemma dbLemma) {
        dbLemma.setFrequency(dbLemma.getFrequency() + 1);
        return dbLemma;
    }

    private Lemma createLemmaEntry(Site site, String lemma) {
        return Lemma.builder().lemma(lemma).site(site).frequency(1).build();
    }

    private Index createIndexEntry(Page page, Lemma lemma, int rank) {
        return Index.builder().page(page).lemma(lemma).rank(rank).build();
    }
}
