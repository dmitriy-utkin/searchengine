package searchengine.services.indexing;

import lombok.Getter;
import searchengine.dto.indexing.ExistedDbLemma;
import searchengine.model.DBIndex;
import searchengine.model.DBLemma;
import searchengine.model.DBPage;
import searchengine.model.DBSite;
import searchengine.repository.LemmaRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HtmlParser {

    public HtmlParser(DBSite site, DBPage page, LemmaFinder lemmaFinder, LemmaRepository lemmaRepository) {
        this.site = site;
        this.page = page;
        this.lemmaFinder = lemmaFinder;
        this.lemmaRepository = lemmaRepository;
        this.lemmas = new ArrayList<>();
        this.indexes = new ArrayList<>();
        collectLemmasAndIndexes(String.valueOf(page.getCode()).startsWith("4") || String.valueOf(page.getCode()).startsWith("5"));
    }

    private final DBSite site;
    private final DBPage page;
    private final LemmaFinder lemmaFinder;
    private final LemmaRepository lemmaRepository;
    @Getter
    private List<DBLemma> lemmas;
    @Getter
    private List<DBIndex> indexes;

    private void collectLemmasAndIndexes(boolean isIncorrectPageCode) {
        if (isIncorrectPageCode) {lemmas = null; indexes = null; return;}
        Map<String, Integer> lemmasMap = lemmaFinder.collectLemmas(page.getContent());
        Optional<List<DBLemma>> dbLemmas = lemmaRepository.findAllByDbSite(site);
        final List<ExistedDbLemma> existedBySiteLemmas = new ArrayList<>();
        dbLemmas.ifPresent(dbLemmaList -> existedBySiteLemmas.addAll(dbLemmaList.stream().map(ExistedDbLemma::new).toList()));
        lemmasMap.keySet().forEach(lemma -> {
            ExistedDbLemma existedLemma = existedBySiteLemmas.stream().filter(l -> l.getDbLemma().getLemma().equals(lemma)).findFirst().orElse(null);
            if (existedLemma != null) {
                updateLemmaAndIndexesLists(updateLemmaFrequency(existedLemma.getDbLemma()), lemmasMap);
            } else {
                updateLemmaAndIndexesLists(createLemmaEntry(site, lemma), lemmasMap);
            }
        });
    }

    private void updateLemmaAndIndexesLists(DBLemma dbLemma, Map<String, Integer> lemmasMap) {
        lemmas.add(dbLemma);
        indexes.add(createIndexEntry(page, dbLemma, lemmasMap.get(dbLemma.getLemma())));
    }

    private DBLemma updateLemmaFrequency(DBLemma dbLemma) {
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

    private DBIndex createIndexEntry(DBPage page, DBLemma lemma, int rank) {
        return DBIndex.builder()
                .dbPage(page)
                .dbLemma(lemma)
                .rank(rank)
                .build();
    }
}