package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.asm.MemberSubstitution;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;

@Service
@Slf4j
public class IndexingServiceImpl implements IndexingService {

    @Override
    public ResponseEntity<IndexResponseService> startIndexing(SitesList sitesList,
                                                              SiteRepository siteRepository,
                                                              PageRepository pageRepository) throws IOException {

        ConcurrentSkipListSet<String> preparedLinks;
        for (Site site : sitesList.getSites()) {
            //TODO: change the rules to use 'www' in yaml
            String siteUrl;
            DBSite preparedSite;
            Optional<DBSite> dbSite = siteRepository.findByUrl(site.getUrl());
            if (dbSite.isPresent() && dbSite.get().getStatus().equals(Status.INDEXING)) {
                return new ResponseEntity<>(new IndexResponseServiceImpl.Response.BadRequest("Indexation already started."), HttpStatus.BAD_REQUEST);
            }
            if (dbSite.isPresent() && dbSite.get().getStatus().equals(Status.INDEXED)) {
                siteRepository.deleteById(dbSite.get().getId());
            }
            preparedSite = getDBSite(site);
            siteUrl = preparedSite.getUrl();
            log.info("\t\t\t\t->>>> " + siteUrl + " started");
            //TODO: пробросить внутрь парсера функционал от pageRepository?
            preparedLinks = launchSiteParser(new SiteParser(siteUrl, siteUrl));

            preparedLinks.forEach(page -> {
                try {
                    pageRepository.save(DBPage.builder()
                                    .path(page.replace(siteUrl, ""))
                                    .code(200)
                                    .content("TEST TEXT")
                                    .dbSite(preparedSite)
                                    .build());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            preparedSite.setStatus(Status.INDEXED);
            siteRepository.save(preparedSite);
            preparedLinks.clear();
        }
        return new ResponseEntity<>(new IndexResponseServiceImpl.Response.SuccessResponseService(), HttpStatus.OK);
    }


    @Override
    public DBSite getDBSite(Site site) {
        DBSite dbSite = new DBSite();
        dbSite.setStatus(Status.INDEXING);
        dbSite.setStatusTime(new Date());
        dbSite.setName(site.getName());
        dbSite.setUrl(site.getUrl());
        return dbSite;
    }

    //TODO: check if i can to delete the addition method from intrface (launcher of the Site Parser \|/

    @Override
    public ConcurrentSkipListSet<String> launchSiteParser(SiteParser siteParser) {
        return new ForkJoinPool().invoke(siteParser);
    }
}
