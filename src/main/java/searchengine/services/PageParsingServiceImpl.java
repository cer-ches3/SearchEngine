package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.model.StatusIndexing;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.tools.HtmlParser;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@RequiredArgsConstructor
public class PageParsingServiceImpl extends RecursiveAction {
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    @Getter
    private boolean indexingIsFinished = false;
    HtmlParser htmlParser = new HtmlParser();
    private ConcurrentSkipListSet<String> listLinks;
    
    @Override
    protected void compute() {
        for (SiteModel site : siteRepository.findAll()) {
            log.info("The site is being indexed " + site.getUrl());
            parsingPages(site);
            if (indexingIsFinished) {
                site.setStatusTime(LocalDateTime.now());
                site.setStatus(StatusIndexing.INDEXED);
                siteRepository.save(site);
                log.info("The site " + site.getUrl() + " is indexed!");
            }
        }
    }

    public void parsingPages(SiteModel site) {
        listLinks = htmlParser.getListLinks(site.getUrl());
        for (String link : listLinks) {
            PageModel newPage = new PageModel();
            try {
                newPage.setCode(htmlParser.getStatusCode(link));
                newPage.setContent(htmlParser.getContent(site.getUrl()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            newPage.setPath(link.substring(site.getUrl().length()));
            newPage.setSiteId(site.getId());
            pageRepository.save(newPage);
        }
        indexingIsFinished = true;
    }
}
