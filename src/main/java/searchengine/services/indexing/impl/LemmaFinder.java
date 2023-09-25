package searchengine.services.indexing.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Slf4j
@Component
public class LemmaFinder {
    private final LuceneMorphology luceneMorphology;
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};

    public LemmaFinder() throws IOException {
        this.luceneMorphology = new RussianLuceneMorphology();
    }

    public Map<String, Integer> collectLemmas(String content) {

        String text = convertHtmlToText(content);

        Map<String, Integer> lemmas = new HashMap<>();
        String[] words = getWordsArray(text);

        for (String word : words) {

            if (word.isBlank()) continue;

            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (anyFormsBelongToParticle(wordBaseForms)) continue;

            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) continue;

            String normalWords = normalForms.get(0);
            if (lemmas.containsKey(normalWords)) {
                lemmas.put(normalWords, lemmas.get(normalWords) + 1);
            } else {
                lemmas.put(normalWords, 1);
            }
        }

        return lemmas;
    }

    public Map<String, String> collectNormalInitialForms(String content, Set<String> query) {
        String text = convertHtmlToText(content);

        Map<String, String> result = new HashMap<>();
        String[] words = getWordsArray(text);

        for (String word : words) {

            if (query.size() == result.size()) return result;

            if (word.isBlank()) continue;

            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (anyFormsBelongToParticle(wordBaseForms)) continue;

            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) continue;

            if (query.stream().anyMatch(normalForms.get(0)::equals)) result.put(normalForms.get(0), word);
        }

        return result;
    }

    public String convertHtmlToText(String content) {
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
