package searchengine.services.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.services.LemmaService;
import searchengine.services.PageIndexerService;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PageIndexerServiceImpl implements PageIndexerService {
    private final LemmaService lemmaService;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Override
    public void indexHtml(String html, PageModel indexingPage) {
        try {
            Map<String, Integer> lemmas = lemmaService.getLemmasFromText(html);
            lemmas.entrySet().parallelStream().forEach(entry -> saveLemma(entry.getKey(), entry.getValue(), indexingPage));
        } catch (IOException e) {
            log.error(String.valueOf(e));
            throw new RuntimeException(e);
        }
    }

    public void saveLemma(String k, Integer v, PageModel indexingPage) {
        LemmaModel existLemmaInDB = lemmaRepository.getLemmaExist(k, indexingPage.getSiteId());
        if (existLemmaInDB != null) {
            existLemmaInDB.setFrequency(existLemmaInDB.getFrequency() + v);
            lemmaRepository.saveAndFlush(existLemmaInDB);
            createIndex(indexingPage, existLemmaInDB, v);
        } else {
            try {
                LemmaModel newLemmaToDB = new LemmaModel();
                newLemmaToDB.setSiteId(indexingPage.getSiteId());
                newLemmaToDB.setLemma(k);
                newLemmaToDB.setFrequency(v);
                newLemmaToDB.setSiteModel(indexingPage.getSiteModel());
                lemmaRepository.saveAndFlush(newLemmaToDB);
                createIndex(indexingPage, newLemmaToDB, v);
            } catch (DataIntegrityViolationException ex) {
                log.debug("Ошибка при сохранении леммы, такая лемма уже существует. Вызов повторного сохранения");
                saveLemma(k, v, indexingPage);
            }
        }
    }

    private void createIndex(PageModel indexingPage, LemmaModel lemmaInDB, Integer rank) {
        IndexModel indexSearchExist = indexRepository.getIndexSearchExist(indexingPage.getId(), lemmaInDB.getId());
        if (indexSearchExist != null) {
            indexSearchExist.setLemmaCount(indexSearchExist.getLemmaCount() + rank);
            indexRepository.save(indexSearchExist);
        } else {
            IndexModel index = new IndexModel();
            index.setPageId(indexingPage.getId());
            index.setLemmaId(lemmaInDB.getId());
            index.setLemmaCount(rank);
            index.setLemmaModel(lemmaInDB);
            index.setPageModel(indexingPage);
            indexRepository.save(index);
        }
    }

    @Override
    public void refreshLemmaAndIndex(PageModel indexingPage) {
        try {
            Map<String, Integer> lemmas = lemmaService.getLemmasFromText(indexingPage.getContent());
            indexRepository.deleteAllByPageId(indexingPage.getId());
            lemmas.entrySet().parallelStream().forEach(entry -> saveLemma(entry.getKey(), entry.getValue(), indexingPage));
            log.debug("Обновление индекса страницы " + " lemmas:" + lemmas.size());
        } catch (IOException e) {
            log.error(String.valueOf(e));
            throw new RuntimeException(e);
        }
    }
}
