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
    void deleteByDbSite(DBSite dbSite);
    Optional<DBLemma> findByDbSiteAndLemma(DBSite dbSite, String lemma);
    Optional<List<DBLemma>> findByLemma(String lemma);
    List<DBLemma> findByDbSite(DBSite dbSite);
//    @Query("SELECT MAX(l.frequency) FROM lemmas l WHERE l.lemma=:lemma GROUP BY l.lemma")
//    Optional<List<DBLemma>> findMaxFrequencyByLemma(String lemma);
}
