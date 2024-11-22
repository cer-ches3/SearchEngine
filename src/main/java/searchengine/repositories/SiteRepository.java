package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.SiteModel;

public interface SiteRepository extends JpaRepository<SiteModel, Integer> {

}
