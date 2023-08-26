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
    //TODO: добавить pageRepository
    private String url;
    volatile static CopyOnWriteArraySet<String> preparedLinks = new CopyOnWriteArraySet<>();
    volatile static ConcurrentHashMap<String, String> incorrectLinks = new ConcurrentHashMap<>();
    private String rootUrl;
    private DBSite site;

    public SiteParser(SiteRepository siteRepository, String url, String rootUrl, DBSite site) {
        this.siteRepository = siteRepository;
        this.url = url;
        this.rootUrl = rootUrl;
        this.site = site;
    }

    @Override
    protected CopyOnWriteArraySet<String> compute() {
        List<SiteParser> tasks = new ArrayList<>();
        if (!IndexingServiceImpl.indexationIsRunning) {
            Thread.currentThread().interrupt();
        }
        try {
            Thread.sleep(150);
            Elements links = Jsoup.connect(url)
                        .userAgent("Chrome/4.0.249.0 Safari/532.5")
                        .referrer("http://www.google.com")
                        .timeout(10_000)
                        .ignoreHttpErrors(true)
                        .followRedirects(false)
                        .get()
                        .select("body")
                        .select("a");
            preparedLinks.add(url);
            links.forEach(link -> {
                String child = link.absUrl("href");
                if  (!incorrectLinks.containsKey(child) && isCorrectLink(child.toLowerCase(), rootUrl)) {
                    SiteParser task = new SiteParser(siteRepository, child, site.getUrl(), site);
                    log.info("List size: " + preparedLinks.size());
//                    site.setStatusTime(new Date());
//                    siteRepository.save(site);
                    task.fork();
                    tasks.add(task);
                } else {
                    incorrectLinks.put(child, "");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        tasks.forEach(ForkJoinTask::join);
        return preparedLinks;
    }

    private boolean isCorrectLink(String url, String rootUrl) {
        return !preparedLinks.contains(url) &&
                url.startsWith(rootUrl) &&
                !url.endsWith(".pdf") &&
                !url.endsWith(".png") &&
                !url.endsWith(".jpg") &&
                !url.endsWith(".jpeg") &&
                !url.endsWith(".eps") &&
                !url.endsWith(".xlsx") &&
                !url.endsWith(".xls") &&
                !url.endsWith(".doc") &&
                !url.endsWith(".docx") &&
                !url.endsWith(".ppt") &&
                !url.endsWith(".jsp") &&
                !url.endsWith(".zip") &&
                !url.endsWith(".rar") &&
                !url.contains("=") &&
                !url.contains("#") &&
                !url.contains("?");
    }
}
