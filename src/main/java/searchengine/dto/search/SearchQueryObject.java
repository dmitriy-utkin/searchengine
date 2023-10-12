package searchengine.dto.search;

import lombok.Builder;
import lombok.Data;
import searchengine.model.Lemma;

import java.util.List;

@Data
@Builder
public class SearchQueryObject implements Comparable<SearchQueryObject> {
    private String lemma;
    private int totalFrequency;
    private List<Lemma> dbLemmaList;
    private List<SearchQueryPage> searchQueryPageList;

    @Override
    public int compareTo(SearchQueryObject otherSearchQueryObject) {
        return Integer.compare(this.totalFrequency, otherSearchQueryObject.totalFrequency);
    }
}
