package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import searchengine.services.Impl.LemmaServiceImpl;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TestLemmaService {

    @InjectMocks
    private LemmaServiceImpl lemmaService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        lemmaService = new LemmaServiceImpl();
        lemmaService.luceneMorphology = mock(LuceneMorphology.class);
    }

    @Test
    @DisplayName("test getLemmasFromText with normal html")
    public void testGetLemmasFromTextWithNormalHtml() {
        String html = "Мама мыла раму";
        when(lemmaService.luceneMorphology.getNormalForms(anyString())).thenReturn(List.of("мама"), List.of("мыть"), List.of("рама"));

        Map<String, Integer> result = lemmaService.getLemmasFromText(html);

        assertEquals(3, result.size());
        assertEquals(1, result.get("мама"));
        assertEquals(1, result.get("мыть"));
        assertEquals(1, result.get("рама"));

        verify(lemmaService.luceneMorphology, times(3)).getNormalForms(anyString());
    }

    @Test
    @DisplayName("test getLemmasFromText with punctuation in html")
    public void testGetLemmasFromTextWithPunctuationInHtml() {
        String html = "Мама,? мыла/ раму.-";
        when(lemmaService.luceneMorphology.getNormalForms(anyString())).thenReturn(List.of("мама"), List.of("мыть"), List.of("рама"));

        Map<String, Integer> result = lemmaService.getLemmasFromText(html);

        assertEquals(3, result.size());
        assertEquals(1, result.get("мама"));
        assertEquals(1, result.get("мыть"));
        assertEquals(1, result.get("рама"));

        verify(lemmaService.luceneMorphology, times(3)).getNormalForms(anyString());
    }

    @Test
    @DisplayName("test getLemmasFromText with empty html")
    public void testGetLemmasFromTextWithEmptyHtml() {
        String html = "";

        Map<String, Integer> result = lemmaService.getLemmasFromText(html);

        assertTrue(result.isEmpty());
    }
}
