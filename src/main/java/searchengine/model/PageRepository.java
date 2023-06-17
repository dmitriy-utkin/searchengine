package searchengine.model;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.dto.indexing.PageDTO;

@Repository
public interface PageRepository extends CrudRepository<DBPage, Integer> {
}
