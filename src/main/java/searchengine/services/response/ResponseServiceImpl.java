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

    @Value public static class IndexingSuccessResponse implements ResponseService, Result {
        public IndexingSuccessResponse() {
            this.result = true;
        }
        Boolean result;
    }

    @Value public static class StatisticSuccessResponse implements ResponseService, Result {
        public StatisticSuccessResponse(StatisticsData statisticsData) {
            this.statistics = statisticsData;
            this.result = true;
        }
        Boolean result;
        StatisticsData statistics;
    }

    @Value public static class SearchSuccessResponse implements ResponseService, Result {
        public SearchSuccessResponse(Page<SearchDataItem> items) {
            this.result = true;
            this.count = items.isEmpty() ? 0 : items.getTotalElements();
            this.data = items.getContent();
            log.info("Found " + count + " pages.");
        }
        Boolean result;
        long count;
        List<SearchDataItem> data;
    }

    @Value public static class ErrorResponse implements ResponseService, Result, Error {
        public ErrorResponse(String error) {
            this.result = false;
            this.error = error;
        }
        Boolean result;
        String error;
    }

}
