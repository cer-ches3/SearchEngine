package searchengine;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.SiteModel;
import searchengine.model.StatusIndexing;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestApplication {

    @LocalServerPort
    private Integer port;

    private TestRestTemplate template = new TestRestTemplate();

    public static MySQLContainer<?> container = new MySQLContainer<>(DockerImageName.parse("mysql:8"));

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private LemmaRepository lemmasRepository;

    @Autowired
    private IndexRepository indexRepository;

    @BeforeAll
    public static void beforeAll() {
        container.start();
    }

    @AfterAll
    public static void afterAll() {
        container.stop();
    }

    @DynamicPropertySource
    public static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.password", container::getPassword);
        registry.add("spring.datasource.username", container::getUsername);
    }

    @BeforeEach
    public void fillingDatabase() {
        SiteModel siteModel = new SiteModel();
        siteModel.setName("Example.com");
        siteModel.setUrl("https://example.com/");
        siteModel.setStatus(StatusIndexing.INDEXED);
        siteModel.setLastError("");
        siteModel.setStatusTime(LocalDateTime.now());
        siteModel.setPages(new ArrayList<>());
        siteRepository.save(siteModel);
    }

    @AfterEach
    public void clearDatabase() {
        siteRepository.deleteAll();
        pageRepository.deleteAll();
        lemmasRepository.deleteAll();
        indexRepository.deleteAll();
    }

    @Test
    @DisplayName("test get statistic")
    public void testGetStatistic() {
        ResponseEntity<StatisticsResponse> response =
                template.getForEntity("http://localhost:" + port + "/api/statistics", StatisticsResponse.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
    }
}
