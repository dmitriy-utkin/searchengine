package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.DBPage;
import searchengine.model.DBSite;

import java.util.Optional;

@Repository
@Transactional
public interface PageRepository extends JpaRepository<DBPage, Integer> {
    void deleteByDbSite(DBSite dbSite);
    Optional<DBPage> findByPath(String path);
}
