package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.DBSite;

import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<DBSite, Integer> {
    Optional<DBSite> findByUrl(String url);
}
