package searchengine.services;


import searchengine.model.SiteModel;

import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public interface IndexingService {
    void startIndexing(AtomicBoolean indexingEnabled);
    void refreshPage(SiteModel refreshingSite, URL urlRefreshingPage);
}
