package searchengine.services.search;

import lombok.Builder;
import lombok.Data;
import searchengine.model.DBIndex;
import searchengine.model.DBLemma;

import java.util.List;

@Data
@Builder
public class SearchEngineObject implements Comparable<SearchEngineObject> {
    private String lemma;
    private int totalFrequency;
    private List<DBLemma> dbLemmaList;
    private List<DBIndex> dbIndexList;
    private List<SearchEnginePage> searchEnginePageList;

    @Override
    public int compareTo(SearchEngineObject otherSearchEngineObject) {
        return Integer.compare(this.totalFrequency, otherSearchEngineObject.totalFrequency);
    }
}
