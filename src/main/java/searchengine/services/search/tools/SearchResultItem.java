package searchengine.services.search.tools;

import lombok.Builder;
import lombok.Data;
import searchengine.dto.search.SearchDataItem;

import java.util.Date;
import java.util.List;

@Data
@Builder
public class SearchResultItem implements Comparable<SearchResultItem> {
    private int resultCount;
    private Date createdAt;
    private List<SearchDataItem> searchResults;

    @Override
    public int compareTo(SearchResultItem o) {
        return o.getCreatedAt().compareTo(this.createdAt);
    }
}
