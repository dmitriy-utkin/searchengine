package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.DBSite;

import java.util.Date;
import java.util.Optional;

@Repository
public interface SiteRepository extends CrudRepository<DBSite, Integer> {
    Optional<DBSite> findByUrl(String url);
}
