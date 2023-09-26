package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.stereotype.Component;

@Configuration
@Getter
@Setter
@Component
@EnableMongoRepositories
@ConditionalOnProperty(prefix = "spring.data.mongodb", name = "search-settings.withCache", havingValue = "true")
public class MongoConfig extends AbstractMongoClientConfiguration {

    private String uri;
    private String name;

    @Override
    protected String getDatabaseName() {
        return name;
    }

    @Override
    protected boolean autoIndexCreation() {
        return true;
    }
}
