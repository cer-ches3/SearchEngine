package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaModel;

import java.util.List;


@Repository
public interface LemmaRepository extends JpaRepository<LemmaModel, Integer> {
    @Query(value = "select * from lemma t where t.lemma = :lemma and t.site_id = :siteId for update", nativeQuery = true)
    LemmaModel getLemmaExist(String lemma, Integer siteId);

    @Query(value = "select count(l) from LemmaModel l where l.siteId = :siteId")
    Integer getCountLemmasBySiteId(Integer siteId);

    @Query(value = "select l from LemmaModel l where l.lemma = :lemma and (:siteId is null or l.siteId = :siteId)")
    List<LemmaModel> getLemmasByLemmaAndSiteId(String lemma, Integer siteId);

    @Query(value = "select l.frequency from LemmaModel l where l.lemma = :lemma and (:siteId is null or l.siteId = :siteId)")
    Integer getCountPageByLemma(String lemma, Integer siteId);
}