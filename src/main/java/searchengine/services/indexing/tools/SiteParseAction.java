package searchengine.services.indexing.tools;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import searchengine.config.JsoupConfig;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.indexing.IndexingServiceImpl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;

@Slf4j
@AllArgsConstructor
public class SiteParseAction extends RecursiveAction {
    private JsoupConfig config;
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private LemmaFinder lemmaFinder;
    private IndexRepository indexRepository;
    private Site site;
    private String url;
    private ConcurrentHashMap<String, Boolean> processedLink;

    @Override
    protected void compute() {
        if (!IndexingServiceImpl.indexationIsRunning) Thread.currentThread().interrupt();
        try {
            Thread.sleep(config.getSleep());
            PageInfoCreator creator = new PageInfoCreator(site, url, config, lemmaFinder, lemmaRepository);
            updateDataBase(url, site, creator.getPage(), creator.getLemmas(), creator.getIndexes());
            creator.getDoc().select("body").select("a").forEach(link -> {
                String uri = link.absUrl("href");
                if (isCorrectLink(uri.toLowerCase(Locale.ROOT), site.getUrl())) {
                    SiteParseAction action = new SiteParseAction(config,siteRepository, pageRepository,
                            lemmaRepository, lemmaFinder,indexRepository, site,uri, processedLink);
                    action.fork();
                    action.join();
                } else {
                    processedLink.put(uri, false);
                }
            });
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private synchronized void updateDataBase(String url, Site site, Page page,
                                             List<Lemma> lemmas, List<Index> indexes) {
        if (!IndexingServiceImpl.indexationIsRunning) return;
        processedLink.put(url, true);
        site.setStatusTime(new Date());
        siteRepository.saveAndFlush(site);
        if (page != null && lemmas != null && indexes != null) {
            pageRepository.saveAndFlush(page);
            lemmaRepository.saveAllAndFlush(lemmas);
            indexRepository.saveAllAndFlush(indexes);
        }
    }

    private boolean isCorrectLink(String url, String rootUrl) {
        return !processedLink.containsKey(url) &&
                url.startsWith(rootUrl) &&
                !url.endsWith(".pdf") &&
                !url.endsWith(".png") &&
                !url.endsWith(".jpg") &&
                !url.endsWith(".jpeg") &&
                !url.endsWith(".eps") &&
                !url.endsWith(".xlsx") &&
                !url.endsWith(".xls") &&
                !url.endsWith(".doc") &&
                !url.endsWith(".docx") &&
                !url.endsWith(".ppt") &&
                !url.endsWith(".jsp") &&
                !url.endsWith(".zip") &&
                !url.endsWith(".rar") &&
                !url.contains("=") &&
                !url.contains("#") &&
                !url.contains("?");
    }
}
