package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.SitesList;
import searchengine.dto.responses.ErrorResponse;
import searchengine.dto.responses.OkResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.SiteModel;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean indexingEnabled = new AtomicBoolean(false);
    private final SitesList sitesList;


    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() throws MalformedURLException {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity startIndexing() {
        if (indexingEnabled.get()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("Индексация уже запущена!"));
        } else {
            executor.submit(() -> {
                indexingEnabled.set(true);
                indexingService.startIndexing(indexingEnabled);
            });
            return ResponseEntity.status(HttpStatus.OK).body(new OkResponse());
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity stopIndexing() {
        if (!indexingEnabled.get()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("Индексация не запущена"));
        } else {
            indexingEnabled.set(false);
            return ResponseEntity.status(HttpStatus.OK).body(new OkResponse());
        }
    }

    @PostMapping("/indexPage")
    public ResponseEntity indexPage(@RequestParam String url) throws MalformedURLException {
        URL urlRefreshingPage = new URL(url);
        SiteModel refreshingSite = new SiteModel();
        try {
            sitesList.getSites().stream().filter(site -> urlRefreshingPage.getHost().equals(site.getUrl().getHost())).findFirst()
                    .map(site -> {
                        refreshingSite.setName(site.getName());
                        refreshingSite.setUrl(site.getUrl().toString());
                        return refreshingSite;
                    }).orElseThrow();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Данная страница находится за пределами сайтов указанных в конфигурационном файле"));
        }
        indexingService.refreshPage(refreshingSite, urlRefreshingPage);
        return ResponseEntity.status(HttpStatus.OK).body(new OkResponse());
    }

    @GetMapping("/search")
    public ResponseEntity<Object> search(
            @RequestParam String query,
            @RequestParam String site,
            @RequestParam(defaultValue = "0") Integer offset,
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        if (query == null || query.isEmpty() || query.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Задан пустой поисковый запрос"));
        }
        if (site == null || site.isEmpty() || site.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Не указан сайт для поиска"));
        }
        return searchService.search(query, site, offset, limit);
    }
}
