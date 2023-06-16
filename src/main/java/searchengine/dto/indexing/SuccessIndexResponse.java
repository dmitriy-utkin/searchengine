package searchengine.dto.indexing;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SuccessIndexResponse implements IndexResponse {
    private boolean result;
}
