package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Jsoup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class LemmaFinder {
    private final LuceneMorphology luceneMorphology;
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};

    public LemmaFinder(LuceneMorphology luceneMorphology) {
        this.luceneMorphology = luceneMorphology;
    }

    public Map<String, Integer> collectLemmas(String content) {

        String text = convertHtmlToText(content);

        Map<String, Integer> lemmas = new HashMap<>();
        String[] words = getWordsArray(text);

        for (String word : words) {

            if (word.isBlank()) {
                continue;
            }

            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (anyFormsBelongToParticle(wordBaseForms)) {
                continue;
            }

            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }

            String normalWords = normalForms.get(0);
            if (lemmas.containsKey(normalWords)) {
                lemmas.put(normalWords, lemmas.get(normalWords) + 1);
            } else {
                lemmas.put(normalWords, 1);
            }
        }


        return lemmas;
    }

    private String convertHtmlToText(String content) {
        return Jsoup.parse(content).text();
    }

    private boolean anyFormsBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }

    private boolean hasParticleProperty(String wordBase) {
        for (String property : particlesNames) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }

    private String[] getWordsArray(String text) {
        return text.toLowerCase().trim()
                .replaceAll("([^а-я\\s])", " ")
                .split("\\s+");
    }

}
