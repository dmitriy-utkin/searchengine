package searchengine.services.indexing;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.config.JsoupConfig;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
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
    private int siteId;
    private String url;
    private ConcurrentHashMap<String, Boolean> processedLink;

    @Override
    protected void compute() {
        if (!IndexingServiceImpl.indexationIsRunning) Thread.currentThread().interrupt();
        try {
            Thread.sleep(config.getSleep());
            Connection.Response response = Jsoup.connect(url).userAgent(config.getUserAgent()).referrer(config.getReferrer()).timeout(config.getTimeout()).ignoreHttpErrors(true).followRedirects(config.isRedirect()).execute();
            Document doc = response.parse();
            DBSite site = siteRepository.findById(siteId).get();
            DBPage page = DBPage.builder().path(url.replace(site.getUrl(), "")).dbSite(site).code(response.statusCode()).content(doc.outerHtml()).build();
            HtmlParser htmlParse = new HtmlParser(site, page, lemmaFinder, lemmaRepository);
            updateDataBase(url, site, page, htmlParse.getLemmas(), htmlParse.getIndexes());
            doc.select("body").select("a").forEach(link -> {
                String uri = link.absUrl("href");
                if (isCorrectLink(uri, site.getUrl())) {
                    SiteParseAction action = new SiteParseAction(config, siteRepository, pageRepository, lemmaRepository, lemmaFinder, indexRepository, siteId, uri, processedLink);
                    action.fork();
                    action.join();
                } else {
                    processedLink.put(uri, false);
                }
            });
            //TODO: как понять, что индексация завершена...
            //TODO: переделать "отлов" ошибок...
        } catch (InterruptedException | IOException e) {
            log.error(e.getMessage());
        }
    }

    private synchronized void updateDataBase(String url, DBSite site, DBPage page, List<DBLemma> lemmas, List<DBIndex> indexes) {
        if (!IndexingServiceImpl.indexationIsRunning) return;
        processedLink.put(url, true);
        site.setStatusTime(new Date());
        siteRepository.saveAndFlush(site);
        pageRepository.saveAndFlush(page);
        if (lemmas != null) lemmaRepository.saveAllAndFlush(lemmas);
        if (indexes != null) indexRepository.saveAllAndFlush(indexes);
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