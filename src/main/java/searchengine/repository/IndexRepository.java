package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.DBIndex;
import searchengine.model.DBLemma;
import searchengine.model.DBPage;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface IndexRepository extends JpaRepository<DBIndex, Integer> {
    void deleteByDbPage(DBPage dbPage);
    Optional<List<DBIndex>> findByDbLemma(DBLemma dbLemma);
    Optional<List<DBIndex>> findByDbPage(DBPage dbPage);
//    @Query("SELECT SUM(l.frequency) FROM DBLemma l WHERE l.lemma IN :lemmas")
//    List<DBIndex> findByDbLemmaIn(@Param("lemmas") List<DBLemma> lemmas);
    List<DBIndex> findByDbLemmaIn(List<DBLemma> lemmas);

}
