package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.DBIndex;
import searchengine.model.DBPage;

@Repository
@Transactional
public interface IndexRepository extends JpaRepository<DBIndex, Integer> {
    void deleteByDbPage(DBPage dbPage);
}
