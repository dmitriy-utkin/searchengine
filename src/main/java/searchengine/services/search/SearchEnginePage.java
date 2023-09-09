package searchengine.services.search;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import searchengine.model.DBPage;

@Data
@Builder
@EqualsAndHashCode
public class SearchEnginePage implements Comparable<SearchEnginePage> {
    private DBPage dbPage;
    private int rank;

    @Override
    public int compareTo(SearchEnginePage otherPage) {
        return Integer.compare(this.rank, otherPage.getRank());
    }

}
