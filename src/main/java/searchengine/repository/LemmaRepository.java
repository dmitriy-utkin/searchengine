package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    Optional<List<Lemma>> findBySiteAndLemma(Site site, String lemma);
    Optional<List<Lemma>> findByLemma(String lemma);
    Optional<List<Lemma>> findAllBySite(Site site);
    @Query("SELECT SUM(l.frequency) FROM Lemma l WHERE l.lemma = :lemma")
    Optional<Float> sumFrequencyByLemma(@Param("lemma") String lemma);
    @Query("SELECT SUM(l.frequency) FROM Lemma l WHERE l.site = :site AND l.lemma = :lemma")
    Optional<Float> sumFrequencyBySiteAndLemma(@Param("site") Site site, @Param("lemma") String lemma);
    long countBySite(Site site);
}
