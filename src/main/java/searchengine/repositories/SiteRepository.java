package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.model.SiteModel;

public interface SiteRepository extends JpaRepository<SiteModel, Integer> {
    @Query(value = "select * from site s where s.url = :host limit 1", nativeQuery = true)
    SiteModel getSiteModelByUrl(@Param("host") String url);
}
