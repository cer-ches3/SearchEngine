package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaModel;


@Repository
public interface LemmaRepository extends JpaRepository<LemmaModel, Integer> {
    @Query(value = "select * from lemma t where t.lemma = :lemma and t.site_id = :siteId for update", nativeQuery = true)
    LemmaModel lemmaExist(String lemma, Integer siteId);

    @Query(value = "select count(l) from LemmaModel l where l.siteId = :siteId")
    Integer findCountLemmasBySiteId(Integer siteId);
}