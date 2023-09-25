package searchengine.services.indexing.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.ErrorOptionConfig;
import searchengine.config.JsoupConfig;
import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.indexing.IndexingService;
import searchengine.services.response.ResponseService;
import searchengine.services.response.impl.ResponseServiceImpl;

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

    @Override
    public ResponseEntity<ResponseService> startIndexing(){
        try {
            if (siteRepository.existsByStatus(Status.INDEXING)) return new ResponseEntity<>(new ResponseServiceImpl
                        .ErrorResponse(errorOptionConfig.getStartIndexingError()), HttpStatus.METHOD_NOT_ALLOWED);
            indexationIsRunning = true;
            clearDataBase();
            sitesList.getSites().forEach(site -> siteRepository.save(createSiteEntry(site)));
            siteRepository.findAll().forEach(dbSite -> new Thread(() -> {
                new ForkJoinPool().invoke(new SiteParseAction(jsoupConfig,siteRepository, pageRepository,
                        lemmaRepository, lemmaFinder, indexRepository, dbSite, dbSite.getUrl() + "/",
                        new ConcurrentHashMap<>()));
                updateSiteStatus(dbSite, Status.INDEXED);
            }).start());
            return new ResponseEntity<>(new ResponseServiceImpl.IndexingSuccessResponseService(), HttpStatus.OK);
        } catch (Exception exception) {
            return new ResponseEntity<>(new ResponseServiceImpl
                    .ErrorResponse(errorOptionConfig.getInternalServerError()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<ResponseService> stopIndexing() {
        try {
            if (!siteRepository.existsByStatus(Status.INDEXING)) return new ResponseEntity<>(new ResponseServiceImpl
                        .ErrorResponse(errorOptionConfig.getStopIndexingError()), HttpStatus.METHOD_NOT_ALLOWED);
            indexationIsRunning = false;
            List<Site> sites = siteRepository.findByStatus(Status.INDEXING);
            sites.forEach(site -> updateSiteStatus(site,Status.FAILED,
                                errorOptionConfig.getIndexingInterruptedError()));
            return new ResponseEntity<>(new ResponseServiceImpl.IndexingSuccessResponseService(), HttpStatus.OK);
        } catch (Exception exception) {
            log.error("Error in method \".stopIndexing()\":" + exception.getMessage());
            return new ResponseEntity<>(new ResponseServiceImpl
                    .ErrorResponse(errorOptionConfig.getInternalServerError()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<ResponseService> indexPage(String newUrl) {
        try {
            String preparedUrl = newUrl.toLowerCase().trim();
            Site dbSite = siteRepository.findAll().stream().filter(site -> preparedUrl.startsWith(site.getUrl()))
                    .findFirst().orElse(null);
            if (dbSite == null) return new ResponseEntity<>(new ResponseServiceImpl
                        .ErrorResponse(errorOptionConfig.getSiteOutOfConfigError()), HttpStatus.NOT_FOUND);
            if (dbSite.getStatus().equals(Status.INDEXING)) return new ResponseEntity<>(new ResponseServiceImpl
                        .ErrorResponse(errorOptionConfig.getSiteIsIndexingError()),HttpStatus.METHOD_NOT_ALLOWED);
            new Thread(() -> updateDataBaseByOnePage(preparedUrl, dbSite)).start();
            return new ResponseEntity<>(new ResponseServiceImpl.IndexingSuccessResponseService(), HttpStatus.OK);
        } catch (Exception exception) {
            log.error("Error in method \".indexPage(String newUrl)\":" + exception.getMessage());
            return new ResponseEntity<>(new ResponseServiceImpl
                    .ErrorResponse(errorOptionConfig.getInternalServerError()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Site createSiteEntry(searchengine.config.Site site) {
        return Site.builder()
                .status(Status.INDEXING)
                .url(prepareSiteUrl(site.getUrl()))
                .name(site.getName())
                .statusTime(new Date()).build();
    }

    private String prepareSiteUrl(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private void updateSiteStatus(Site site, Status newStatus) {
        if (newStatus.equals(Status.INDEXED) && !indexationIsRunning) return;
        if (pageRepository.countBySite(site) == 1) {
            updateSiteStatus(site, Status.FAILED, errorOptionConfig.getMainPageUnavailableError());
            return;
        }
        site.setStatus(newStatus);
        site.setStatusTime(new Date());
        siteRepository.saveAndFlush(site);
        log.info("Site status for \"" + site.getUrl() + "\" was changed to \"" + newStatus + "\".");
    }

    private void updateSiteStatus(Site site, Status newStatus, String lastError) {
        site.setStatus(newStatus);
        site.setLastError(lastError);
        site.setStatusTime(new Date());
        siteRepository.saveAndFlush(site);
        log.info("Site status for \""+ site.getUrl() + "\" was changed to \""
                + newStatus.toString() + "\" with last error \"" + lastError + "\".");
        if (siteRepository.existsByStatus(Status.INDEXING)) indexationIsRunning = false;

    }

    private void clearDataBase() {
        indexRepository.deleteAllInBatch(); log.info("Indexes was deleted.");
        lemmaRepository.deleteAllInBatch(); log.info("Lemmas was deleted.");
        pageRepository.deleteAllInBatch(); log.info("Pages was deleted.");
        siteRepository.deleteAllInBatch(); log.info("Sites was deleted.");
    }

    private void updateDataBaseByOnePage(String url, Site site) {
        indexationIsRunning = true;
        Status initialStatus = site.getStatus();
        updateSiteStatus(site, Status.INDEXING);
        Page oldPage = pageRepository.findByPathAndSite(url.replace(site.getUrl(), ""),site)
                .orElse(null);
        clearDataBaseByOnePage(oldPage);
        PageInfoCreator creator = new PageInfoCreator(site, url, jsoupConfig,lemmaFinder, lemmaRepository);
        pageRepository.save(creator.getPage());
        if (creator.getLemmas() != null) lemmaRepository.saveAllAndFlush(creator.getLemmas());
        if (creator.getIndexes() != null) indexRepository.saveAllAndFlush(creator.getIndexes());
        updateSiteStatus(site, initialStatus);
        siteRepository.save(site);
        indexationIsRunning = false;
    }

    private void clearDataBaseByOnePage(Page page) {
        try {
            if (page == null) return;
            Optional<List<Index>> lemmaList = indexRepository.findByPage(page);
            lemmaList.ifPresent(indexes -> lemmaRepository.saveAllAndFlush(indexes.stream().map(dbIndex -> {
                Lemma dbLemma = dbIndex.getLemma();
                dbLemma.setFrequency(dbLemma.getFrequency() - 1);
                return dbLemma;
            }).toList()));
            indexRepository.deleteByPage(page);
            pageRepository.deleteById(page.getId());
        } catch (Exception e) {
            log.error("Error in method clearDataBaseByOnePage(Optional<DBPage> optionalDBPage): " + e.getMessage());
        }
    }
}
