package searchengine.dto.search;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import searchengine.model.DBPage;

@Data
@Builder
@EqualsAndHashCode
public class SearchQueryPage implements Comparable<SearchQueryPage> {
    private DBPage dbPage;
    private int rank;

    @Override
    public int compareTo(SearchQueryPage otherPage) {
        return Integer.compare(this.dbPage.getId(), otherPage.getDbPage().getId());
    }
}
