package searchengine.services.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.dto.RankDto;
import searchengine.dto.responses.ErrorResponse;
import searchengine.dto.responses.SearchDataResponse;
import searchengine.dto.responses.SearchResponse;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.SearchService;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

        //Калькулятор релевантности
        Set<RankDto> relevancePages = new HashSet<>();
        for (IndexModel index : indexByLemmas.values()) {
            RankDto rankPage = new RankDto();
            List<Integer> listRankLemmas = new ArrayList<>();
            int absRel = 0;
            for (LemmaModel lemma : lemmasForSearch) {
                rankPage.setAbsRelevance(absRel += indexRepository.getIndexSearchExist(index.getPageId(), lemma.getId()).getLemmaCount());
                listRankLemmas.add(indexRepository.getIndexSearchExist(index.getPageId(), lemma.getId()).getLemmaCount());
            }
            rankPage.setPageId(index.getPageId());
            rankPage.setPageModel(index.getPageModel());
            rankPage.setMaxLemmaRank(Collections.max(listRankLemmas));
            rankPage.setRelativeRelevance(rankPage.getAbsRelevance() / rankPage.getMaxLemmaRank());
            relevancePages.add(rankPage);
        }

        //Сортировка страниц по релевантности
        List<RankDto> relevancePagesSorted = relevancePages.stream().sorted(Comparator.comparingDouble(RankDto::getRelativeRelevance).reversed()).toList();

        //Выдача результата
        List<String> listLemmasFromSearch = new ArrayList<>(lemmasForSearch.stream().map(LemmaModel::getLemma).toList());
        List<SearchDataResponse> searchDataResponses = new ArrayList<>();

        for (RankDto rankPage : relevancePagesSorted) {
            StringBuilder builder = new StringBuilder();
            Document doc = Jsoup.parse(rankPage.getPageModel().getContent());
            List<String> sentences = doc.body().getElementsMatchingOwnText("[\\p{IsCyrillic}]").stream().map(Element::text).toList();
            for (String sentence : sentences) {
                List<String> words = List.of(sentence.split(" "));
                for (String word : words) {
                    String lemmaFromWord = "";
                    if (word.length() > 1) {
                        lemmaFromWord = lemmaService.getLemmaByWord(word);
                    }
                    if (listLemmasFromSearch.contains(lemmaFromWord.toLowerCase())) {
                        builder.append(" <b>").append(word).append("</b>");
                    } else {
                        builder.append(" " + word);
                    }
                }
            }
            SiteModel siteModel = siteRepository.findById(pageRepository.findById(rankPage.getPageId()).get().getSiteId()).get();
            searchDataResponses.add(new SearchDataResponse(
                    siteModel.getUrl(),
                    siteModel.getName(),
                    rankPage.getPageModel().getPath(),
                    doc.title(),
                    extractSnippet(builder.toString()),
                    rankPage.getRelativeRelevance()
            ));
        }
        List<SearchDataResponse> sortedSearchDataResponse = searchDataResponses.stream().sorted(Comparator.comparingDouble(SearchDataResponse::getRelevance).reversed()).toList();
        List<SearchDataResponse> listSearchDataResponseByLimit = new ArrayList<>();
        for (int i = limit * offset; i <= limit * offset + limit; i++) {
            try {
                listSearchDataResponseByLimit.add(sortedSearchDataResponse.get(i));
            } catch (IndexOutOfBoundsException ex) {
                break;
            }
        }
        SearchResponse result = new SearchResponse(true, searchDataResponses.size(), listSearchDataResponseByLimit);
        return ResponseEntity.ok(result);
    }

    private Boolean checkStatusIndexing(String site) {
        StatusIndexing indexingSuccessful = StatusIndexing.INDEXED;
        return siteRepository.getSiteModelByUrl(site).getStatus().equals(indexingSuccessful);
    }

    private String extractSnippet(String text) {
        //Находим выделенное слово
        String highlightedWord = "<b>(.*?)</b>";
        Pattern pattern = Pattern.compile(highlightedWord);
        Matcher matcher = pattern.matcher(text);


        if (matcher.find()) {
            int index = matcher.start(1);
            String[] words = text.split("\\s+");

            int highlightedWordPosition = -1;
            for (int i = 0; i < words.length; i++) {
                int wordStartIndex = text.indexOf(words[i]);
                if (wordStartIndex <= index && (wordStartIndex + words[i].length()) >= index) {
                    highlightedWordPosition = i;
                    break;
                }
            }
            //Определяем его позицию в массиве
            int start = Math.max(0, highlightedWordPosition - 15);
            int end = Math.min(words.length, start + 30);

            // Собираем сниппет
            StringBuilder snippetBuilder = new StringBuilder();
            for (int i = start; i < end; i++) {
                snippetBuilder.append(words[i]).append(" ");
            }
            return snippetBuilder.toString().trim();
        }

        return "";
    }
}
