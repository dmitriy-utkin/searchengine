package searchengine.repository.sql;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.sql.Page;
import searchengine.model.sql.Site;

import java.util.Optional;

@Repository
@Transactional
public interface PageRepository extends JpaRepository<Page, Integer> {
    Optional<Page> findByPathAndSite(String path, Site site);
    long countBySite(Site site);
}
