package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.DBIndex;

@Repository
@Transactional
public interface IndexRepository extends JpaRepository<DBIndex, Integer> {
}
