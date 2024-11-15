package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.model.PageModel;

public interface PageRepository extends JpaRepository<PageModel, Integer> {
    @Query(value = "select * from page t where t.site_id = :siteId and t.path = :path limit 1", nativeQuery = true)
    PageModel findPageBySiteIdAndPath(@Param("path") String path, @Param("siteId") Integer siteId);

    @Query(value = "select count(p) from PageModel p where p.siteId = :siteId")
    Integer findCountRecordBySiteId(@Param("siteId") Integer siteId);


}
