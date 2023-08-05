package searchengine.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ForkJoinPool;

@Slf4j
@Service
public class IndexingServiceImpl implements IndexingService {

    private final String START_INDEXING_ERROR = "Индексация уже запущена";
    private final String STOP_INDEXING_ERROR = "Индексация не запущена";
    private final String INDEX_PAGE_ERROR = "Данная страница находится за пределами сайтов, указанных в конфигурационном файле";

    static boolean indexationIsRunning = false;
    private LemmaFinder lemmaFinder = new LemmaFinder(new RussianLuceneMorphology());

    public IndexingServiceImpl() throws IOException {
    }

    @Override
    public ResponseEntity<ResponseService> startIndexing(SitesList sitesList, SiteRepository siteRepository, PageRepository pageRepository){
        DBSite preparedSite;
        if (siteRepository.findByStatus(Status.INDEXING).size() > 0) {
            return new ResponseEntity<>(new ResponseServiceImpl.Response.BadRequest(START_INDEXING_ERROR), HttpStatus.BAD_REQUEST);
        }
        indexationIsRunning = true;
        for (Site site : sitesList.getSites()) {
            Optional<DBSite> dbSite = siteRepository.findByUrl(site.getUrl());
            if (dbSite.isEmpty()) {
                preparedSite = createSiteEntry(site);
                siteRepository.save(preparedSite);
            } else if (dbSite.get().getStatus().equals(Status.INDEXED) || dbSite.get().getStatus().equals(Status.FAILED)) {
                preparedSite = dbSite.get();
                preparedSite.setStatus(Status.INDEXING);
                siteRepository.save(preparedSite);
                pageRepository.deleteByDbSite(preparedSite);
            }
        }
        Thread thread = new Thread(new IndexerLauncher(siteRepository, pageRepository, siteRepository.findAll()));
        thread.start();
        return new ResponseEntity<>(new ResponseServiceImpl.Response.SuccessResponseService(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ResponseService> stopIndexing(SiteRepository siteRepository) {
        boolean indexingInProcess = siteRepository.findByStatus(Status.INDEXING).size() > 0;
        if (!indexingInProcess) {
            return new ResponseEntity<>(new ResponseServiceImpl.Response.BadRequest(STOP_INDEXING_ERROR), HttpStatus.BAD_REQUEST);
        } else {
            try {
                indexationIsRunning = false;
                List<DBSite> sites = siteRepository.findByStatus(Status.INDEXING);
                sites.forEach(site -> {
                    site.setStatus(Status.FAILED);
                    site.setLastError("Stopped by user");
                    site.setStatusTime(new Date());
                    siteRepository.save(site);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new ResponseEntity<>(new ResponseServiceImpl.Response.SuccessResponseService(), HttpStatus.OK);
        }
    }

    @Override
    public ResponseEntity<ResponseService> indexPage(SiteRepository siteRepository,
                                                     PageRepository pageRepository,
                                                     LemmaRepository lemmaRepository,
                                                     IndexRepository indexRepository,
                                                     String url) {

        List<DBSite> siteList = siteRepository.findAll();
        boolean pageIsLinkedToExistedSites = false;
        DBPage dbPage = null;
        DBSite dbSite = null;
        for (DBSite site : siteList) {
            if (url.contains(site.getUrl())) {
                pageIsLinkedToExistedSites = true;
                dbPage = createPageEntry(site, url);
                dbSite = site;
                pageRepository.save(dbPage);
                break;
            }
        }
        //TODO: добавить проверку на наличие сайта в конфигурационном файле
        if (!pageIsLinkedToExistedSites) {
            return new ResponseEntity<>(new ResponseServiceImpl.Response.BadRequest(INDEX_PAGE_ERROR), HttpStatus.BAD_REQUEST);
        } else {
            createLemmaAndIndex(dbSite, dbPage, dbPage.getContent(), lemmaRepository, indexRepository);
        }

        return new ResponseEntity<>(new ResponseServiceImpl.Response.SuccessResponseService(), HttpStatus.OK);
    }

    @AllArgsConstructor
    static class IndexerLauncher implements Runnable{

        private SiteRepository siteRepository;
        private PageRepository pageRepository;
        private List<DBSite> sites;

        @Override
        public void run() {
            sites.forEach(site -> {
                if (indexationIsRunning) {
                    log.info("RUN for " + site.getUrl() + " in thread " + Thread.currentThread().getName());
                    ForkJoinPool pool = new ForkJoinPool();
                    CopyOnWriteArraySet<String> links = new CopyOnWriteArraySet<>();
                    clearAllLists(links);
                    links = pool.invoke(new SiteParser(siteRepository, site.getUrl(), site.getUrl(), site));
                    if (indexationIsRunning) {
                        links.forEach(link -> pageRepository.save(createPageEntry(site, link)));
                        site.setStatus(Status.INDEXED);
                        clearAllLists(links);
                    } else {
                        site.setStatus(Status.FAILED);
                        site.setLastError("Stopped by user");
                        clearAllLists(links);
                    }
                    siteRepository.save(site);
                }
            });

            indexationIsRunning = false;
        }
    }

    //TODO: добавляются одинаковые леммы, выглядит неправильно (нужно поменять, чтобы увеличивалось кол-во лемм?)
    private void createLemmaAndIndex(DBSite site, DBPage page, String content, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        Map<String, Integer> lemmas = lemmaFinder.collectLemmas(content);
        for (String lemma : lemmas.keySet()) {
            DBLemma dbLemma = createLemmaEntry(site, lemma, lemmas.get(lemma));
            DBIndex dbIndex = createIndexEntry(page, dbLemma, 1);
            lemmaRepository.save(dbLemma);
            indexRepository.save(dbIndex);
        }
    }

    private static DBPage createPageEntry(DBSite site, String url) {
        try {
            String rootUrl = site.getUrl();
            Connection.Response response = Jsoup.connect(url)
                    .userAgent("Chrome/4.0.249.0 Safari/532.5")
                    .referrer("http://www.google.com")
                    .timeout(10_000)
                    .ignoreHttpErrors(true)
                    .followRedirects(false)
                    .execute();
            Document doc = response.parse();
            int statusCode = response.statusCode();
            String content = doc.outerHtml();

            return DBPage.builder()
                    .path(url.replace(rootUrl, ""))
                    .content(content)
                    .dbSite(site)
                    .code(statusCode)
                    .build();
        } catch (Exception e) {
            log.error(e.getMessage());
            return DBPage.builder()
                    .path(url.replace(site.getUrl(), ""))
                    .content("BAD REQUEST")
                    .dbSite(site)
                    .code(404)
                    .build();
        }
    }

    private static DBLemma createLemmaEntry(DBSite site, String lemma, int frequency) {
        return DBLemma.builder()
                .lemma(lemma)
                .frequency(frequency)
                .dbSite(site)
                .build();
    }

    private static DBIndex createIndexEntry(DBPage page, DBLemma lemma, float rank) {
        return DBIndex.builder()
                .dbPage(page)
                .dbLemma(lemma)
                .rank(rank)
                .build();
    }

    private DBSite createSiteEntry(Site site) {

        return DBSite.builder().status(Status.INDEXING).url(site.getUrl().endsWith("/") ? site.getUrl() : (site.getUrl() + "/")).name(site.getName()).statusTime(new Date()).build();
    }

    private static void clearAllLists(CopyOnWriteArraySet list) {
        list.clear();
        SiteParser.incorrectLink.clear();
        SiteParser.preparedLinks.clear();
    }

}
