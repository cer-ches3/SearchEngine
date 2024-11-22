package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexModel;

@Repository
public interface IndexRepository extends JpaRepository<IndexModel, Integer> {

    @Query(value = "select i from IndexModel i where i.pageId = :pageId and i.lemmaId = :lemmaId")
    IndexModel indexSearchExist(@Param("pageId") Integer pageId, @Param("lemmaId") Integer lemmaId);

}
