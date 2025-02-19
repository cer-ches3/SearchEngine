package searchengine.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.SiteModel;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ApiControllerV1Test {

    @Mock
    StatisticsService statisticsService;

    @Mock
    private ExecutorService executor;

    @Mock
    SearchService searchService;

    @Mock
    IndexingService indexingService;

    @Mock
    SitesList sitesList;

    @InjectMocks
    ApiControllerV1 apiController;

    MockMvc mockMvc;
    private AtomicBoolean indexingEnabled;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(apiController).build();
        indexingEnabled = new AtomicBoolean(false);
        try {
            java.lang.reflect.Field field = ApiControllerV1.class.getDeclaredField("indexingEnabled");
            field.setAccessible(true);
            field.set(apiController, indexingEnabled);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to inject indexingEnabled", e);
        }
    }

    @Test
    @DisplayName("test statistics")
    public void testStatistics() throws Exception {
        StatisticsResponse mockResponse = new StatisticsResponse();
        when(statisticsService.getStatistics()).thenReturn(mockResponse);

        mockMvc.perform(get("/api/statistics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));

        verify(statisticsService, times(1)).getStatistics();
    }

    @Test
    @DisplayName("test startIndexing if indexing enabled")
    public void testStartIndexingIfIndexingEnabled() {
        indexingEnabled.set(true);

        ResponseEntity response = apiController.startIndexing();

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    @DisplayName("test startIndexing if indexing disabled")
    public void testStartIndexingIfIndexingDisabled() {
        when(executor.submit(any(Runnable.class))).thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        });

        ResponseEntity response = apiController.startIndexing();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(indexingService, times(1)).startIndexing(indexingEnabled);
    }

    @Test
    @DisplayName("test stopIndexing if indexing enabled")
    public void testStopIndexingIfIndexingEnabled() {
        indexingEnabled.set(true);

        ResponseEntity response = apiController.stopIndexing();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("test stopIndexing if indexing disabled")
    public void testStopIndexingIfIndexingDisabled() {
        indexingEnabled.set(false);

        ResponseEntity response = apiController.stopIndexing();

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    @DisplayName("test indexPage if url is valid")
    public void testIndexPageIfUrlIsValid() throws MalformedURLException {
        String urlString = "http://example.com/page";
        URL url = new URL("http://example.com/page");

        Site existingSite = new Site();
        existingSite.setName("Example Site");
        existingSite.setUrl(new URL("http://example.com"));

        List<Site> siteList = new ArrayList<>();
        siteList.add(existingSite);

        SitesList mockSitesList = new SitesList();
        mockSitesList.setSites(siteList);

        when(sitesList.getSites()).thenReturn(siteList);

        ResponseEntity<?> responseEntity = apiController.indexPage(urlString);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        verify(indexingService, times(1)).refreshPage(any(SiteModel.class), eq(url));
    }

    @Test
    @DisplayName("test indexPage if url invalid")
    public void testIndexPageIfUrlInvalid() throws Exception {
        String url = "http://invalid-url.com";

        when(sitesList.getSites()).thenReturn(Collections.singletonList(new Site()));

        mockMvc.perform(post("/api/indexPage").param("url", url))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Данная страница находится за пределами сайтов указанных в конфигурационном файле"));

        verify(indexingService, times(0)).refreshPage(any(SiteModel.class), any(URL.class));
    }

    @Test
    @DisplayName("test search with valid request")
    public void testSearchWithValidRequest() throws Exception {
        String query = "test";
        String site = "example.com";
        Integer offset = 10;
        Integer limit = 10;
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok("");

        when(searchService.search(query, site, offset, limit)).thenReturn(expectedResponse);

        ResponseEntity<Object> response = apiController.search(query, site, offset, limit);

        assertEquals(expectedResponse, response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("test search if query is empty or blank")
    public void testSearchIfQueryIsEmpty() throws Exception {
        String query = "  \t  ";
        String site = "example.com";
        Integer offset = 10;
        Integer limit = 10;

        ResponseEntity<Object> response = apiController.search(query, site, offset, limit);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("test search if site is empty or blank")
    public void testSearchIfSiteIsEmpty() {
        String query = "test";
        List<Site> sites = new ArrayList<>();
        sites.add(new Site());
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok("searchAllSite result");

        when(sitesList.getSites()).thenReturn(sites);
        when(searchService.searchAllSite(query, sites, 10, 10)).thenReturn(expectedResponse);

        ResponseEntity<Object> response = apiController.search(query, "", 10, 10);

        assertEquals(expectedResponse, response);
    }
}
