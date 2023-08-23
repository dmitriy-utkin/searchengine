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

    @Override
    public ResponseEntity<ResponseService> startIndexing(SitesList sitesList,
                                                         SiteRepository siteRepository,
                                                         PageRepository pageRepository,
                                                         LemmaRepository lemmaRepository,
                                                         IndexRepository indexRepository,
                                                         LemmaFinder lemmaFinder){
        DBSite preparedSite;
        if (siteRepository.findByStatus(Status.INDEXING).size() > 0) {
            return new ResponseEntity<>(new ResponseServiceImpl.Response.BadRequest(START_INDEXING_ERROR), HttpStatus.BAD_REQUEST);
        }
        indexationIsRunning = true;
        for (Site site : sitesList.getSites()) {
            //TODO: поправить логику с сохранением адреса сайта с "/" в конце (при этом изменить логику сохранения page.path -> сейчас без "/" в начале адреса
            String siteUrl = site.getUrl().endsWith("/") ? site.getUrl() : site.getUrl() + "/";
            Optional<DBSite> dbSite = siteRepository.findByUrl(siteUrl);
            if (dbSite.isEmpty()) {
                preparedSite = createSiteEntry(site);
                siteRepository.save(preparedSite);
            } else if (dbSite.get().getStatus().equals(Status.INDEXED) || dbSite.get().getStatus().equals(Status.FAILED)) {
                preparedSite = dbSite.get();
                preparedSite.setStatus(Status.INDEXING);
                siteRepository.save(preparedSite);
                pageRepository.findByDbSite(preparedSite).forEach(indexRepository::deleteByDbPage);
                lemmaRepository.deleteByDbSite(preparedSite);
                pageRepository.deleteByDbSite(preparedSite);
            }
        }
        Thread thread = new Thread(new IndexerLauncher(siteRepository, pageRepository, lemmaRepository, indexRepository, siteRepository.findAll(), lemmaFinder));
        thread.start();
        return new ResponseEntity<>(new ResponseServiceImpl.Response.IndexingSuccessResponseService(), HttpStatus.OK);
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
            return new ResponseEntity<>(new ResponseServiceImpl.Response.IndexingSuccessResponseService(), HttpStatus.OK);
        }
    }

    @Override
    public ResponseEntity<ResponseService> indexPage(SiteRepository siteRepository,
                                                     PageRepository pageRepository,
                                                     LemmaRepository lemmaRepository,
                                                     IndexRepository indexRepository,
                                                     String url,
                                                     LemmaFinder lemmaFinder) {
        String preparedUrl = url.toLowerCase().trim();
        List<DBSite> siteList = siteRepository.findAll();
        boolean pageIsLinkedToExistedSites = false;
        DBPage dbPage = null;
        DBSite dbSite = null;
        for (DBSite site : siteList) {
            if (preparedUrl.contains(site.getUrl())) {
                pageIsLinkedToExistedSites = true;
                //TODO: error with a duplicate value for pages (when i add the same page in the second time)
                dbSite = site;
                Optional<DBPage> page = pageRepository.findByPathAndDbSite(preparedUrl.replace(site.getUrl(), ""), site);
                if (page.isPresent()) {
                    log.info("Started for " + page.get().getPath());
                    //TODO: error when i delete the indexes due to the Lemma_id, check it it is error in the table architecture! Many-to-one? ore something another like more-to-more...
                    indexRepository.deleteByDbPage(page.get());
                    log.info("Found page: " + page.get().getPath() + "; " + page.get().getId());
                    //TODO: if i add https://1c.ru (without "/" at the end of the site url) it will be error due to the sites in db with "/" at the end
                    //TODO: ошибка с удалением (foreign key ...indexe... ) связана с тем, что удаляются леммы по сайту. Пробую убрать удаление из леммы, так как иначе нелдьзя удалить лемму по сайту, если она уже была проиндексирована
//                    lemmaRepository.deleteByDbSite(dbSite);
                    pageRepository.deleteById(page.get().getId());
                }
                dbPage = createPageEntry(site, preparedUrl);
                break;
            }
        }
        //TODO: добавить проверку на наличие сайта в конфигурационном файле
        if (!pageIsLinkedToExistedSites) {
            return new ResponseEntity<>(new ResponseServiceImpl.Response.BadRequest(INDEX_PAGE_ERROR), HttpStatus.BAD_REQUEST);
        } else {
            //TODO: do not changed INDEXES and LEMMAS tables if it is the second and more relaunch of the page from the table
            pageRepository.save(dbPage);
            createLemmaAndIndex(dbSite, dbPage, lemmaRepository, indexRepository, lemmaFinder);
        }
        return new ResponseEntity<>(new ResponseServiceImpl.Response.IndexingSuccessResponseService(), HttpStatus.OK);
    }

    @AllArgsConstructor
    static class IndexerLauncher implements Runnable{

        private SiteRepository siteRepository;
        private PageRepository pageRepository;
        private LemmaRepository lemmaRepository;
        private IndexRepository indexRepository;
        private List<DBSite> sites;
        private LemmaFinder lemmaFinder;

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
                        links.forEach(link -> {
                            DBPage page = createPageEntry(site, link);
                            pageRepository.save(page);
                            IndexingServiceImpl.createLemmaAndIndex(site, page, lemmaRepository, indexRepository, lemmaFinder);
                        });
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
    private static void createLemmaAndIndex(DBSite site,
                                            DBPage page,
                                            LemmaRepository lemmaRepository,
                                            IndexRepository indexRepository,
                                            LemmaFinder lemmaFinder) {
        //TODO: проверка далее не убирает доп операцию по проверке в indexPage, то есть сначала все происходит/удаляется и тд там, и только потом доходит до этой проверке на "код")
        if (!(String.valueOf(page.getCode()).startsWith("4") || String.valueOf(page.getCode()).startsWith("5"))) {
            Map<String, Integer> lemmas = lemmaFinder.collectLemmas(page.getContent());
            log.info("Size of lemmas list: " + lemmas.size());
            for (String lemma : lemmas.keySet()) {
                Optional<DBLemma> existedLemma = lemmaRepository.findByDbSiteAndLemma(site, lemma);
                DBLemma dbLemma;
                if (existedLemma.isPresent()) {
                    dbLemma = existedLemma.get();
                    dbLemma.setFrequency(existedLemma.get().getFrequency() + 1);
                } else {
                    dbLemma = createLemmaEntry(site, lemma);
                }
                DBIndex dbIndex = createIndexEntry(page, dbLemma, lemmas.get(lemma));
                //TODO: переделать функцию добавления лемм, сейчас привязано под один метод (нет дополнительной проверки на входе, которая бы позволила увеличивать frequency по сайту???)
                lemmaRepository.save(dbLemma);
                indexRepository.save(dbIndex);
            }
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

    private static DBLemma createLemmaEntry(DBSite site, String lemma) {
        return DBLemma.builder()
                .lemma(lemma)
                .dbSite(site)
                .frequency(1)
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
        return DBSite.builder()
                .status(Status.INDEXING)
                .url(site.getUrl().endsWith("/") ? site.getUrl() : (site.getUrl() + "/"))
                .name(site.getName())
                .statusTime(new Date()).build();
    }

    private static void clearAllLists(CopyOnWriteArraySet list) {
        list.clear();
        SiteParser.incorrectLink.clear();
        SiteParser.preparedLinks.clear();
    }

}
