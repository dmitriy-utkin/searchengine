package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.DBLemma;
import searchengine.model.DBSite;

@Repository
@Transactional
public interface LemmaRepository extends JpaRepository<DBLemma, Integer> {
    void deleteByDbSite(DBSite dbSite);
}
