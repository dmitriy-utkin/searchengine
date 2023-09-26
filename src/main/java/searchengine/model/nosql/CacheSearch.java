package searchengine.model.nosql;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
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
    private int numberOfResult;
    private List<SearchDataItem> searchDataItems;

    @Indexed(name = "createdAtIndex", expireAfter = "${spring.data.mongodb.ttl}")
    private Date createdAt;

    public void populateCreatedAt() {
        this.createdAt = Date.from(Instant.now());
    }

}
