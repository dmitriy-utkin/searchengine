package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.DBPage;

@Repository
public interface PageRepository extends CrudRepository<DBPage, Integer> {
}
