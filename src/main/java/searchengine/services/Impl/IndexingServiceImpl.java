package searchengine.services.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Connection;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.SiteModel;
import searchengine.model.StatusIndexing;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.services.PageIndexerService;
import searchengine.tools.PageIndexer;

import java.net.URL;
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
    private final LemmaRepository lemmaRepository;
    private final SitesList sitesForIndexing;
    private final List<SiteModel> listAllSitesFromDB;
    private final Connection connection;
    private final PageIndexerService pageIndexerService;

    private volatile AtomicBoolean indexingEnabled;

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
        List<SiteModel> listSitesFromDB = siteRepository.findAll();
        listSitesFromDB.forEach(siteModel -> {
            pageRepository.deleteAllBySiteId(siteModel.getId());
            siteRepository.deleteById(siteModel.getId());
        });
        log.info("База данных очищена!");
    }

    public void addSiteToDB() {
        for (Site site : sitesForIndexing.getSites()) {
            SiteModel newSite = new SiteModel();
            newSite.setName(site.getName());
            newSite.setUrl(String.valueOf(site.getUrl()));
            newSite.setStatus(StatusIndexing.INDEXING);
            newSite.setStatusTime(LocalDateTime.now());
            siteRepository.save(newSite);
            log.info("В базе данных создан сайт - {}", site.getUrl());
        }
    }

    public void indexingAllSites() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        listAllSitesFromDB.addAll(siteRepository.findAll());
        List<Thread> indexingThreadList = new ArrayList<>();
        for (SiteModel indexingSite : listAllSitesFromDB) {
            Runnable indexSite = () -> {
                log.info("Запущена индексация сайта " + indexingSite.getName());
                new ForkJoinPool().invoke(new PageIndexer(siteRepository, pageRepository, lemmaRepository, indexingSite, connection, indexingEnabled, pageIndexerService));

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
        long endTime = System.currentTimeMillis();
        log.info("Индексация сайтов завершена! Время индексации: " + timeConverter(startTime, endTime));
    }

    @Override
    public void refreshPage(SiteModel refreshingSite, URL urlRefreshingPage) {
        SiteModel siteModelFromDB = siteRepository.getSiteModelByUrl(refreshingSite.getUrl());
        refreshingSite.setId(siteModelFromDB.getId());
        try {
            log.info("Запущена переиндексация страницы: " + urlRefreshingPage);
            PageIndexer refPageIndexer = new PageIndexer(siteRepository, pageRepository, lemmaRepository, refreshingSite, connection, indexingEnabled, pageIndexerService);
            refPageIndexer.refreshPage(urlRefreshingPage);
        } catch (SecurityException ex) {
            SiteModel sitePage = siteRepository.findById(refreshingSite.getId()).orElseThrow();
            sitePage.setStatus(StatusIndexing.FAILED);
            sitePage.setLastError(ex.getMessage());
            siteRepository.save(sitePage);
        }
        log.info("Проиндексирован сайт: " + refreshingSite.getName());
        SiteModel sitePage = siteRepository.findById(refreshingSite.getId()).orElseThrow();
        sitePage.setStatus(StatusIndexing.INDEXED);
        sitePage.setStatusTime(LocalDateTime.now());
        sitePage.setLastError(null);
        siteRepository.save(sitePage);
    }

    public String timeConverter(Long startTime, Long endTime) {
        long resultTime = endTime - startTime;

        long hour = resultTime / 3600000;
        long minute = resultTime % 3600000 / 60000;
        long second = resultTime % 60000 / 1000;

        return hour + " ч. " + minute + " мин. " + second + " сек.";
    }
}

