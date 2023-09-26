package searchengine.dto.indexing;

import lombok.Data;
import searchengine.model.sql.Lemma;

@Data
public class ExistedLemma implements Comparable<Lemma>{
    public ExistedLemma(Lemma dbLemma) {
        this.dbLemma = dbLemma;
        this.frequency = dbLemma.getFrequency();
    }

    private Lemma dbLemma;
    private int frequency;

    @Override
    public int compareTo(Lemma otherDbLemma) {
        return this.dbLemma.getLemma().compareTo(otherDbLemma.getLemma());
    }
}
