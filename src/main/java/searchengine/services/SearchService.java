package searchengine.services;

import org.springframework.http.ResponseEntity;

public interface SearchService {
    ResponseEntity<Object> search(String query, String site, Integer offset, Integer limit);
}
