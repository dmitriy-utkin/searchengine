package searchengine.services;

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
                .sites(siteRepository.findAll().size())
                .pages(pageRepository.findAll().size())
                .lemmas(lemmaRepository.findAll().size())
                .indexing(!IndexingServiceImpl.indexationIsRunning)
                .build();
        statisticsData.setTotal(totalStatistics);

        List<DetailedStatisticsItem> detailedStatisticsItems = new ArrayList<>();
        List<Site> siteList = sites.getSites();
        for (Site site : siteList) {
            String url = site.getUrl().endsWith("/") ? site.getUrl().substring(0, site.getUrl().length() - 1) : site.getUrl();
            DBSite dbSite = siteRepository.findByUrl(url).isPresent() ? siteRepository.findByUrl(url).get() : null;
            if (dbSite != null) {
                DetailedStatisticsItem item = DetailedStatisticsItem.builder()
                        .url(url)
                        .name(dbSite.getName())
                        .status(dbSite.getStatus().toString())
                        .statusTime(dbSite.getStatusTime().getTime())
                        .error(dbSite.getLastError())
                        .pages(pageRepository.findByDbSite(dbSite).size())
                        .lemmas(lemmaRepository.findByDbSite(dbSite).size())
                        .build();
                detailedStatisticsItems.add(item);
            }
        }

        statisticsData.setDetailed(detailedStatisticsItems);

        return new ResponseEntity<>(new ResponseServiceImpl.StatisticSuccessResponseService(statisticsData), HttpStatus.OK);
    }
}
