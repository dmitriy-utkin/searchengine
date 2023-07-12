package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.DBSite;
import searchengine.repository.SiteRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class SiteParser extends RecursiveTask<ConcurrentHashMap> {

    private SiteRepository siteRepository;
    private String url;
    private DBSite site;
    private ConcurrentHashMap<String, String> pages;
    private String rootUrl;

    public SiteParser(SiteRepository siteRepository, String url, DBSite site, ConcurrentHashMap<String, String> pages) {
        this.siteRepository = siteRepository;
        this.url = url;
        this.site = site;
        this.pages = pages;
        this.rootUrl = site.getUrl();
    }

    @Override
    protected ConcurrentHashMap<String, String> compute() {
        if (!pages.containsKey(url)) {
            try {
                Thread.sleep(100);
                Connection.Response response = Jsoup.connect(url)
                        .userAgent("Chrome/4.0.249.0 Safari/532.5")
                        .referrer("http://www.google.com")
                        .timeout(10_000)
                        .ignoreHttpErrors(true)
                        .followRedirects(false)
                        .execute();
                Document doc = response.parse();
                pages.put(url, "1");
                log.info(pages.size() + " size");
                List<SiteParser> tasks = new ArrayList<>();
                Elements elements = doc.select("a[href]");
                for (Element element : elements) {
                    String child = element.absUrl("href");
                    if (child.startsWith(rootUrl) && (child.endsWith(".html"))) {
//                        site.setStatusTime(new Date());
//                        siteRepository.save(site);
                        SiteParser task = new SiteParser(siteRepository, child, site, pages);
                        tasks.add(task);
                        task.fork();
                    }
                }

                tasks.forEach(ForkJoinTask::join);

            } catch (Exception e) {
                pages.put(url, "0");
            }
        }

        return pages;
    }
}
