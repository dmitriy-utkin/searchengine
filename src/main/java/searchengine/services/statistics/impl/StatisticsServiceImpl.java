package searchengine.services.statistics.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.ErrorOptionConfig;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Site;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.response.ResponseService;
import searchengine.services.response.impl.ResponseServiceImpl;
import searchengine.services.indexing.impl.IndexingServiceImpl;
import searchengine.services.statistics.StatisticsService;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SitesList sites;
    private final ErrorOptionConfig errorOptionConfig;

    @Override
    public ResponseEntity<ResponseService> getStatistics() {
        try {
            StatisticsData statisticsData = new StatisticsData();

            TotalStatistics totalStatistics = TotalStatistics.builder()
                    .sites((int) siteRepository.count())
                    .pages((int) pageRepository.count())
                    .lemmas((int) lemmaRepository.count())
                    .indexing(!IndexingServiceImpl.indexationIsRunning)
                    .build();
            statisticsData.setTotal(totalStatistics);

            List<DetailedStatisticsItem> detailedStatisticsItems = new ArrayList<>();
            List<searchengine.config.Site> siteList = sites.getSites();
            for (searchengine.config.Site site : siteList) {
                String url = site.getUrl().endsWith("/") ?
                        site.getUrl().substring(0, site.getUrl().length() - 1) : site.getUrl();
                siteRepository.findByUrl(url)
                        .ifPresent(dbSite -> detailedStatisticsItems.add(createDetailedStatisticsItem(url, dbSite)));
            }
            statisticsData.setDetailed(detailedStatisticsItems);
            return new ResponseEntity<>(new ResponseServiceImpl
                    .StatisticSuccessResponseService(statisticsData), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(new ResponseServiceImpl
                    .ErrorResponse(errorOptionConfig.getInternalServerError()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private DetailedStatisticsItem createDetailedStatisticsItem(String url, Site dbSite) {
        return DetailedStatisticsItem.builder()
                .url(url)
                .name(dbSite.getName())
                .status(dbSite.getStatus().toString())
                .statusTime(dbSite.getStatusTime().getTime())
                .error(dbSite.getLastError() == null ? "Без ошибок" : dbSite.getLastError())
                .pages((int) pageRepository.countBySite(dbSite))
                .lemmas((int) lemmaRepository.countBySite(dbSite))
                .build();
    }
}
