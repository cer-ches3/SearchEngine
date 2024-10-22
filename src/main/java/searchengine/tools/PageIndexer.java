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
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RequiredArgsConstructor
@Data
public class PageIndexer extends RecursiveAction {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SiteModel indexingSite;
    private final Connection connection;
    private final AtomicBoolean indexingEnable;

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
        if (message.contains("UnsupportedMimeTypeException")) {
            errorCode = 415;    // ������ �� pdf, jpg, png ���������
        } else if (message.contains("Status=401")) {
            errorCode = 401;    // �� �������������� �����
        } else if (message.contains("UnknownHostException")) {
            errorCode = 401;
        } else if (message.contains("Status=403")) {
            errorCode = 403;    // ��� �������, 403 Forbidden
        } else if (message.contains("Status=404")) {
            errorCode = 404;    // // ������ �� pdf-��������, �����. ��������, �������������
        } else if (message.contains("Status=500")) {
            errorCode = 401;    // �������� �����������
        } else if (message.contains("ConnectException: Connection refused")) {
            errorCode = 500;    // ERR_CONNECTION_REFUSED, �� ������ ������� ��������
        } else if (message.contains("SSLHandshakeException")) {
            errorCode = 525;
        } else if (message.contains("Status=503")) {
            errorCode = 503; // ������ �������� �� ����� ����������� ������������ ������� �� ����������� �������� (������������, ���������� � ������).
        } else {
            errorCode = -1;
        }
        indexingPage.setCode(errorCode);
    }
}
