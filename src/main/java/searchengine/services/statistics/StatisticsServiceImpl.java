package searchengine.services.statistics;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.DBSite;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.response.ResponseService;
import searchengine.services.response.ResponseServiceImpl;
import searchengine.services.indexing.IndexingServiceImpl;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SitesList sites;

    @Override
    public ResponseEntity<ResponseService> getStatistics() {
        StatisticsData statisticsData = new StatisticsData();

        TotalStatistics totalStatistics = TotalStatistics.builder()
                .sites((int) siteRepository.count())
                .pages((int) pageRepository.count())
                .lemmas((int) lemmaRepository.count())
                .indexing(!IndexingServiceImpl.indexationIsRunning)
                .build();
        statisticsData.setTotal(totalStatistics);

        List<DetailedStatisticsItem> detailedStatisticsItems = new ArrayList<>();
        List<Site> siteList = sites.getSites();
        for (Site site : siteList) {
            String url = site.getUrl().endsWith("/") ? site.getUrl().substring(0, site.getUrl().length() - 1) : site.getUrl();
            siteRepository.findByUrl(url).ifPresent(dbSite -> detailedStatisticsItems.add(createDetailedStatisticsItem(url, dbSite)));
        }
        statisticsData.setDetailed(detailedStatisticsItems);
        return new ResponseEntity<>(new ResponseServiceImpl.StatisticSuccessResponseService(statisticsData), HttpStatus.OK);
    }

    private DetailedStatisticsItem createDetailedStatisticsItem(String url, DBSite dbSite) {
        return DetailedStatisticsItem.builder()
                .url(url)
                .name(dbSite.getName())
                .status(dbSite.getStatus().toString())
                .statusTime(dbSite.getStatusTime().getTime())
                .error(dbSite.getLastError())
                .pages(pageRepository.countByDbSite(dbSite).intValue())
                .lemmas((int) lemmaRepository.countByDbSite(dbSite))
                .build();
    }
}
