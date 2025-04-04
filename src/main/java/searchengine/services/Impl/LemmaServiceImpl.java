package searchengine.services.Impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.WrongCharaterException;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.services.LemmaService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервис поиска лемм из html-кода страницы
 * с последующим их добавлением в БД.
 * Для работы использует библиотеку LuceneMorphology.
 * @author Сергей Сергеевич Ч
 */
@Service
@Slf4j
public class LemmaServiceImpl implements LemmaService {
    public LuceneMorphology luceneMorphology;
    {
        try {
            luceneMorphology = new RussianLuceneMorphology();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Парсинг html-кода страницы и
     * передача слов в метод получения лемм.
     * @param html
     * @return
     */
    @Override
    public Map<String, Integer> getLemmasFromText(String html) {
        Map<String, Integer> lemmasInText = new HashMap<>();
        String text = Jsoup.parse(html).text();
        List<String> words = new ArrayList<>(List.of(text.toLowerCase().split("[^а-я]+")));
        words.forEach(word -> getLemmaByWord(word, lemmasInText));
        return lemmasInText;
    }

    /**
     * Получение леммы переданного слова,
     * используя библиотеку LuceneMorphology.
     * @param word
     * @param lemmasInText
     */
    public void getLemmaByWord(String word, Map<String, Integer> lemmasInText) {
        try {
            if (!checkWord(word)) {
                return;
            }
            List<String> normalWordForms = luceneMorphology.getNormalForms(word);
            String normalWord = normalWordForms.get(0);
            lemmasInText.put(normalWord, lemmasInText.containsKey(normalWord) ? (lemmasInText.get(normalWord) + 1) : 1);
        } catch (RuntimeException ex) {
            log.debug(ex.getMessage());
        }
    }

    /**
     * Получение леммы переданного слова,
     * используя библиотеку LuceneMorphology.
     * Используется только в SearchServiceImpl
     * для поиска лемм из поискового запроса.
     * @param word
     * @return
     */
    public String getLemmaByWord(String word) {
        String preparedWord = word.toLowerCase();
        try {
            List<String> normalWordForms = luceneMorphology.getNormalForms(preparedWord);
            String wordInfo = luceneMorphology.getMorphInfo(preparedWord).toString();
            return normalWordForms.get(0);
        } catch (WrongCharaterException ex) {
            log.debug(ex.getMessage());
        }
        return "";
    }

    /**
     * Проверка на то, что переданное слово не
     * является союзом, предлогом или междометием,
     * используя библиотеку LuceneMorphology.
     * @param word
     * @return
     */
    private boolean checkWord(String word) {
        List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
        for (String wordBaseForm : wordBaseForms) {
            if (wordBaseForm.contains("ПРЕДЛ") || wordBaseForm.contains("СОЮЗ") || wordBaseForm.contains("МЕЖД")) {
                return false;
            }
        }
        return true;
    }
}