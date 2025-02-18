package searchengine.services;

import searchengine.model.PageModel;

public interface PageIndexerService {
    void indexHtml(String html, PageModel indexingPage);
    void refreshLemmaAndIndex(PageModel indexingPage);
}