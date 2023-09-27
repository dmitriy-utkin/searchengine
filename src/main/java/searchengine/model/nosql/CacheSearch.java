package searchengine.model.nosql;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import searchengine.dto.search.SearchDataItem;

import java.time.Instant;
import java.util.Date;
import java.util.List;


@Document(collection = "search_results")
@Data
@Builder
public class CacheSearch {

    @Id
    private String searchResultId;
    private int numberOfResults;
    private List<SearchDataItem> searchDataItems;
    private Date createdAt;

    public void recordCreatedAt() {
        this.createdAt = Date.from(Instant.now());
    }
}
