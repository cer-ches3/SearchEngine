package searchengine.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.services.Impl.PageIndexerServiceImpl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TestPageIndexerService {
    @Mock
    private LemmaService lemmaService;

    @Mock
    private LemmaRepository lemmaRepository;

    @Mock
    private IndexRepository indexRepository;

    @InjectMocks
    private PageIndexerServiceImpl pageIndexerService;

    private PageModel indexingPage;
    private SiteModel siteModel;

    @BeforeEach
    public void setUp() {
        siteModel = new SiteModel();
        siteModel.setId(1);

        indexingPage = new PageModel();
        indexingPage.setId(1);
        indexingPage.setSiteId(1);
        indexingPage.setSiteModel(siteModel);
    }

    @Test
    @DisplayName("test indexHtml")
    public void testindexHtml() throws IOException {
        String content = "test content";
        Map<String, Integer> lemmas = new HashMap<>();
        lemmas.put("test", 1);
        lemmas.put("content", 2);

        when(lemmaService.getLemmasFromText(content)).thenReturn(lemmas);

        pageIndexerService.indexHtml(content, indexingPage);

        verify(lemmaService, times(1)).getLemmasFromText(content);
        verify(lemmaRepository, times(2)).getLemmaExist(anyString(), anyInt());
    }

    @Test
    @DisplayName("test saveLemma if lemma not exist")
    public void testSaveLemmaIfLemmaNotExist() {
        String lemmaText = "text";
        Integer frequency = 3;
        LemmaModel existingLemma = new LemmaModel();

        when(lemmaRepository.getLemmaExist(lemmaText, indexingPage.getSiteId())).thenReturn(existingLemma);

        pageIndexerService.saveLemma(lemmaText, frequency, indexingPage);

        assertEquals(3, existingLemma.getFrequency());
        verify(lemmaRepository, times(1)).saveAndFlush(existingLemma);
        verify(indexRepository, times(1)).getIndexSearchExist(indexingPage.getId(), existingLemma.getId());
    }

    @Test
    @DisplayName("test saveLemma if lemma exist")
    public void testSaveLemmaIfLemmaExist() {
        String lemmaText = "text";
        Integer frequency = 3;
        LemmaModel existingLemma = new LemmaModel();
        existingLemma.setFrequency(7);

        when(lemmaRepository.getLemmaExist(lemmaText, indexingPage.getSiteId())).thenReturn(existingLemma);

        pageIndexerService.saveLemma(lemmaText, frequency, indexingPage);

        assertEquals(10, existingLemma.getFrequency());
        verify(lemmaRepository, times(1)).saveAndFlush(existingLemma);
        verify(indexRepository, times(1)).getIndexSearchExist(indexingPage.getId(), existingLemma.getId());
    }

    @Test
    @DisplayName("test createIndex if index not exist")
    public void testCreateIndexIfIndexNotExist() {
        LemmaModel lemma = new LemmaModel();
        lemma.setId(1);
        Integer rank = 3;
        IndexModel existingIndex = new IndexModel();

        when(indexRepository.getIndexSearchExist(indexingPage.getId(), lemma.getId())).thenReturn(existingIndex);

        pageIndexerService.createIndex(indexingPage, lemma, rank);

        assertEquals(3, existingIndex.getLemmaCount());
        verify(indexRepository, times(1)).save(existingIndex);
    }

    @Test
    @DisplayName("test createIndex if index exist")
    public void testCreateIndexIfIndexExist() {
        LemmaModel lemma = new LemmaModel();
        lemma.setId(1);
        Integer rank = 3;
        IndexModel existingIndex = new IndexModel();
        existingIndex.setLemmaCount(7);

        when(indexRepository.getIndexSearchExist(indexingPage.getId(), lemma.getId())).thenReturn(existingIndex);

        pageIndexerService.createIndex(indexingPage, lemma, rank);

        assertEquals(10, existingIndex.getLemmaCount());
        verify(indexRepository, times(1)).save(existingIndex);
    }

    @Test
    @DisplayName("test refreshLemmaAndIndex")
    public void testRefreshLemmaAndIndex() throws IOException {
        String content = "test content";
        indexingPage.setContent(content);
        Map<String, Integer> lemmas = new HashMap<>();
        lemmas.put("test", 1);
        lemmas.put("content", 2);

        when(lemmaService.getLemmasFromText(content)).thenReturn(lemmas);

        pageIndexerService.refreshLemmaAndIndex(indexingPage);

        verify(indexRepository, times(1)).deleteAllByPageId(indexingPage.getId());
        verify(lemmaRepository, times(2)).getLemmaExist(anyString(), anyInt());
    }

}
