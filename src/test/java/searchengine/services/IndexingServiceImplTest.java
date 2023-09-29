package searchengine.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import searchengine.config.SiteConfig;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.services.indexing.IndexingServiceImpl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


class IndexingServiceImplTest {

    IndexingServiceImpl indexingServiceImpl;
    SiteConfig site;

    @BeforeEach
    void beforeEach() {
        indexingServiceImpl = new IndexingServiceImpl();
        site = new SiteConfig();
        site.setName("Test");
        site.setUrl("http://testPage.ru");
    }

    @Test
    void startIndexing() {

    }

    @Test
    @DisplayName("Get DBSite object")
    void getDBSite() {
        Site dbSiteExpected = Site.builder()
                .url("http://testPage.ru")
                .name("Test")
                .status(Status.INDEXING)
                .build();

        assertThat(dbSiteExpected.getStatus()).isEqualTo(indexingServiceImpl.getDBSite(site).getStatus());
        assertThat(dbSiteExpected.getName()).isEqualTo(indexingServiceImpl.getDBSite(site).getName());
        assertThat(dbSiteExpected.getUrl()).isEqualTo(indexingServiceImpl.getDBSite(site).getUrl());
    }

    @Test
    void launchSiteParser() {

    }
}