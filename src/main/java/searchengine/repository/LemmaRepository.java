package searchengine.repository;

import org.springframework.cache.annotation.Cacheable;
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

    @Cacheable("searchLemmaCache")
    Optional<List<Lemma>> findBySiteAndLemma(Site site, String lemma);

    @Cacheable("searchLemmaCache")
    Optional<List<Lemma>> findByLemma(String lemma);

    @Cacheable("searchLemmaCache")
    Optional<List<Lemma>> findAllBySite(Site site);

    @Cacheable("cacheLemmaFrequency")
    @Query("SELECT SUM(l.frequency) FROM Lemma l WHERE l.lemma = :lemma")
    Optional<Float> sumFrequencyByLemma(@Param("lemma") String lemma);

    @Cacheable("cacheLemmaFrequency")
    @Query("SELECT SUM(l.frequency) FROM Lemma l WHERE l.site = :site AND l.lemma = :lemma")
    Optional<Float> sumFrequencyBySiteAndLemma(@Param("site") Site site, @Param("lemma") String lemma);
    long countBySite(Site site);
}
