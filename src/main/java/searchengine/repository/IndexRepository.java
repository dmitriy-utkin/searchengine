package searchengine.repository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface IndexRepository extends JpaRepository<Index, Integer> {
    void deleteByPage(Page page);
    Optional<List<Index>> findByPage(Page page);

    @Cacheable("searchCache")
    @EntityGraph(value = "indexWithPageAndSite")
    List<Index> findByLemmaIn(List<Lemma> lemmas);
}
