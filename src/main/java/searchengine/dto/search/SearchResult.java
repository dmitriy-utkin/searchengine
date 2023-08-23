package searchengine.dto.search;

import lombok.Data;

import java.util.List;

@Data
public class SearchResult {
    private List<SearchDataItem> data;
}
