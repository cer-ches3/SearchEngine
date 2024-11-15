package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.responses.ErrorResponse;
import searchengine.dto.responses.OkResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

import java.net.MalformedURLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean indexingEnabled = new AtomicBoolean(false);


    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() throws MalformedURLException {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity startIndexing() {
        if (indexingEnabled.get()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("Индексация уже запущена!"));
        }else {
            executor.submit(()->{
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
        }else {
            indexingEnabled.set(false);
            return ResponseEntity.status(HttpStatus.OK).body(new OkResponse());
        }
    }
}
