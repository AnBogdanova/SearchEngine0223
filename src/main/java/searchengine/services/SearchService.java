package searchengine.services;

import searchengine.dto.SearchResponse;

import java.io.IOException;

public interface SearchService
{
    SearchResponse search(String query, String site, int offset, int limit) throws IOException;
}
