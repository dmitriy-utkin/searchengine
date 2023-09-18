package searchengine.services.indexing;

import lombok.Getter;
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

public class HtmlParser {
    public HtmlParser(Site site, Page page, LemmaFinder lemmaFinder, LemmaRepository lemmaRepository) {
        this.site = site;
        this.page = page;
        this.lemmaFinder = lemmaFinder;
        this.lemmaRepository = lemmaRepository;
        this.lemmas = new ArrayList<>();
        this.indexes = new ArrayList<>();
        collectLemmasAndIndexes(isCorrectCodeCheck(page.getCode()));
    }

    private final Site site;
    private final Page page;
    private final LemmaFinder lemmaFinder;
    private final LemmaRepository lemmaRepository;
    @Getter
    private List<Lemma> lemmas;
    @Getter
    private List<Index> indexes;

    private void collectLemmasAndIndexes(boolean isCorrectCode) {
        if (!isCorrectCode) {lemmas = null; indexes = null; return;}
        Map<String, Integer> lemmasMap = lemmaFinder.collectLemmas(page.getContent());
        Optional<List<Lemma>> dbLemmas = lemmaRepository.findAllBySite(site);
        final List<ExistedLemma> existedBySiteLemmas = new ArrayList<>();
        dbLemmas.ifPresent(dbLemmaList -> existedBySiteLemmas.addAll(dbLemmaList.stream()
                .map(ExistedLemma::new).toList()));
        lemmasMap.keySet().forEach(lemma -> {
            ExistedLemma existedLemma = existedBySiteLemmas.stream()
                    .filter(l -> l.getDbLemma().getLemma().equals(lemma))
                    .findFirst().orElse(null);
            if (existedLemma != null) {
                updateLemmaAndIndexesLists(updateLemmaFrequency(existedLemma.getDbLemma()), lemmasMap);
            } else {
                updateLemmaAndIndexesLists(createLemmaEntry(site, lemma), lemmasMap);
            }
        });
    }

    private boolean isCorrectCodeCheck(int code) {
        return !String.valueOf(code).startsWith("4") || !String.valueOf(code).startsWith("5");
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
        return Lemma.builder()
                .lemma(lemma)
                .site(site)
                .frequency(1)
                .build();
    }

    private Index createIndexEntry(Page page, Lemma lemma, int rank) {
        return Index.builder()
                .page(page)
                .lemma(lemma)
                .rank(rank)
                .build();
    }
}