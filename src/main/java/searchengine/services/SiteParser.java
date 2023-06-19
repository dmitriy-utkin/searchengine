package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveTask;

@Slf4j
public class SiteParser extends RecursiveTask<ConcurrentSkipListSet> {

    private String url;

    private String rootUrl;
    private final String ATTRIBUTE_KEY = "href";
    //TODO: check if I can to delete this regex ?
//    private final String LINKS_REGEX = "https?://[a-z0-9]+-?[a-z0-9]*.[a-z0-9]{2,5}[^.#]*";
    static ConcurrentSkipListSet<String> preparedLinks = new ConcurrentSkipListSet<>();
    static ConcurrentSkipListSet<String> passedLinks = new ConcurrentSkipListSet<>();


    public SiteParser(String url, String rootUrl) {
        this.url = url;
        this.rootUrl = rootUrl;
    }

    @Override
    protected ConcurrentSkipListSet<String> compute() {

        try {
            Thread.sleep(200);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<SiteParser> tasksList = new ArrayList<>();
        Elements links = elementsCreator(documentCreator(url));
        if (isCorrectLink(url, rootUrl)) {
            preparedLinks.add(url);
        }
        try {
            if (links == null) {
                preparedLinks.add(url);
            } else {
                for (Element element : links) {
                    String childLink = element.absUrl(ATTRIBUTE_KEY);
                    if (isCorrectLink(childLink, rootUrl)) {
                        log.info("Added: " + preparedLinks.size() + " items is " + childLink);
                        preparedLinks.add(childLink);
                        SiteParser task = new SiteParser(childLink, rootUrl);
                        task.fork();
                        tasksList.add(task);
                    } else {
                        passedLinks.add(childLink);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (SiteParser task : tasksList) {
            task.join();
        }
        return preparedLinks;
    }
    //TODO: add the functional of the updating the statusTime for sites here?

    private boolean isCorrectLink(String childLink, String rootUrl) {
        return childLink.startsWith(rootUrl)
                //TODO: change the quality of checnking this links
                && !preparedLinks.contains(childLink)
                && (childLink.endsWith(".html") || childLink.endsWith("/") || childLink.endsWith("#"))
                && !passedLinks.contains(childLink)
                ;
    }


    private Document documentCreator(String url) {
        try {
            return Jsoup.connect(url).userAgent("Chrome/4.0.249.0 Safari/532.5")
                    .referrer("http://www.google.com").timeout(10000)
                    .get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Elements elementsCreator(Document document) {
        try {
            return document.select("a[href]");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
