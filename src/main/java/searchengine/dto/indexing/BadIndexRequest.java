package searchengine.dto.indexing;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BadIndexRequest implements IndexResponse {
    private boolean result;
    private String error;
}
