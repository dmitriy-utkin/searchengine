package searchengine.services;

import com.sun.istack.NotNull;
import jdk.jfr.BooleanFlag;
import lombok.Value;
import searchengine.dto.search.SearchResult;
import searchengine.dto.statistics.StatisticsData;

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


        //TODO: протестировать выдачу данных по data, есть подозрение, что отрабоатет некорректно
        @Value public static class SearchSuccessResponseService implements ResponseService, Result {
            public SearchSuccessResponseService(SearchResult searchResult) {
                this.result = true;
                this.count = searchResult.getData().size();
                this.data = searchResult;
            }
            Boolean result;
            int count;
            SearchResult data;
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
