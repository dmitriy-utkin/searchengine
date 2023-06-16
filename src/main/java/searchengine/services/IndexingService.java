package searchengine.services;

import searchengine.config.Site;
import searchengine.model.DBSite;

public interface IndexingService {
    DBSite getSite(Site site);
}
