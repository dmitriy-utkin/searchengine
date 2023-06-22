package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.DBPage;
import searchengine.model.DBSite;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;

@Slf4j
public class SiteParserAction extends RecursiveAction {

    private PageRepository pageRepository;
    private SiteRepository siteRepository;
    private DBSite site;
    private String rootUrl;
    private String url;
    private ConcurrentHashMap<String, String> processedLinks;
    private String lastError;

    public SiteParserAction(PageRepository pageRepository,
                            SiteRepository siteRepository,
                            DBSite site,
                            String rootUrl,
                            String url) {
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.site = site;
        this.rootUrl = rootUrl;
        this.url = url;
        processedLinks = new ConcurrentHashMap<>();
    }

    @Override
    protected void compute() {
        try {

            Thread.sleep(200);
            Document doc = Jsoup.connect(url).userAgent("Chrome/4.0.249.0 Safari/532.5")
                    .referrer("").timeout(10000).get();
            String content = doc.text();
            int code = doc.connection().response().statusCode();
            pageRepository.save(createPage(site, content, code, url));
            updateSite(new Date(), lastError);
            Elements elements = doc.select("body").select("a");
            List<SiteParserAction> actions = new ArrayList<>();
            if (!elements.isEmpty()) {
                for (Element element : elements) {
                    String child = element.absUrl("href");
                    if (isCorrectUrl(child) && !processedLinks.containsKey(child)) {
                        SiteParserAction action = new SiteParserAction(pageRepository, siteRepository, site, rootUrl, child);
                        actions.add(action);
                    }
                    processedLinks.put(url, " is parsed");
                }
            }
            invokeAll(actions);

        } catch (Exception e) {
            log.error("Link: " + url + e.getMessage());
            lastError = e.getMessage();
            processedLinks.put(url, " is incorrect.");
            savePage(createPage(site, "", 404, url));
        }
    }

    private DBPage createPage(DBSite site, String content, int code, String url) {
        return DBPage.builder().dbSite(site).path(url.replace(rootUrl, ""))
                .content(content).code(code).build();
    }

    private boolean isCorrectUrl(String url) {
        return url.startsWith(rootUrl) &&
                (url.endsWith("/") || url.endsWith(".html"));
    }

    private void savePage(DBPage page) {
        pageRepository.save(page);
    }

    private void updateSite(Date statusTime, String lastError) {
        site.setStatusTime(statusTime);
        site.setLastError(lastError);
        siteRepository.save(site);
    }
}
