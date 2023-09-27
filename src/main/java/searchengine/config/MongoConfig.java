package searchengine.config;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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
    public MongoClient mongoClient() {
        return MongoClients.create(uri);
    }

    @Override
    protected List<String> getMappingBasePackages() {
        return List.of("searchengine");
    }

    @Override
    protected boolean autoIndexCreation() {
        return true;
    }

}
