package searchengine.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import searchengine.config.SitesList;
import searchengine.model.SiteModel;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.Impl.IndexingServiceImpl;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class TestIndexingService {

    @Mock
    private PageRepository pageRepository;

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private SitesList sitesForIndexing;

    @InjectMocks
    private IndexingServiceImpl indexingService;


    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("test delete sites and pages from DB")
    public void testDeleteSitesAndPagesFromDB() {
        SiteModel siteModel = new SiteModel();
        siteModel.setId(1);

        when(siteRepository.findAll()).thenReturn(Arrays.asList(siteModel));

        indexingService.deleteSitesAndPagesFromDB();

        verify(pageRepository, times(1)).deleteAllBySiteId(1);
        verify(siteRepository, times(1)).deleteById(1);
    }

    @Test
    @DisplayName("test add site to DB")
    public void testAddSiteToDB() {
        SiteModel siteModel = new SiteModel();
        siteModel.setId(1);
        siteRepository.save(siteModel);

        when(siteRepository.findAll()).thenReturn(Arrays.asList(siteModel));

        indexingService.addSiteToDB();

        assertEquals(siteRepository.findAll(), Arrays.asList(siteModel));
        verify(siteRepository, times(1)).save(siteModel);
    }
}
