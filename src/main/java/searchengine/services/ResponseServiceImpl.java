package searchengine.services;

import com.sun.istack.NotNull;
import jdk.jfr.BooleanFlag;
import lombok.Value;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.DBSite;

import java.util.List;
import java.util.Map;

public enum ResponseServiceImpl {;

    private interface Result{@BooleanFlag Boolean getResult(); }
    private interface Error{@NotNull String getError(); }

    public enum Response {;

        @Value public static class IndexingSuccessResponseService implements ResponseService, Result {
            public IndexingSuccessResponseService() {
                this.result = true;
            }
            Boolean result;
        }

        @Value public static class BadRequest implements ResponseService, Result, Error {
            public BadRequest(String error) {
                this.error = error;
                this.result = false;
            }
            Boolean result;
            String error;
        }

        @Value public static class StatisticSuccessResponseService implements ResponseService, Result {
            public StatisticSuccessResponseService(StatisticsData statisticsData) {
                this.statistics = statisticsData;
                this.result = true;
            }
            Boolean result;
            StatisticsData statistics;

        }

    }

}
