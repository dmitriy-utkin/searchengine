import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import searchengine.services.LemmaFinder;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws IOException {

        String text = "Повторное появление леопарда в Осетии позволяет предположить, что леопард постоянно обитает в некоторых районах Северного Кавказа.";

        LemmaFinder lemmaFinder = new LemmaFinder(new RussianLuceneMorphology());

        Map<String, Integer> lemmas = lemmaFinder.collectLemmas(text);
        for (String lemma : lemmas.keySet()) {
            System.out.println(lemma + " - " + lemmas.get(lemma));
        }

//        String[] words = text.toLowerCase().replaceAll("[^a-zа-я ]", "").trim().split("\\s+");
//        List.of(words).forEach(word -> {
//            try {
//                collectLemmas(word, list);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        });
//
//        for (String key : list.keySet()) {
//            System.out.println(key + " - " + list.get(key));
//        }
    }

//    public static Map<String, Integer> collectLemmas(String word, Map<String, Integer> list) throws IOException {
//        Map<String, Integer> lemmasMap = new HashMap<>();
//        LuceneMorphology luceneMorph = new RussianLuceneMorphology();
//        List<String> wordBaseForms = luceneMorph.getNormalForms(word);
//        wordBaseForms.forEach(w -> {
//
//        });
//
//    }
}
