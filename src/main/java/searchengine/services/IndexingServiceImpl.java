package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.model.DBSite;
import searchengine.model.Status;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    @Override
    public DBSite getSite(Site site) {
        DBSite dbSite = new DBSite();
        dbSite.setUrl(site.getUrl());
        dbSite.setName(site.getName());
        dbSite.setStatus(Status.INDEXING);
        dbSite.setStatusTime(new Date());
        return dbSite;
    }
}
