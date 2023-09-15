package searchengine.dto.search;

import lombok.Builder;
import lombok.Data;
import searchengine.model.DBPage;

import java.util.Set;

@Data
@Builder
public class SearchQueryResult implements Comparable<SearchQueryResult> {
    private DBPage dbPage;
    private int absRel;
    private double relRel;
    private Set<String> lemmas;

    @Override
    public int compareTo(SearchQueryResult otherSearchQueryResult) {
        return Double.compare(otherSearchQueryResult.relRel, this.relRel);
    }
}
