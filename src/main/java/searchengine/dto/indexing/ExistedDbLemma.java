package searchengine.dto.indexing;

import lombok.Data;
import searchengine.model.DBLemma;

@Data
public class ExistedDbLemma implements Comparable<DBLemma>{
    public ExistedDbLemma(DBLemma dbLemma) {
        this.dbLemma = dbLemma;
        this.frequency = dbLemma.getFrequency();
    }

    private DBLemma dbLemma;
    private int frequency;

    @Override
    public int compareTo(DBLemma otherDbLemma) {
        return this.dbLemma.getLemma().compareTo(otherDbLemma.getLemma());
    }
}
