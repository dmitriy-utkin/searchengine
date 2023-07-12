package searchengine.dto.indexing;

import searchengine.model.Status;

import java.util.Date;

public record SiteDTO(Status status, Date statusTime, String lastError, String url, String name) {
}
