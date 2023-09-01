package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.JsoupConfig;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.response.ResponseService;
import searchengine.services.response.ResponseServiceImpl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private final JsoupConfig jsoupConfig;

    private final String START_INDEXING_ERROR = "Индексация уже запущена";
    private final String STOP_INDEXING_ERROR = "Индексация не запущена";
    private final String INDEX_PAGE_ERROR = "Данная страница находится за пределами сайтов, указанных в конфигурационном файле";

    public static boolean indexationIsRunning = false;

    @Override
    public ResponseEntity<ResponseService> startIndexing(){
        if (siteRepository.existsByStatus(Status.INDEXING)) {
            return new ResponseEntity<>(new ResponseServiceImpl.BadRequest(START_INDEXING_ERROR), HttpStatus.BAD_REQUEST);
        }
        indexationIsRunning = true;
        clearDataBase(true, true, true, true);
        sitesList.getSites().forEach(site -> siteRepository.save(createSiteEntry(site)));
        siteRepository.findAll().forEach(dbSite -> {
            new Thread(() -> new ForkJoinPool().invoke(
                    new SiteParseAction(jsoupConfig,
                            siteRepository,
                            pageRepository,
                            lemmaRepository,
                            lemmaFinder,
                            indexRepository,
                            dbSite.getId(),
                            dbSite.getUrl(),
                            new ConcurrentHashMap<>())
            )).start();
        });
        return new ResponseEntity<>(new ResponseServiceImpl.IndexingSuccessResponseService(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ResponseService> stopIndexing() {
        if (!siteRepository.existsByStatus(Status.INDEXING)) return new ResponseEntity<>(new ResponseServiceImpl.BadRequest(STOP_INDEXING_ERROR), HttpStatus.BAD_REQUEST);
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
//                dbPage = createPageEntry(site, preparedUrl);
                break;
            }
        }
        //TODO: добавить проверку на наличие сайта в конфигурационном файле
        if (!pageIsLinkedToExistedSites) {
            return new ResponseEntity<>(new ResponseServiceImpl.BadRequest(INDEX_PAGE_ERROR), HttpStatus.BAD_REQUEST);
        } else {
            //TODO: do not changed INDEXES and LEMMAS tables if it is the second and more relaunch of the page from the table
            pageRepository.save(dbPage);
            //TODO: переделывать индексацию отдельной страницы
//            createLemmaAndIndex(dbSite, dbPage);
        }
        return new ResponseEntity<>(new ResponseServiceImpl.IndexingSuccessResponseService(), HttpStatus.OK);
    }

    //TODO: добавляются одинаковые леммы, выглядит неправильно (нужно поменять, чтобы увеличивалось кол-во лемм?)


    private DBSite createSiteEntry(Site site) {
        return DBSite.builder()
                .status(Status.INDEXING)
                .url(prepareSiteUrl(site.getUrl()))
                .name(site.getName())
                .statusTime(new Date()).build();
    }

    private String prepareSiteUrl(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private void clearDataBase(boolean indexes, boolean lemmas, boolean pages, boolean sites) {
        if (indexes) {indexRepository.deleteAllInBatch(); log.info("Indexes was deleted.");}
        if (lemmas) {lemmaRepository.deleteAllInBatch(); log.info("Lemmas was deleted.");}
        if (pages) {pageRepository.deleteAllInBatch(); log.info("Pages was deleted.");}
        if (sites) {siteRepository.deleteAllInBatch(); log.info("Sites was deleted.");}
    }

}
