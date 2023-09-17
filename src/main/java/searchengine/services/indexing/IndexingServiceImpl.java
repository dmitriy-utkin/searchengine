package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.ErrorOptionConfig;
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
    private final ErrorOptionConfig errorOptionConfig;

    public static boolean indexationIsRunning = false;

    //TODO: поработать над механизмом выставления "ошибок", к примеру в случае, если главная страница недоступна
    //TODO: доработать механизм выставления статуса "INDEXED" -> сейчас в некоторых сайтах не выставляется статус

    @Override
    public ResponseEntity<ResponseService> startIndexing(){
        try {
            if (siteRepository.existsByStatus(Status.INDEXING))
                return new ResponseEntity<>(new ResponseServiceImpl.ErrorResponse(errorOptionConfig.getStartIndexingError()), HttpStatus.METHOD_NOT_ALLOWED);
            indexationIsRunning = true;
            clearDataBase();
            sitesList.getSites().forEach(site -> siteRepository.save(createSiteEntry(site)));
            siteRepository.findAll().forEach(dbSite -> new Thread(() -> {
                new ForkJoinPool().invoke(
                        new SiteParseAction(jsoupConfig,
                                siteRepository, pageRepository, lemmaRepository, lemmaFinder, indexRepository,
                                dbSite, dbSite.getUrl() + "/",
                                new ConcurrentHashMap<>())
                );
                updateSiteStatus(dbSite, Status.INDEXED);
            }).start());
            return new ResponseEntity<>(new ResponseServiceImpl.IndexingSuccessResponseService(), HttpStatus.OK);
        } catch (Exception exception) {
            return new ResponseEntity<>(new ResponseServiceImpl.ErrorResponse(errorOptionConfig.getInternalServerError()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<ResponseService> stopIndexing() {
        try {
            if (!siteRepository.existsByStatus(Status.INDEXING))
                return new ResponseEntity<>(new ResponseServiceImpl.ErrorResponse(errorOptionConfig.getStopIndexingError()), HttpStatus.METHOD_NOT_ALLOWED);
            try {
                indexationIsRunning = false;
                List<DBSite> sites = siteRepository.findByStatus(Status.INDEXING);
                sites.forEach(site -> updateSiteStatus(site, Status.FAILED, "Индексация остановлена пользователем"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new ResponseEntity<>(new ResponseServiceImpl.IndexingSuccessResponseService(), HttpStatus.OK);
        } catch (Exception exception) {
            log.error("Error in method \".stopIndexing()\":" + exception.getMessage());
            return new ResponseEntity<>(new ResponseServiceImpl.ErrorResponse(errorOptionConfig.getInternalServerError()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<ResponseService> indexPage(String newUrl) {
        try {
            String preparedUrl = newUrl.toLowerCase().trim();
            if (sitesList.getSites().stream().map(Site::getUrl).noneMatch(preparedUrl::startsWith))
                return new ResponseEntity<>(new ResponseServiceImpl.ErrorResponse(errorOptionConfig.getIndexPageSiteIsOutOfTheListError()), HttpStatus.NOT_FOUND);
            DBSite dbSite = siteRepository.findAll().stream()
                    .filter(site -> preparedUrl.startsWith(site.getUrl())).toList().get(0);
            if (dbSite.getStatus().equals(Status.INDEXING))
                return new ResponseEntity<>(new ResponseServiceImpl.ErrorResponse(errorOptionConfig.getIndexPageSiteIsInIndexingProcessError()), HttpStatus.METHOD_NOT_ALLOWED);
            new Thread(() -> {
                indexationIsRunning = true;
                Status initialStatus = dbSite.getStatus();
                updateSiteStatus(dbSite, Status.INDEXING);
                clearDataBaseByOnePage(pageRepository.findByPathAndDbSite(preparedUrl.replace(dbSite.getUrl(), ""), dbSite));
                DBPage dbPage = pageRepository.save(createNewPageEntry(preparedUrl, dbSite));
                updateDataBaseForOneIndexedPage(dbSite, dbPage, new HtmlParser(dbSite, dbPage, lemmaFinder, lemmaRepository));
                updateSiteStatus(dbSite, initialStatus);
                indexationIsRunning = false;
            }).start();
            return new ResponseEntity<>(new ResponseServiceImpl.IndexingSuccessResponseService(), HttpStatus.OK);
        } catch (Exception exception) {
            log.error("Error in method \".indexPage(String newUrl)\":" + exception.getMessage());
            return new ResponseEntity<>(new ResponseServiceImpl.ErrorResponse(errorOptionConfig.getInternalServerError()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

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

    private void updateSiteStatus(DBSite site, Status newStatus) {
        if (newStatus.equals(Status.INDEXED) && !indexationIsRunning) return;
        if (pageRepository.countByDbSite(site) == 1) {updateSiteStatus(site, Status.FAILED, errorOptionConfig.getMainPageUnavailableError()); return;}
        site.setStatus(newStatus);
        site.setStatusTime(new Date());
        siteRepository.saveAndFlush(site);
        log.info("Site status for \"" + site.getUrl() + "\" was changed to \"" + newStatus + "\".");
    }

    private void updateSiteStatus(DBSite site, Status newStatus, String lastError) {
        site.setStatus(newStatus);
        site.setLastError(lastError);
        site.setStatusTime(new Date());
        siteRepository.saveAndFlush(site);
        log.info("Site status for \"" + site.getUrl() + "\" was changed to \"" + newStatus.toString() + "\" with last error \"" + lastError + "\".");
        if (siteRepository.existsByStatus(Status.INDEXING)) indexationIsRunning = false;

    }

    private void clearDataBase() {
        indexRepository.deleteAllInBatch(); log.info("Indexes was deleted.");
        lemmaRepository.deleteAllInBatch(); log.info("Lemmas was deleted.");
        pageRepository.deleteAllInBatch(); log.info("Pages was deleted.");
        siteRepository.deleteAllInBatch(); log.info("Sites was deleted.");
    }

    private DBPage createNewPageEntry(String uri, DBSite site) {
        try {
            Connection.Response response = Jsoup.connect(uri)
                    .userAgent(jsoupConfig.getUserAgent())
                    .referrer(jsoupConfig.getReferrer())
                    .timeout(jsoupConfig.getTimeout())
                    .ignoreHttpErrors(true)
                    .followRedirects(jsoupConfig.isRedirect())
                    .execute();
            Document doc = response.parse();
            return DBPage.builder()
                    .path(uri.replace(site.getUrl(), ""))
                    .dbSite(site)
                    .code(response.statusCode())
                    .content(doc.outerHtml())
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return DBPage.builder()
                    .path(uri.replace(site.getUrl(), ""))
                    .dbSite(site)
                    .code(404)
                    .content("")
                    .build();
        }
    }

    private void clearDataBaseByOnePage(Optional<DBPage> optionalDBPage) {
        try {
            if (optionalDBPage.isEmpty()) return;
            List<DBLemma> lemmas = indexRepository.findByDbPage(optionalDBPage.get()).get().stream().map(dbIndex -> {
                DBLemma dbLemma = dbIndex.getDbLemma();
                dbLemma.setFrequency(dbLemma.getFrequency() - 1);
                return dbLemma;
            }).toList();
            lemmaRepository.saveAll(lemmas);
            indexRepository.deleteByDbPage(optionalDBPage.get());
            pageRepository.deleteById(optionalDBPage.get().getId());
        } catch (Exception e) {
            log.error("Error in method clearDataBaseByOnePage(Optional<DBPage> optionalDBPage): " + e.getMessage());
        }

    }

    private void updateDataBaseForOneIndexedPage(DBSite site, DBPage page, HtmlParser htmlParse) {
        pageRepository.save(page);
        if (htmlParse.getLemmas() != null) lemmaRepository.saveAllAndFlush(htmlParse.getLemmas());
        if (htmlParse.getIndexes() != null) indexRepository.saveAllAndFlush(htmlParse.getIndexes());
        site.setStatusTime(new Date());
        siteRepository.save(site);
    }

}
