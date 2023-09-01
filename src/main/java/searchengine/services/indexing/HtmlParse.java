package searchengine.services.indexing;

import lombok.Getter;
import searchengine.model.DBIndex;
import searchengine.model.DBLemma;
import searchengine.model.DBPage;
import searchengine.model.DBSite;
import searchengine.repository.LemmaRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HtmlParse {

    private final DBSite site;
    private final DBPage page;
    private final LemmaFinder lemmaFinder;
    private final LemmaRepository lemmaRepository;
    @Getter
    private List<DBLemma> lemmas;
    @Getter
    private List<DBIndex> indexes;

    public HtmlParse(DBSite site, DBPage page, LemmaFinder lemmaFinder, LemmaRepository lemmaRepository) {
        this.site = site;
        this.page = page;
        this.lemmaFinder = lemmaFinder;
        this.lemmaRepository = lemmaRepository;
        this.lemmas = new ArrayList<>();
        this.indexes = new ArrayList<>();
        collectLemmasAndIndexes(String.valueOf(page.getCode()).startsWith("4") || String.valueOf(page.getCode()).startsWith("5"));
    }

    private void collectLemmasAndIndexes(boolean isIncorrectCode) {
        if (isIncorrectCode) {lemmas = null; indexes = null; return;}
        Map<String, Integer> lemmasMap = lemmaFinder.collectLemmas(page.getContent());
        List<String> dbLemmas = lemmaRepository.findByDbSite(site).stream().map(DBLemma::getLemma).toList();
        lemmasMap.keySet().forEach(lemma -> {
            DBLemma dbLemma = dbLemmas.contains(lemma) ? updateLemmaFrequency(lemma) : createLemmaEntry(site, lemma);
            lemmas.add(dbLemma);
            indexes.add(createIndexEntry(page, dbLemma, lemmasMap.get(lemma)));
        });
    }

    private DBLemma updateLemmaFrequency(String lemma) {
        DBLemma dbLemma = lemmaRepository.findByDbSiteAndLemma(site, lemma).get();
        dbLemma.setFrequency(dbLemma.getFrequency() + 1);
        return dbLemma;
    }

    private DBLemma createLemmaEntry(DBSite site, String lemma) {
        return DBLemma.builder()
                .lemma(lemma)
                .dbSite(site)
                .frequency(1)
                .build();
    }

    private DBIndex createIndexEntry(DBPage page, DBLemma lemma, float rank) {
        return DBIndex.builder()
                .dbPage(page)
                .dbLemma(lemma)
                .rank(rank)
                .build();
    }
}