package searchengine.services;

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
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;

@Service
public class IndexingServiceImpl implements IndexingService {

    @Override
    public ResponseEntity<IndexResponseService> startIndexing(SitesList sitesList,
                                                              SiteRepository siteRepository,
                                                              PageRepository pageRepository) throws IOException {
        for (Site site : sitesList.getSites()) {
            Optional<DBSite> dbSite = siteRepository.findByUrl(site.getUrl());
            if (dbSite.isPresent() && dbSite.get().getStatus().equals(Status.INDEXING)) {
                return new ResponseEntity<>(new IndexResponseServiceImpl.Response.BadRequest("Indexation already started."), HttpStatus.BAD_REQUEST);
            }
            if (dbSite.isPresent() && dbSite.get().getStatus().equals(Status.INDEXED)) {
                pageRepository.deleteAll();
                siteRepository.deleteById(dbSite.get().getId());
            }
            siteRepository.save(getDBSite(site));
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
}
