package searchengine.services.response;

import com.sun.istack.NotNull;
import jdk.jfr.BooleanFlag;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import searchengine.dto.search.SearchDataItem;
import searchengine.dto.statistics.StatisticsData;

import java.util.List;

@Slf4j
public enum ResponseServiceImpl {;

    private interface Result{@BooleanFlag Boolean getResult(); }
    private interface Error{@NotNull String getError(); }

        @Value public static class IndexingSuccessResponseService implements ResponseService, Result {
            public IndexingSuccessResponseService() {
                this.result = true;
            }
            Boolean result;
        }

        @Value public static class StatisticSuccessResponseService implements ResponseService, Result {
            public StatisticSuccessResponseService(StatisticsData statisticsData) {
                this.statistics = statisticsData;
                this.result = true;
            }
            Boolean result;
            StatisticsData statistics;
        }

        @Value public static class SearchSuccessResponseService implements ResponseService, Result {
            public SearchSuccessResponseService(Page<SearchDataItem> items) {
                this.result = true;
                this.count = items.isEmpty() ? 0 : items.getTotalElements();
                this.data = items.getContent();
                log.info("Found " + count + " pages.");
            }
            Boolean result;
            long count;
            List<SearchDataItem> data;
        }

        @Value public static class BadRequest implements ResponseService, Result, Error {
            public BadRequest(String error) {
                this.error = error;
                this.result = false;
            }
            Boolean result;
            String error;
        }
}
