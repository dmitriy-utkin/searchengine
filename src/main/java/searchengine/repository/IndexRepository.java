package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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
    Optional<List<DBIndex>> findByDbPage(DBPage dbPage);
    List<DBIndex> findByDbLemmaIn(List<DBLemma> lemmas);

}
