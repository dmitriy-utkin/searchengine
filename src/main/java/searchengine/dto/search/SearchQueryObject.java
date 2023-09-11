package searchengine.dto.search;

import lombok.Builder;
import lombok.Data;
import searchengine.model.DBIndex;
import searchengine.model.DBLemma;

import java.util.List;

@Data
@Builder
public class SearchQueryObject implements Comparable<SearchQueryObject> {
    private String lemma;
    private int totalFrequency;
    private List<DBLemma> dbLemmaList;
    private List<DBIndex> dbIndexList;
    private List<SearchQueryPage> searchQueryPageList;

    @Override
    public int compareTo(SearchQueryObject otherSearchQueryObject) {
        return Integer.compare(this.totalFrequency, otherSearchQueryObject.totalFrequency);
    }
}
