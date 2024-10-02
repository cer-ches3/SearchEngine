package searchengine.tools;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.concurrent.ConcurrentSkipListSet;

import static java.lang.Thread.sleep;

public class HtmlParser {

    private ConcurrentSkipListSet<String> listLinks;

    public ConcurrentSkipListSet<String> getListLinks(String url) {
        listLinks = new ConcurrentSkipListSet<>();
        checkingUrl(url);
        try {
            sleep(150);
            Connection connection = Jsoup.connect(url)
                    .userAgent("Mozilla")
                    .referrer("http://www.google.com")
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true);
            Document document = connection.get();
            Elements elements = document.select("a");
            for (Element element : elements) {
                String link = element.absUrl("href");
                if (link.startsWith(url) && !link.equals(url) && !listLinks.contains(link)) {
                    listLinks.add(link);
                }
            }
        } catch (InterruptedException | IOException e) {
            System.out.println(e.getMessage());
        }
        return listLinks;
    }

    public String getContent(String path) throws IOException {
        Connection connection = Jsoup.connect(path)
                .userAgent("Mozilla")
                .referrer("http://www.google.com")
                .ignoreContentType(true)
                .ignoreHttpErrors(true);
        Document document = connection.get();
        return document.html();
    }

    public Integer getStatusCode(String path) {
        try {
            Connection connection = Jsoup.connect(path)
                    .userAgent("Mozilla")
                    .referrer("http://www.google.com")
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true);
            Document document = connection.get();
            Integer code = document.connection().response().statusCode();
            return code;
        } catch (HttpStatusException e) {
            return e.getStatusCode();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String checkingUrl(String url) {
        char lastIndex = url.charAt(url.length() - 1);
        if (lastIndex != '/') {
            url += "/";
        }
        return url;
    }
}