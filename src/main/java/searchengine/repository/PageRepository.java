package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.DBPage;

@Repository
public interface PageRepository extends JpaRepository<DBPage, Integer> {
}
