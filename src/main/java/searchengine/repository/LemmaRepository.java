package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.DBLemma;
import searchengine.model.DBSite;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface LemmaRepository extends JpaRepository<DBLemma, Integer> {
    Optional<DBLemma> findByDbSiteAndLemma(DBSite dbSite, String lemma);
    Optional<List<DBLemma>> findByLemma(String lemma);
    List<DBLemma> findAllByDbSite(DBSite dbSite);
    @Query("SELECT SUM(l.frequency) FROM DBLemma l WHERE l.lemma = :lemma")
    Float sumFrequencyByLemma(@Param("lemma") String lemma);
    @Query("SELECT SUM(l.frequency) FROM DBLemma l WHERE l.dbSite = :dbSite AND l.lemma = :lemma")
    Float sumFrequencyByDbSiteAndLemma(@Param("dbSite") DBSite dbSite, @Param("lemma") String lemma);

}
