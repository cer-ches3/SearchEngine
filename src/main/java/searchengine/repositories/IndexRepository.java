package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexModel;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexModel, Integer> {
    @Query(value = "select i from IndexModel i where i.pageId = :pageId and i.lemmaId = :lemmaId")
    IndexModel getIndexSearchExist(@Param("pageId") Integer pageId, @Param("lemmaId") Integer lemmaId);

    @Modifying
    @Transactional
    @Query(value = "delete from IndexModel i where i.pageId = :pageId")
    void deleteAllByPageId(@Param("pageId") Integer pageId);

    @Query(value = "select i from IndexModel i where i.lemmaId = :lemmaId")
    List<IndexModel> getIndexesByLemma(Integer lemmaId);
}
