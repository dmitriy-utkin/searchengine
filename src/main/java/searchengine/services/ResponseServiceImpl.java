package searchengine.services;

import com.sun.istack.NotNull;
import jdk.jfr.BooleanFlag;
import lombok.Value;
import searchengine.dto.search.SearchDataItem;
import searchengine.dto.statistics.StatisticsData;

import java.util.List;

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

        @Value public static class StatisticSuccessResponseService implements ResponseService, Result {
            public StatisticSuccessResponseService(StatisticsData statisticsData) {
                this.statistics = statisticsData;
                this.result = true;
            }
            Boolean result;
            StatisticsData statistics;
        }


        @Value public static class SearchSuccessResponseService implements ResponseService, Result {
            public SearchSuccessResponseService(List<SearchDataItem> data) {
                this.result = true;
                this.count = data.size();
                this.data = data;
            }
            Boolean result;
            int count;
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

}
