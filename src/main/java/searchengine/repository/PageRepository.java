package searchengine.repository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Optional;

@Repository
@Transactional
public interface PageRepository extends JpaRepository<Page, Integer> {
    Optional<Page> findByPathAndSite(String path, Site site);
    long countBySite(Site site);

    @Cacheable("pageCountCache")
    @Query("SELECT COUNT(p) FROM Page p WHERE p.site = :site")
    long countBySiteWithCache(Site site);

}
