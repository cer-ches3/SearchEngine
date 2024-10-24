package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Connection;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.SiteModel;
import searchengine.model.StatusIndexing;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.tools.PageIndexer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;


@Service
@Slf4j
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final SitesList sitesForIndexing;
    private final List<SiteModel> listAllSitesFromDB;
    private final Connection connection;

    private volatile AtomicBoolean indexingEnabled;
    private ForkJoinPool forkJoinPool;


    @Override
    public void startIndexing(AtomicBoolean indexingEnabled) {
        this.indexingEnabled = indexingEnabled;
        try {
            deleteSitesAndPagesFromDB();
            addSiteToDB();
            indexingAllSites();
        } catch (RuntimeException | InterruptedException ex) {
            indexingEnabled.set(false);
            log.error("Error: " + ex.getMessage());
        }
    }

    public void deleteSitesAndPagesFromDB() {
        pageRepository.deleteAll();
        siteRepository.deleteAll();
        log.info("База данных очищена!");
    }

    public void addSiteToDB() {
        for (Site site : sitesForIndexing.getSites()) {
            SiteModel newSite = new SiteModel();
            newSite.setName(site.getName());
            newSite.setUrl(site.getUrl());
            newSite.setStatus(StatusIndexing.INDEXING);
            newSite.setStatusTime(LocalDateTime.now());
            siteRepository.save(newSite);
            log.info("В базе данных создан сайт - {}", site.getUrl());
        }
    }

    public void indexingAllSites() throws InterruptedException {
        listAllSitesFromDB.addAll(siteRepository.findAll());
        List<Thread> indexingThreadList = new ArrayList<>();
        for (SiteModel indexingSite : listAllSitesFromDB) {
            Runnable indexSite = () -> {
                log.info("Запущена индексация сайта " + indexingSite.getName());
                forkJoinPool = new ForkJoinPool();
                forkJoinPool.invoke(new PageIndexer(siteRepository, pageRepository, indexingSite, connection, indexingEnabled));

                if (!indexingEnabled.get()) {
                    log.warn("Индексация сайта " + indexingSite.getUrl() + " остановлена пользователем!");
                    SiteModel siteModel = siteRepository.findById(indexingSite.getId()).orElseThrow();
                    siteModel.setStatus(StatusIndexing.FAILED);
                    siteModel.setLastError("Индексация сайта остановлена пользователем!");
                    siteRepository.save(siteModel);
                } else {
                    log.info("Завершена индексация сайта " + indexingSite.getName());
                    SiteModel siteModel = siteRepository.findById(indexingSite.getId()).orElseThrow();
                    siteModel.setStatus(StatusIndexing.INDEXED);
                    siteModel.setStatusTime(LocalDateTime.now());
                    siteRepository.save(siteModel);
                }
            };
            Thread thread = new Thread(indexSite);
            indexingThreadList.add(thread);
            thread.start();

        }
        for (Thread thread : indexingThreadList) {
            thread.join();
        }
        indexingEnabled.set(false);
        log.info("Индексация сайтов завершена!");
    }
}

