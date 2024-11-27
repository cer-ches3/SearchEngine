package searchengine.tools;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.Connection;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.PageIndexerService;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RequiredArgsConstructor
@Data
public class PageIndexer extends RecursiveAction {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SiteModel indexingSite;
    private final Connection connection;
    private final AtomicBoolean indexingEnable;
    private final PageIndexerService pageIndexerService;

    private ConcurrentSkipListSet<String> listLinks;
    private PageModel indexingPage;

    @Override
    protected void compute() {
        listLinks = getListLinks(indexingSite.getUrl());
        for (String path : listLinks) {
            try {
                org.jsoup.Connection connect = Jsoup.connect(path)
                        .userAgent(connection.getUserAgent())
                        .referrer(connection.getReferer());
                Document document = connect.get();
                indexingPage = new PageModel();
                indexingPage.setSiteId(indexingSite.getId());
                indexingPage.setPath(path.substring(indexingSite.getUrl().length()));
                indexingPage.setCode(connect.response().statusCode());
                indexingPage.setContent(document.html());
                pageRepository.save(indexingPage);
            } catch (IOException ex) {
                errorIdentifier(ex, indexingPage);
                pageRepository.save(indexingPage);
            }
            if (!indexingEnable.get()) {
                return;
            }
            SiteModel siteModel = siteRepository.findById(indexingSite.getId()).orElseThrow();
            siteModel.setStatusTime(LocalDateTime.now());
            siteRepository.save(siteModel);
            pageIndexerService.indexHtml(indexingPage.getContent(), indexingPage);
        }

    }

    public ConcurrentSkipListSet<String> getListLinks(String url) {
        listLinks = new ConcurrentSkipListSet<>();
        try {
            org.jsoup.Connection connect = Jsoup.connect(indexingSite.getUrl())
                    .userAgent(connection.getUserAgent())
                    .referrer(connection.getReferer());
            Document document = connect.get();
            Elements elements = document.select("a");
            for (Element element : elements) {
                String link = element.absUrl("href");
                if (link.startsWith(url) && !link.equals(url) && !listLinks.contains(link)) {
                    listLinks.add(link);
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return listLinks;
    }

    void errorIdentifier(Exception ex, PageModel indexingPage) {
        String message = ex.toString();
        int errorCode;
        if (message.contains("Status=401") || message.contains("UnknownHostException")) {
            errorCode = 401;    // Не авторизован || Не существующий домен
        } else if (message.contains("Status=403")) {
            errorCode = 403;    // Нет доступа, 403 Forbidden
        } else if (message.contains("Status=404")) {
            errorCode = 404;    // // Ссылка на не существующую страницу
        } else if (message.contains("UnsupportedMimeTypeException")) {
            errorCode = 415;    // Неподдерживаемый тип данных. Ссылка на документ, pdf, jpg, png
        } else if (message.contains("Status=500") || message.contains("ConnectException: Connection refused")) {
            errorCode = 500;    // Внутренняя ошибка сервера || ERR_CONNECTION_REFUSED, не удаётся открыть страницу
        } else if (message.contains("Status=503")) {
            errorCode = 503;    // Cервис недоступен
        } else {
            errorCode = -1;
        }
        indexingPage.setCode(errorCode);
    }

    public void refreshPage(URL urlRefreshingPage) {
        PageModel pageFromDB = pageRepository.findPageByPath(urlRefreshingPage.toString().substring(indexingSite.getUrl().length()));
        if (pageFromDB != null) {
            log.info("Сайт уже присутствует в БД. Обновление данных!");
            pageIndexerService.refreshLemmaAndIndex(pageFromDB);
        }else {
            try {
                org.jsoup.Connection connect = Jsoup.connect(urlRefreshingPage.toString())
                        .userAgent(connection.getUserAgent())
                        .referrer(connection.getReferer());
                Document document = connect.get();
                indexingPage = new PageModel();
                indexingPage.setSiteId(indexingSite.getId());
                indexingPage.setPath(urlRefreshingPage.toString().substring(indexingSite.getUrl().length()));
                indexingPage.setCode(connect.response().statusCode());
                indexingPage.setContent(document.html());
                pageRepository.save(indexingPage);
                pageIndexerService.refreshLemmaAndIndex(indexingPage);
            } catch (IOException ex) {
                errorIdentifier(ex, indexingPage);
                pageRepository.save(indexingPage);
                pageIndexerService.refreshLemmaAndIndex(indexingPage);
            }
        }
    }
}


