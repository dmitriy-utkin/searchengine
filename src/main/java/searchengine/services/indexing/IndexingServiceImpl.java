package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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

    //TODO: придумать механизм "завершения индексации", т.е. добавить обновление статуса -> Status.INDEXED
    //TODO: поработать над механизмом выставления "ошибок", к примеру в случае, если главная страница недоступна

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
                    new SiteParseAction(jsoupConfig, siteRepository, pageRepository, lemmaRepository, lemmaFinder, indexRepository, dbSite.getId(), dbSite.getUrl(), new ConcurrentHashMap<>())
            )).start();
        });
        return new ResponseEntity<>(new ResponseServiceImpl.IndexingSuccessResponseService(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ResponseService> stopIndexing() {
        if (!siteRepository.existsByStatus(Status.INDEXING)) return new ResponseEntity<>(new ResponseServiceImpl.BadRequest(STOP_INDEXING_ERROR), HttpStatus.BAD_REQUEST);
        try {
            indexationIsRunning = false;
            List<DBSite> sites = siteRepository.findByStatus(Status.INDEXING);
            sites.forEach(site -> updateSiteStatus(site, Status.FAILED, "Индексация остановлена пользователем"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ResponseEntity<>(new ResponseServiceImpl.IndexingSuccessResponseService(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ResponseService> indexPage(String newUrl) {
        String preparedUrl = newUrl.toLowerCase().trim();
        if (sitesList.getSites().stream().map(Site::getUrl).noneMatch(preparedUrl::startsWith)) return new ResponseEntity<>(new ResponseServiceImpl.BadRequest(INDEX_PAGE_ERROR), HttpStatus.BAD_REQUEST);
        List<DBSite> sites = siteRepository.findAll().stream().filter(site -> preparedUrl.startsWith(site.getUrl())).toList();
        updateSiteStatus(sites.get(0), Status.INDEXING);
        clearDataBaseByOnePage(pageRepository.findByPathAndDbSite(preparedUrl.replace(sites.get(0).getUrl(), ""), sites.get(0)));
        DBPage dbPage = pageRepository.save(createNewPageEntry(preparedUrl, sites.get(0)));
        updateDataBaseForOneIndexedPage(sites.get(0), dbPage, new HtmlParse(sites.get(0), dbPage, lemmaFinder, lemmaRepository));
        updateSiteStatus(sites.get(0), Status.INDEXED);
        return new ResponseEntity<>(new ResponseServiceImpl.IndexingSuccessResponseService(), HttpStatus.OK);
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
        site.setStatus(newStatus);
        site.setStatusTime(new Date());
        siteRepository.saveAndFlush(site);
        log.info("Site status for \"" + site.getUrl() + "\" was changed to \"" + newStatus.toString() + "\".");
    }

    private void updateSiteStatus(DBSite site, Status newStatus, String lastError) {
        site.setStatus(newStatus);
        site.setLastError(lastError);
        site.setStatusTime(new Date());
        siteRepository.saveAndFlush(site);
        log.info("Site status for \"" + site.getUrl() + "\" was changed to \"" + newStatus.toString() + "\" with last error \"" + lastError + "\".");

    }

    private void clearDataBase(boolean indexes, boolean lemmas, boolean pages, boolean sites) {
        if (indexes) {indexRepository.deleteAllInBatch(); log.info("Indexes was deleted.");}
        if (lemmas) {lemmaRepository.deleteAllInBatch(); log.info("Lemmas was deleted.");}
        if (pages) {pageRepository.deleteAllInBatch(); log.info("Pages was deleted.");}
        if (sites) {siteRepository.deleteAllInBatch(); log.info("Sites was deleted.");}
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
        if (optionalDBPage.isEmpty()) return;
        List<DBLemma> lemmas = indexRepository.findByDbPage(optionalDBPage.get()).get().stream().map(dbIndex -> {DBLemma dbLemma = dbIndex.getDbLemma(); dbLemma.setFrequency(dbLemma.getFrequency() - 1); return dbLemma;}).toList();
        lemmaRepository.saveAll(lemmas);
        indexRepository.deleteByDbPage(optionalDBPage.get());
        pageRepository.deleteById(optionalDBPage.get().getId());

    }

    private void updateDataBaseForOneIndexedPage(DBSite site, DBPage page, HtmlParse htmlParse) {
        pageRepository.save(page);
        if (htmlParse.getLemmas() != null) lemmaRepository.saveAll(htmlParse.getLemmas());
        if (htmlParse.getIndexes() != null) indexRepository.saveAll(htmlParse.getIndexes());
        site.setStatusTime(new Date());
        siteRepository.save(site);
    }

}
