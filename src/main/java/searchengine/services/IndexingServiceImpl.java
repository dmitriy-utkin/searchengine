package searchengine.services;

import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ForkJoinPool;

@Slf4j
@Service
public class IndexingServiceImpl implements IndexingService {

    static boolean indexationIsRunning = false;

    @Override
    public ResponseEntity<ResponseService> startIndexing(SitesList sitesList, SiteRepository siteRepository, PageRepository pageRepository){
        DBSite preparedSite;
        if (siteRepository.findByStatus(Status.INDEXING).size() > 0) {
            return new ResponseEntity<>(new ResponseServiceImpl.Response.BadRequest("Индексация уже запущена"), HttpStatus.BAD_REQUEST);
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
            return new ResponseEntity<>(new ResponseServiceImpl.Response.BadRequest("Индексация не запущена"), HttpStatus.BAD_REQUEST);
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

    private DBSite createSiteEntry(Site site) {

        return DBSite.builder().status(Status.INDEXING).url(site.getUrl().endsWith("/") ? site.getUrl() : (site.getUrl() + "/")).name(site.getName()).statusTime(new Date()).build();
    }

    private static void clearAllLists(CopyOnWriteArraySet list) {
        list.clear();
        SiteParser.incorrectLink.clear();
        SiteParser.preparedLinks.clear();
    }

}
