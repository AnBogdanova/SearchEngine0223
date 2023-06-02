package searchengine.web_crawling;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import searchengine.dto.NodePage;
import searchengine.models.StatusType;
import searchengine.repositories.SiteRepository;

import java.util.concurrent.ForkJoinPool;

@Component
@RequiredArgsConstructor
public class MultiThreadedWebCrawling extends Thread
{
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();

    private final NodePage nodePage;

    private final PageParser pageParser;

    private final SiteRepository siteRepository;

    @Override
    public void run() {
        forkJoinPool.execute(new RecursiveWebCrawling(pageParser, nodePage));
        var site = siteRepository.findById(nodePage.getSiteId()).orElseThrow();
        while(isAlive()) {
            if(isInterrupted()) {
                forkJoinPool.shutdown();
                site.setStatus(StatusType.FAILED).setLastError("Индексация остановлена пользователем");
                siteRepository.save(site);
            }
            if(forkJoinPool.isShutdown() && !site.getStatus().equals(StatusType.FAILED)) {
                site.setStatus(StatusType.INDEXED);
                siteRepository.save(site);
            }
        }
    }

}
