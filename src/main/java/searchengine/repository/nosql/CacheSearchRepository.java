package searchengine.repository.nosql;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.nosql.CacheSearch;

@Repository
public interface CacheSearchRepository extends MongoRepository<CacheSearch, String> {
}
