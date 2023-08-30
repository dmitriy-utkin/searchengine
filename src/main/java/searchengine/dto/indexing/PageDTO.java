package searchengine.dto.indexing;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PageDTO {
    private int id;
    private String path;
}
