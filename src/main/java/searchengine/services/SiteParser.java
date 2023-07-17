package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import searchengine.model.DBSite;
import searchengine.repository.SiteRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class SiteParser extends RecursiveTask<CopyOnWriteArraySet> {

    private SiteRepository siteRepository;
    private String url;
    static CopyOnWriteArraySet<String> preparedLinks = new CopyOnWriteArraySet<>();
    private String rootUrl;
    private DBSite site;

    public SiteParser(SiteRepository siteRepository, String url, String rootUrl, DBSite site) {
        this.siteRepository = siteRepository;
        this.url = url;
        this.rootUrl = rootUrl;
        this.site = site;
//        site.setStatusTime(new Date());
//        siteRepository.save(site);
    }

    @Override
    protected CopyOnWriteArraySet<String> compute() {
        List<SiteParser> tasks = new ArrayList<>();
        try {
            Thread.sleep(100);
            preparedLinks.add(url);
            Elements links = Jsoup.connect(url)
                        .userAgent("Chrome/4.0.249.0 Safari/532.5")
                        .referrer("http://www.google.com")
                        .timeout(10_000)
                        .ignoreHttpErrors(true)
                        .followRedirects(false)
                        .get()
                        .select("body")
                        .select("a");
            links.forEach(link -> {
                String child = link.absUrl("href");
                if  (isCorrectLink(child, rootUrl)) {
                    SiteParser task = new SiteParser(siteRepository, child, site.getUrl(), site);
                    log.info("Size: " + preparedLinks.size());
                    task.fork();
                    tasks.add(task);
                }
            });
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        tasks.forEach(ForkJoinTask::join);
        return preparedLinks;
    }

    private boolean isCorrectLink(String url, String rootUrl) {
        return !preparedLinks.contains(url) &&
                !url.contains("?") &&
                url.startsWith(rootUrl) &&
                (url.endsWith(".html") || url.endsWith("/"));
    }

}
