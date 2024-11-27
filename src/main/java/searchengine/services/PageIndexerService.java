package searchengine.services;

import searchengine.model.PageModel;

import java.net.URL;

public interface PageIndexerService {
    void indexHtml(String html, PageModel indexingPage);
    void refreshLemmaAndIndex(PageModel indexingPage);
}