package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.DBSite;
import searchengine.model.Status;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface SiteRepository extends JpaRepository<DBSite, Integer> {
    Optional<DBSite> findByUrl(String url);
    List<DBSite> findByStatus(Status status);
    Boolean existsByStatus(Status status);
    Optional<Integer> countByStatus(Status status);
}
