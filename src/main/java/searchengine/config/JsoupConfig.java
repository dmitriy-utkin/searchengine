package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "indexing-settings.jsoup-settings")
public class JsoupConfig {
    private String userAgent;
    private String referrer;
    private boolean redirect;
    private long sleep;
    private int timeout;
    private boolean ignoreHttpErrors;
}
