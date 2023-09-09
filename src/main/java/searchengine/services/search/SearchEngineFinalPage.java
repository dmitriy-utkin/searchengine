package searchengine.services.search;

import lombok.Builder;
import lombok.Data;
import searchengine.model.DBPage;

@Data
@Builder
public class SearchEngineFinalPage implements Comparable<SearchEngineFinalPage> {
    private DBPage dbPage;
    private int absRel;
    private double relRel;

    @Override
    public int compareTo(SearchEngineFinalPage otherSearchEngineFinalPage) {
        return Double.compare(otherSearchEngineFinalPage.relRel, this.relRel);
    }
}
