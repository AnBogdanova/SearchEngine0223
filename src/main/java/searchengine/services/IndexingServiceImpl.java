package searchengine.services;

import searchengine.lemmatizer.Lemmatizer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.IndexResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import searchengine.dto.NodePage;
import searchengine.models.PageEntity;
import searchengine.models.SiteEntity;
import searchengine.models.StatusType;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.web_crawling.MultiThreadedWebCrawling;
import searchengine.web_crawling.PageParser;

@RequiredArgsConstructor
@Service
public class IndexingServiceImpl implements IndexingService
{
    private final PageParser pageParser;
    @Autowired
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    private final LemmaRepository lemmaRepository;

    private final IndexRepository indexRepository;
    private final List<Thread> threads = new ArrayList<>();
    @Autowired
    private final SitesList sites;

    @Override
    public IndexResponse startIndexing() {
        if (threads.stream().anyMatch(Thread::isAlive)) {
            return new IndexResponse().setResult(false).setError("Индексация уже запущена");
        }
        List<NodePage> nodePages = new ArrayList<>();

        var sitesList = sites.getSites();
        for (Site site : sitesList) {
            deleteAllInfoFromDataBase(site);
            createAndSaveSiteEntity(site);
            NodePage nodePage = getNodePage(site.getUrl(), site.getUrl(), "");
            nodePages.add(nodePage);
        }

        nodePages.forEach(nodePage -> threads.add(new MultiThreadedWebCrawling(nodePage, pageParser, siteRepository)));
        threads.forEach(Thread::start);
        return new IndexResponse().setResult(true);
    }

    private void deleteAllInfoFromDataBase(Site site) {
        if (siteRepository.existsByUrl(site.getUrl())) {
            var siteEntity = siteRepository.findByUrl(site.getUrl());

            indexRepository.deleteAllInBatch();
            lemmaRepository.deleteAllBySiteId(siteEntity.getId());
            pageRepository.deleteAllInfoBySiteId(siteEntity.getId());
            siteRepository.deleteById(siteEntity.getId());
        }

    }
    private void deleteAllPageInfoFromDatabase(String pagePath) throws IOException {
        Lemmatizer lemmatizer = Lemmatizer.getInstance();

        var page = pageRepository.findByPath(pagePath);

        var lemmas = lemmatizer.getLemmaSet(page.getContent());

        lemmas.forEach(lemma -> lemmaRepository.decrementAllFrequencyBySiteIdAndLemma(page.getSite().getId(), lemma));
        indexRepository.deleteAllByPageId(page.getId());
        pageRepository.delete(page);
    }

    private void createAndSaveSiteEntity(Site site) {
        siteRepository.save(new SiteEntity().setStatus(StatusType.INDEXING)
                .setStatusTime(LocalDateTime.now())
                .setLastError(null)
                .setUrl(site.getUrl())
                .setName(site.getName()));
    }


    private NodePage getNodePage(String path, String suffix, String prefix) {
        var siteEntity = siteRepository.findByUrl(suffix);
        return new NodePage().setPath(path)
                .setSuffix(suffix)
                .setPrefix(prefix)
                .setTimeBetweenRequest(150)
                .setSiteId(siteEntity.getId());
    }

    @Override
    public IndexResponse stopIndexing() {
        boolean isSitesNotIndexing = !siteRepository.existsByStatus(StatusType.INDEXING);
        if (isSitesNotIndexing) return new IndexResponse().setResult(false).setError("Индексация не запущена");

        threads.forEach(Thread::interrupt);

        return new IndexResponse().setResult(true);
    }

    @Override
    public IndexResponse indexPage(String url) throws IOException {
        String[] pathElements = url.split("/");
        String prefix = pathElements[0] + "//" + pathElements[1] + pathElements[2];
        String suffix = url.replaceAll(prefix, "");

        Site site = getSiteFromConfigOrNull(prefix);
        if (site == null) return new IndexResponse().setResult(false)
                .setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");

        if (!siteRepository.existsByUrl(site.getUrl())) {
            createAndSaveSiteEntity(site);
        }
        if (pageRepository.existsByPath(suffix)) {
            deleteAllPageInfoFromDatabase(suffix);
        }

        NodePage nodePage = getNodePage(url, prefix, suffix);
        pageParser.onePageParsing(nodePage);

        return new IndexResponse().setResult(true);
    }

    private Site getSiteFromConfigOrNull(String url) {
        return sites.getSites().stream().filter(s -> s.getUrl().equals(url)).findFirst().orElse(null);
    }

}
