package searchengine.dto.search;

import lombok.Builder;
import lombok.Data;
import searchengine.model.DBPage;

@Data
@Builder
public class LemmaSearchDTO {
    private DBPage page;
    private String lemma;
    private int frequency;
}
