package searchengine.services;

import org.springframework.http.ResponseEntity;
import searchengine.config.Site;

import java.util.List;

public interface SearchService {
    ResponseEntity<Object> search(String query, String site, Integer offset, Integer limit);
    ResponseEntity<Object> searchAllSite(String query, List<Site> sites, Integer offset, Integer limit);
}
