package searchengine.services;

import searchengine.dto.IndexResponse;


import java.io.IOException;


public interface IndexingService
{
    IndexResponse startIndexing() throws IOException, InterruptedException;

    IndexResponse stopIndexing() throws InterruptedException;

    IndexResponse indexPage(String url) throws InterruptedException, IOException;
}
