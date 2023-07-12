package searchengine.dto.indexing;

import searchengine.model.DBSite;

public record PageDTO(String path, DBSite site, int code, String content) {
}
