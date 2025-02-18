package searchengine.services.Impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteModel;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.StatisticsService;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис, возвращающий статистику по сайтам.
 * @author Сергей Сергеевич Ч
 */
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    /**
     * Метод возвращает актуальную статистику,
     * по проиндексированным страницам.
     * @return
     * @throws MalformedURLException если не корректный URL
     */
    @Override
    public StatisticsResponse getStatistics() throws MalformedURLException {
        List<SiteModel> sitePages = siteRepository.findAll();
        if (sitePages.isEmpty()) {
            return getStartStatistics();
        }
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<SiteModel> sites = siteRepository.findAll();
        for (SiteModel siteModel : sites) {
            Site site = new Site();
            site.setName(siteModel.getName());
            site.setUrl(new URL(siteModel.getUrl()));
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl().toString());
            int pages = pageRepository.getCountPagesBySiteId(siteModel.getId());
            int lemmas = lemmaRepository.getCountLemmasBySiteId(siteModel.getId());
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(String.valueOf(siteModel.getStatus()));
            item.setError(siteModel.getLastError());
            item.setStatusTime(siteModel.getStatusTime());
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

    /**
     * Метод возвращает пустую статистику,
     * если индексация страниц еще не запускалась и
     * БД пуста.
     * @return
     */
    public StatisticsResponse getStartStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(false);
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        for (Site site : sites.getSites()) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(String.valueOf(site.getUrl()));
            item.setPages(0);
            item.setLemmas(0);
            item.setStatus(null);
            item.setError(null);
            item.setStatus("WAIT");
            item.setStatusTime(LocalDateTime.now());
            detailed.add(item);
        }
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
