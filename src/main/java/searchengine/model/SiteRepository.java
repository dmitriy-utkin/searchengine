package searchengine.model;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SiteRepository extends CrudRepository<DBSite, Integer> {
    //TODO: to check if I need to change the DBSite
    Optional<DBSite> findByUrl(String url);
    void deleteByUrl(String url);
}
