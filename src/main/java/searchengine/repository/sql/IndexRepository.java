package searchengine.repository.sql;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.sql.Index;
import searchengine.model.sql.Lemma;
import searchengine.model.sql.Page;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface IndexRepository extends JpaRepository<Index, Integer> {
    void deleteByPage(Page page);
    Optional<List<Index>> findByPage(Page page);
    @EntityGraph(value = "indexWithPageAndSite")
    List<Index> findByLemmaIn(List<Lemma> lemmas);
}
