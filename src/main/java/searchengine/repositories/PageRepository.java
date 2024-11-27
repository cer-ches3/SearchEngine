package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;

import javax.transaction.Transactional;

public interface PageRepository extends JpaRepository<PageModel, Integer> {
    @Query(value = "select count(p) from PageModel p where p.siteId = :siteId")
    Integer findCountRecordBySiteId(@Param("siteId") Integer siteId);

    @Query(value = "select * from page t where  t.path = :path limit 1", nativeQuery = true)
    PageModel findPageByPath(@Param("path") String path);

    @Modifying
    @Transactional
    @Query(value = "delete from PageModel p where p.siteId = :siteId")
    void deleteAllBySiteId(@Param("siteId") Integer siteId);

}
