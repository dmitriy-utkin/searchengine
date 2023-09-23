package searchengine.dto.search;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import searchengine.model.Page;

@Data
@Builder
@EqualsAndHashCode(exclude = {"rank"})
public class SearchQueryPage implements Comparable<SearchQueryPage> {
    private Page dbPage;
    private int rank;

    @Override
    public int compareTo(SearchQueryPage otherPage) {
        return Integer.compare(this.dbPage.getId(), otherPage.getDbPage().getId());
    }
}
