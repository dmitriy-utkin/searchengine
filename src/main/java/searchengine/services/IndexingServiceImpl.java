package searchengine.services;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
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
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ForkJoinPool;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaFinder lemmaFinder;
    private final SitesList sitesList;

    private final String START_INDEXING_ERROR = "Индексация уже запущена";
    private final String STOP_INDEXING_ERROR = "Индексация не запущена";
    private final String INDEX_PAGE_ERROR = "Данная страница находится за пределами сайтов, указанных в конфигурационном файле";

    static boolean indexationIsRunning = false;

    @Override
    public ResponseEntity<ResponseService> startIndexing(){
        if (siteRepository.existByStatus(Status.INDEXING)) {
            return new ResponseEntity<>(new ResponseServiceImpl.BadRequest(START_INDEXING_ERROR), HttpStatus.BAD_REQUEST);
        }
        indexationIsRunning = true;
        clearDataBase();
        for (Site site : sitesList.getSites()) {
            siteRepository.save(createSiteEntry(site));
        }
        siteRepository.findAll().forEach(dbSite -> {
            new Thread(() -> new ForkJoinPool().invoke(
                    //TODO: add a site ID!!!!!
                    new SiteParser(siteRepository, dbSite.getId(), )
            ));
        });

        Thread thread = new Thread(new IndexerLauncher(siteRepository, pageRepository, siteRepository.findAll()));
        thread.start();
        return new ResponseEntity<>(new ResponseServiceImpl.IndexingSuccessResponseService(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ResponseService> stopIndexing() {
        if (!siteRepository.existByStatus(Status.INDEXING)) return new ResponseEntity<>(new ResponseServiceImpl.BadRequest(STOP_INDEXING_ERROR), HttpStatus.BAD_REQUEST);
        else {
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
            return new ResponseEntity<>(new ResponseServiceImpl.IndexingSuccessResponseService(), HttpStatus.OK);
        }
    }

    @Override
    public ResponseEntity<ResponseService> indexPage(String url) {
        String preparedUrl = url.toLowerCase().trim();
        List<DBSite> siteList = siteRepository.findAll();
        boolean pageIsLinkedToExistedSites = false;
        DBPage dbPage = null;
        DBSite dbSite = null;
        //TODO: сделать проверку по siteList из конфигурационного сайта
        for (DBSite site : siteList) {
            if (preparedUrl.contains(site.getUrl())) {
                pageIsLinkedToExistedSites = true;
                //TODO: error with a duplicate value for pages (when i add the same page in the second time)
                dbSite = site;
                Optional<DBPage> page = pageRepository.findByPathAndDbSite(preparedUrl.replace(site.getUrl(), ""), site);
                if (page.isPresent()) {
                    log.info("Started for " + page.get().getPath());
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
            return new ResponseEntity<>(new ResponseServiceImpl.BadRequest(INDEX_PAGE_ERROR), HttpStatus.BAD_REQUEST);
        } else {
            //TODO: do not changed INDEXES and LEMMAS tables if it is the second and more relaunch of the page from the table
            pageRepository.save(dbPage);
            createLemmaAndIndex(dbSite, dbPage);
        }
        return new ResponseEntity<>(new ResponseServiceImpl.IndexingSuccessResponseService(), HttpStatus.OK);
    }

    @AllArgsConstructor
    public class IndexerLauncher implements Runnable{

        private SiteRepository siteRepository;
        private PageRepository pageRepository;
        private List<DBSite> sites;

        @Override
        public void run() {
            sites.forEach(site -> {
                if (indexationIsRunning) {
                    ForkJoinPool pool = new ForkJoinPool();
                    CopyOnWriteArraySet<String> links = new CopyOnWriteArraySet<>();
                    clearAllLists(links);
                    links = pool.invoke(new SiteParser(siteRepository, site.getUrl(), site.getUrl(), site));
                    if (indexationIsRunning) {
                        links.forEach(link -> {
                            DBPage page = createPageEntry(site, link);
                            pageRepository.save(page);
                            createLemmaAndIndex(site, page);
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
    private void createLemmaAndIndex(DBSite site, DBPage page) {
        List<DBLemma> dbLemmas = new ArrayList<>();
        List<DBIndex> dbIndexes = new ArrayList<>();
        //TODO: не выполняется остановка добавления в базу данных в случае стопИндексинг (если вставить проверку в лоб - будет ошибка
        //TODO: проверка далее не убирает доп операцию по проверке в indexPage, то есть сначала все происходит/удаляется и тд там, и только потом доходит до этой проверке на "код")
        if (!(String.valueOf(page.getCode()).startsWith("4") || String.valueOf(page.getCode()).startsWith("5"))) {
            Map<String, Integer> lemmas = lemmaFinder.collectLemmas(page.getContent());
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
                dbLemmas.add(dbLemma);
                dbIndexes.add(dbIndex);
                //TODO: переделать функцию добавления лемм, сейчас привязано под один метод (нет дополнительной проверки на входе, которая бы позволила увеличивать frequency по сайту???)
                lemmaRepository.saveAll(dbLemmas);
                indexRepository.saveAll(dbIndexes);
            }
        }
    }

    private static DBPage createPageEntry(DBSite site, String url) {
        int statusCode = 0;
        try {
            String rootUrl = site.getUrl();
            Connection.Response response = Jsoup.connect(url)
                    .userAgent("Chrome/4.0.249.0 Safari/532.5")
                    .referrer("http://www.google.com")
                    .timeout(10_000)
                    .ignoreHttpErrors(true)
                    .followRedirects(false)
                    .execute();
            statusCode = response.statusCode();
            Document doc = response.parse();
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
                    .content("")
                    .dbSite(site)
                    .code(statusCode)
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
                .url(prepareSiteUrl(site.getUrl()))
                .name(site.getName())
                .statusTime(new Date()).build();
    }

    private static void clearAllLists(CopyOnWriteArraySet list) {
        list.clear();
        SiteParser.incorrectLinks.clear();
        SiteParser.preparedLinks.clear();
    }

    private String prepareSiteUrl(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private void clearDataBase() {
        indexRepository.deleteAllInBatch();
        lemmaRepository.deleteAllInBatch();
        pageRepository.deleteAllInBatch();
        siteRepository.deleteAllInBatch();
        log.info("All info was removed.");
    }

}
