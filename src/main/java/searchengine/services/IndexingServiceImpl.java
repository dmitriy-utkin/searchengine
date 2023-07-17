package searchengine.services;

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

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ForkJoinPool;

@Slf4j
@Service
public class IndexingServiceImpl implements IndexingService {

    @Override
    public ResponseEntity<ResponseService> startIndexing(SitesList sitesList,
                                                         SiteRepository siteRepository,
                                                         PageRepository pageRepository){
        int countOfIndexedSite = sitesList.getSites().size() - 1;
        DBSite preparedSite;
        for (Site site : sitesList.getSites()) {
            Optional<DBSite> dbSite = siteRepository.findByUrl(site.getUrl());
            if (dbSite.isEmpty()) {
                countOfIndexedSite--;
                siteRepository.save(createSiteEntry(site));
            } else if (dbSite.get().getStatus().equals(Status.INDEXED)) {
                preparedSite = dbSite.get();
                preparedSite.setStatus(Status.INDEXING);
                siteRepository.save(preparedSite);
                pageRepository.deleteByDbSite(preparedSite);
                countOfIndexedSite--;
            }
        }

        //TODO: если уже что-то было проиндексировано, все равно запускается переиндексация даже на запущенный сайт. Переделать выбор сайтов для запуска индексации

        Thread thread = new Thread(new IndexerThread(siteRepository, pageRepository, siteRepository.findAll()));
        thread.start();

        boolean response = countOfIndexedSite < sitesList.getSites().size() - 1;

        return response ? new ResponseEntity<>(new ResponseServiceImpl.Response.SuccessResponseService(), HttpStatus.OK)
                : new ResponseEntity<>(new ResponseServiceImpl.Response.BadRequest("Индексация уже запущена"), HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<ResponseService> stopIndexing(SiteRepository siteRepository) {
        boolean indexingInProcess = siteRepository.findByStatus(Status.INDEXING).size() > 0;
        if (!indexingInProcess) {
            return new ResponseEntity<>(new ResponseServiceImpl.Response.BadRequest("Индексация не запущена"), HttpStatus.BAD_REQUEST);
        } else {

            return new ResponseEntity<>(new ResponseServiceImpl.Response.SuccessResponseService(), HttpStatus.OK);
        }
    }

    static class IndexerThread implements Runnable{

        private SiteRepository siteRepository;
        private PageRepository pageRepository;
        private List<DBSite> sites;

        public IndexerThread(SiteRepository siteRepository, PageRepository pageRepository, List<DBSite> sites) {
            this.siteRepository = siteRepository;
            this.pageRepository = pageRepository;
            this.sites = sites;
        }

        @Override
        public void run() {
            for (DBSite site : sites) {
                log.info("RUN for " + site.getUrl());
                indexTask(siteRepository, pageRepository, site);
            }
        }
    }

    private static void indexTask(SiteRepository siteRepository, PageRepository pageRepository,DBSite site) {
        SiteParser task = new SiteParser(siteRepository, site.getUrl(), site.getUrl(), site);
        ForkJoinPool pool = new ForkJoinPool();
        CopyOnWriteArraySet<String> links = pool.invoke(task);
        links.forEach(link -> {
            try {
                pageRepository.save(createPageEntry(site, link));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        site.setStatus(Status.INDEXED);
        siteRepository.save(site);
        links.clear();
    }

    private static DBPage createPageEntry(DBSite site, String url) throws IOException {
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
    }

    private DBSite createSiteEntry(Site site) {
        return DBSite.builder().status(Status.INDEXING).url(site.getUrl()).name(site.getName()).statusTime(new Date()).build();
    }

}
