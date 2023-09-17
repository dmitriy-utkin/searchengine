package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "error-settings")
public class ErrorOptionConfig {
    private String startIndexingError;
    private String stopIndexingError;
    private String indexPageSiteIsOutOfTheListError;
    private String indexPageSiteIsInIndexingProcessError;
    private String mainPageUnavailableError;
    private String emptyQuerySearchError;
    private String badRequestError;
    private String unauthorizedError;
    private String forbiddenError;
    private String methodNotAllowedError;
    private String notFoundError;
    private String internalServerError;
}
