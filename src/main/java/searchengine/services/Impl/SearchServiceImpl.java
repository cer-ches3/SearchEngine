package searchengine.services.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.dto.RankDto;
import searchengine.dto.responses.ErrorResponse;
import searchengine.dto.responses.SearchDataResponse;
import searchengine.dto.responses.SearchResponse;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;
import searchengine.model.SiteModel;
import searchengine.model.StatusIndexing;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.SearchService;

import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaServiceImpl lemmaService;
    private final double frequencyLimitProportion = 60;

    @Override
    public ResponseEntity<Object> search(String query, String site, Integer offset, Integer limit) {
        if (!checkStatusIndexing(site)) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Индексация сайта " + site + " не завершена. Поиск не возможен."));
        }

        SiteModel siteFromQuery = siteRepository.getSiteModelByUrl(site);
        Integer countPages = pageRepository.getCountPagesBySiteId(siteFromQuery.getId());


        //Получение лемм из текста запроса
        List<LemmaModel> lemmasForSearch = lemmaService.getLemmasFromText(query).keySet()
                .stream()
                .map(lemma -> lemmaRepository.getLemmasByLemmaAndSiteId(lemma, siteFromQuery.getId()))
                .flatMap(java.util.Collection::stream).collect(Collectors.toList());
        lemmasForSearch.removeIf(lemma -> {
            Integer lemmaFrequency = lemmaRepository.getCountPageByLemma(lemma.getLemma(), lemma.getSiteId());
            return ((double) lemmaFrequency / countPages > frequencyLimitProportion);
        });
        if (lemmasForSearch.isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Поиск не дал результатов."));
        }

        //Сортировка полученного списка по frequency
        lemmasForSearch.sort(Comparator.comparing(LemmaModel::getFrequency));

        //Поиск страниц по леммам
        Map<Integer, IndexModel> indexByLemmas = indexRepository.getIndexesByLemma(lemmasForSearch.get(0).getId())
                .stream()
                .collect(Collectors.toMap(IndexModel::getPageId, index -> index));
        for (int i = 1; i < lemmasForSearch.size(); i++) {
            List<IndexModel> indexesNextLemma = indexRepository.getIndexesByLemma(lemmasForSearch.get(i).getId());
            List<Integer> pagesToSave = new ArrayList<>();
            for (IndexModel index : indexesNextLemma) {
                pagesToSave.add(index.getPageId());
            }
            indexByLemmas.entrySet().removeIf(entry -> !pagesToSave.contains(entry.getKey()));
        }
        if (indexByLemmas.isEmpty()) {
            return ResponseEntity.ok().body(new SearchResponse(true, 0, Collections.emptyList()));
        }

        //Rank калькулятор
        Set<RankDto> pagesRelevance = new HashSet<>();
        int pageId = indexByLemmas.values().stream().toList().get(0).getPageId();
        RankDto rankPage = new RankDto();
        for (IndexModel index : indexByLemmas.values()) {
                rankPage.setRelativeRelevance(rankPage.getAbsRelevance() / rankPage.getMaxLemmaRank());
                pagesRelevance.add(rankPage);
                rankPage = new RankDto();
                rankPage.setPageModel(index.getPageModel());
                pageId = index.getPageId();

            rankPage.setPageId(index.getPageId());
            rankPage.setAbsRelevance(rankPage.getAbsRelevance() + index.getLemmaCount());
            if (rankPage.getMaxLemmaRank() < index.getLemmaCount()) rankPage.setMaxLemmaRank(index.getLemmaCount());
        }
        rankPage.setRelativeRelevance(rankPage.getAbsRelevance() / rankPage.getMaxLemmaRank());
        pagesRelevance.add(rankPage);

        //Сортировка страниц по релевантности
        List<RankDto> pagesRelevanceSorted = pagesRelevance.stream().sorted(Comparator.comparingDouble(RankDto::getRelativeRelevance).reversed()).toList();

        //Вывод результатов поиска
        List<String> simpleLemmasFromSearch = new ArrayList<>(lemmasForSearch.stream().map(LemmaModel::getLemma).toList());
        List<SearchDataResponse> searchDataResponses = new ArrayList<>();
        for (RankDto rank : pagesRelevanceSorted) {
            Document doc = Jsoup.parse(rank.getPageModel().getContent());
            List<String> sentences = doc.body().getElementsMatchingOwnText("[\\p{IsCyrillic}]").stream().map(Element::text).toList();
            for (String sentence : sentences) {
                StringBuilder textFromElement = new StringBuilder(sentence);
                List<String> words = List.of(sentence.split("[\s:punct]"));
                int searchWords = 0;
                for (String word : words) {
                    String lemmaFromWord = lemmaService.getLemmaByWord1(word.replaceAll("\\p{Punct}", ""));
                    if (simpleLemmasFromSearch.contains(lemmaFromWord)) {
                        markWord(textFromElement, word, 0);
                        searchWords += 1;
                    }
                }
                if (searchWords != 0) {
                    SiteModel siteModel = siteRepository.findById(pageRepository.findById(rank.getPageId()).get().getSiteId()).get();
                    searchDataResponses.add(new SearchDataResponse(
                            siteModel.getUrl(),
                            siteModel.getName(),
                            rank.getPageModel().getPath(),
                            doc.title(),
                            textFromElement.toString(),
                            rank.getRelativeRelevance(),
                            searchWords
                    ));
                }
            }
        }
        /*List<SearchDataResponse> sortedSearchDataResponse = searchDataResponses.stream().sorted(Comparator.comparingDouble(SearchDataResponse::getRelevance).reversed()).toList();
        List<SearchDataResponse> result = new ArrayList<>();
        for (int i = limit * offset; i <= limit * offset + limit; i++) {
            try {
                result.add(sortedSearchDataResponse.get(i));
            } catch (IndexOutOfBoundsException ex) {
                break;
            }
        }
        result = result.stream().sorted(Comparator.comparingInt(SearchDataResponse::getWordsFound).reversed()).toList();
        return ResponseEntity.ok(result);*/
        return ResponseEntity.ok().body(new ErrorResponse("ok"));
    }

    private Boolean checkStatusIndexing(String site) {
        StatusIndexing indexingSuccessful = StatusIndexing.INDEXED;
        return siteRepository.getSiteModelByUrl(site).getStatus().equals(indexingSuccessful);
    }

    private void markWord(StringBuilder textFromElement, String word, int startPosition) {
        int start = textFromElement.indexOf(word, startPosition);
        if (textFromElement.indexOf("<b>", start - 3) == (start - 3)) {
            markWord(textFromElement, word, start + word.length());
            return;
        }
        int end = start + word.length();
        textFromElement.insert(start, "<b>");
        if (end == -1) {
            textFromElement.insert(textFromElement.length(), "</b>");
        } else textFromElement.insert(end + 3, "</b>");
    }
}
