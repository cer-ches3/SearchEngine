package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteModel;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.net.MalformedURLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

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
        for (SiteModel sitePage : sites) {
            Site site = new Site();
            site.setName(sitePage.getName());
            site.setUrl(sitePage.getUrl());
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl().toString());
            int pages = pageRepository.findCountRecordBySiteId(sitePage.getId());
            int lemmas = 0;
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(String.valueOf(sitePage.getStatus()));
            item.setError(sitePage.getLastError());
            item.setStatusTime(sitePage.getStatusTime());
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

    StatisticsResponse getStartStatistics() {
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
