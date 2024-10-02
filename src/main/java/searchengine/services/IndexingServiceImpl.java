package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.SiteModel;
import searchengine.model.StatusIndexing;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;


@Service
@Slf4j
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final SitesList sitesForIndexing;

    private volatile AtomicBoolean indexingEnabled;


    @Override
    public void startIndexing(AtomicBoolean indexingEnabled) {
        this.indexingEnabled = indexingEnabled;
        try {
            deleteSitesAndPagesFromDB();
            addSiteToDB();
            indexingAllSites();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteSitesAndPagesFromDB() {
        siteRepository.deleteAll();
        pageRepository.deleteAll();
        log.info("Database is cleared!");
    }

    public void addSiteToDB() {
        for (Site site : sitesForIndexing.getSites()) {
            SiteModel newSite = new SiteModel();
            newSite.setName(site.getName());
            newSite.setUrl(site.getUrl());
            newSite.setStatus(StatusIndexing.INDEXING);
            newSite.setStatusTime(LocalDateTime.now());
            siteRepository.save(newSite);
            log.info("A site has been added to the database - {}", site.getUrl());
        }
    }

    public void indexingAllSites() throws InterruptedException, IOException {
        log.info("Site indexing has been started");
        new ForkJoinPool().invoke(new PageParsingServiceImpl(pageRepository, siteRepository));
        log.info("Site indexing has been finished");
    }
}
