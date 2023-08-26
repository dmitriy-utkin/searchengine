package searchengine.dto.indexing;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LemmaDTO {
    String lemma;
    int frequency;
}
