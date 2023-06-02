package searchengine.web_crawling;

import searchengine.lemmatizer.Lemmatizer;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import org.jsoup.Jsoup;
import searchengine.dto.NodePage;
import searchengine.models.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PageParser
{
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;

    private final LemmaRepository lemmaRepository;

    private final IndexRepository indexRepository;

    public synchronized void startPageParser(NodePage nodePage) {
        var siteEntity = siteRepository.findById(nodePage.getSiteId()).orElseThrow();
        updateStatusTime(siteEntity);

        try{
            Document document = Jsoup.connect(nodePage.getPath()).get();
            var elements = document.select("a[href]");
            List<PageEntity> pages = new ArrayList<>();
            for (Element element : elements) {
                var subPath = element.attr("abs:href");
                var page = getPage(subPath, siteEntity, document);
                var refOnChildSet = nodePage.getRefOnChildSet();
                if(isNeedToSave(page.getPath())) {
                    pages.add(page);
                    refOnChildSet.add(subPath);
                }
            }
            pageRepository.saveAll(pages);

        } catch (IOException e) {
            setStatusFailedAndErrorMessage(siteEntity, e.toString());
            throw new RuntimeException(e.getMessage());
        }
    }

    private boolean isNeedToSave(String path) {
        return !pageRepository.existsByPath(path)
                && path.startsWith("/")
                && !path.matches("(\\S+(\\.(?i)(jpg|png|gif|bmp|pdf|xml))$)")
                && !path.contains("#");
    }

    private synchronized void saveLemmaAndIndex(List<PageEntity> pageEntities) throws IOException {
        Lemmatizer lemmatizer = Lemmatizer.getInstance();

        List<Lemma> lemmaEntities = new ArrayList<>();
        List<SearchIndex> searchIndex = new ArrayList<>();

        for (PageEntity pageEntity : pageEntities) {
            int siteId = pageEntity.getSite().getId();

            Map<String, Integer> lemmas = lemmatizer.collectLemmas(pageEntity.getContent());
            for (Map.Entry<String, Integer> word : lemmas.entrySet()) {
                var lemma = word.getKey();

                Lemma lemmaEntity;
                if (lemmaRepository.existsBySiteIdAndLemma(siteId, lemma)) {
                    lemmaEntity = lemmaRepository.findBySiteIdAndLemma(siteId, lemma).orElseThrow();
                    lemmaEntity.setFrequency(Math.incrementExact(lemmaEntity.getFrequency()));
                    lemmaEntities.add(lemmaEntity);
                } else {
                    lemmaEntity = new Lemma()
                            .setSite(pageEntity.getSite())
                            .setLemma(lemma)
                            .setFrequency(1);
                    lemmaEntities.add(lemmaEntity);
                }

                if (lemmaEntities.size() >= 100) {
                    lemmaRepository.saveAll(lemmaEntities);
                    lemmaEntities.clear();
                }

                searchIndex.add(new SearchIndex()
                        .setPage(pageEntity)
                        .setLemma(lemmaEntity)
                        .setRank(word.getValue()));
                if (searchIndex.size() >= 5000) {
                    indexRepository.saveAll(searchIndex);
                    searchIndex.clear();
                }
            }
        }
        lemmaRepository.saveAll(lemmaEntities);
        indexRepository.saveAll(searchIndex);
    }

    private PageEntity getPage(String path, SiteEntity site, Document document) {
        return new PageEntity().setSite(site).setPath(path.replaceAll(site.getUrl(), ""))
                .setCode(document.connection().response().statusCode())
                .setContent(document.html());
    }

    public void onePageParsing(NodePage nodePage) {
        var siteEntity = siteRepository.findById(nodePage.getSiteId()).orElseThrow();
        siteRepository.save(siteEntity.setStatus(StatusType.INDEXING));
        updateStatusTime(siteEntity);
        try {
            Document document = Jsoup.connect(nodePage.getPath()).get();
            var page = getPage(nodePage.getPath(), siteEntity, document);
            pageRepository.save(page);
            saveLemmaAndIndex(List.of(page));

        } catch (IOException e) {
            setStatusFailedAndErrorMessage(siteEntity, e.toString());
            throw new RuntimeException(e.getMessage());
        }

        siteRepository.save(siteEntity.setStatus(StatusType.INDEXED));
    }

    private void updateStatusTime(SiteEntity siteEntity) {
        if (siteEntity != null) siteRepository.save(siteEntity.setStatusTime(LocalDateTime.now()));
    }

    private void setStatusFailedAndErrorMessage(SiteEntity siteEntity, String error) {
        if (siteEntity != null) siteRepository.save(siteEntity.setStatus(StatusType.FAILED).setLastError(error));
    }



}
