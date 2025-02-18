package searchengine.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.SiteModel;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.Impl.StatisticsServiceImpl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TestStatisticsService {
    @Mock
    private SitesList sites;

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private PageRepository pageRepository;

    @Mock
    private LemmaRepository lemmaRepository;

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    @BeforeEach
    public void setup() {
        when(sites.getSites()).thenReturn(new ArrayList<>());
    }

    @Test
    @DisplayName("test getStatistics")
    public void testGetStatistics() throws MalformedURLException {
        SiteModel siteModel = new SiteModel();
        siteModel.setName("Example.com");
        siteModel.setUrl("https://example.com");
        siteModel.setId(1);
        when(siteRepository.findAll()).thenReturn(List.of(siteModel));
        when(pageRepository.getCountPagesBySiteId(any())).thenReturn(10);
        when(lemmaRepository.getCountLemmasBySiteId(any())).thenReturn(20);

        StatisticsResponse response = statisticsService.getStatistics();

        assertEquals(1, response.getStatistics().getDetailed().size());
        assertEquals("Example.com", response.getStatistics().getDetailed().get(0).getName());
        assertEquals("https://example.com", response.getStatistics().getDetailed().get(0).getUrl());
        assertEquals(10, response.getStatistics().getDetailed().get(0).getPages());
        assertEquals(20, response.getStatistics().getDetailed().get(0).getLemmas());
    }

    @Test
    @DisplayName("test getStartStatistics")
    public void testGetStartStatistics() throws MalformedURLException {
        Site site = new Site();
        site.setName("Example.com");
        site.setUrl(new URL("https://example.com"));
        when(sites.getSites()).thenReturn(List.of(site));

        StatisticsResponse response = statisticsService.getStartStatistics();

        assertEquals(1, response.getStatistics().getDetailed().size());
        assertEquals("Example.com", response.getStatistics().getDetailed().get(0).getName());
        assertEquals("https://example.com", response.getStatistics().getDetailed().get(0).getUrl());
        assertEquals(0, response.getStatistics().getDetailed().get(0).getPages());
        assertEquals(0, response.getStatistics().getDetailed().get(0).getLemmas());
    }
}
