package searchengine.repository.sql;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.sql.Site;
import searchengine.model.sql.Status;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface SiteRepository extends JpaRepository<Site, Integer> {
    Optional<Site> findByUrl(String url);
    List<Site> findByStatus(Status status);
    Boolean existsByStatus(Status status);
}
