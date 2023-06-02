package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.LemmaDTO;
import searchengine.dto.SearchDTO;
import searchengine.dto.SearchResponse;
import searchengine.lemmatizer.Lemmatizer;
import searchengine.models.Lemma;
import searchengine.models.PageEntity;
import searchengine.models.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService
{
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SitesList sites;

    @Override
    public SearchResponse search(String query, String site, int offset, int limit) {
        if (query.isEmpty()) return new SearchResponse().setResult(false).setError("Задан пустой поисковый запрос");
        Pageable pageable = PageRequest.of(offset / limit, limit);

        var data = site.isEmpty() ? searchAllSites(query, pageable) : searchSite(query, site, pageable);
        return new SearchResponse()
                .setResult(true)
                .setCount(data.size())
                .setData(data)
                .setError("");
    }

    private List<SearchDTO> searchAllSites(String query, Pageable pageable) {
        var siteList= sites.getSites();
        List<SearchDTO> data = new ArrayList<>();
        siteList.forEach(site -> data.addAll(searchSite(query, site.getUrl(), pageable)));
        return data;
    }

    private List<SearchDTO> searchSite(String query, String siteUrl, Pageable pageable) {
        var site = siteRepository.findByUrl(siteUrl);
        var filteredLemmas = getFrequencyFilteredLemmas(query, site);

        Set<PageEntity> pages = new HashSet<>();
        for (Lemma lemma : filteredLemmas) {
            var pageEntities = pageRepository.findAllByLemmaId(lemma.getId(), pageable);
            if (pages.isEmpty()) pages.addAll(pageEntities.toList());
            pages.retainAll(pageEntities.toList());
        }

        double maxRelevance = pages.stream().map(page -> indexRepository.absoluteRelevanceByPageId(page.getId())).max(Double::compareTo).orElseThrow();
        List<SearchDTO> data = new ArrayList<>();
        for (PageEntity page : pages) {
            var content = page.getContent();
            var title = getTitleFromContent(content);
            var snippet = getSnippet(content ,filteredLemmas);
            var relativeRelevance = calculateRelativeRelevance(page.getId(), maxRelevance);

            data.add(new SearchDTO()
                    .setSite(site.getUrl())
                    .setSiteName(site.getName())
                    .setUri(page.getPath())
                    .setTitle(title)
                    .setSnippet(snippet)
                    .setRelevance(relativeRelevance));
        }

        return data;
    }


    private String getSnippet(String content, List<Lemma> queryLemmas) {
        var contentLemmas = getLemmatizer().getLemmaDTO(content);

        Map<String, Integer> snippets = new HashMap<>();
        for (Lemma lemma : queryLemmas) {
            int countMatches = 0;
            for (LemmaDTO lemmaDTO : contentLemmas) {
                if (!lemmaDTO.getNormalForm().equals(lemma.getLemma())) continue;
                countMatches++;

                String regex = "[\\s*()A-Za-zА-Яа-я-,\\d/]*";
                Pattern pattern = Pattern.compile( regex.concat(lemmaDTO.getInputForm()).concat(regex));
                Matcher matcher = pattern.matcher(content);

                while(matcher.find()) {
                    String match = matcher.group();
                    snippets.put(match.replaceAll(lemmaDTO.getInputForm(), "<b>" + lemmaDTO.getInputForm() + "<b>"), countMatches);
                }
            }
        }

        return Objects.requireNonNull(snippets.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null)).getKey();
    }

    private String getTitleFromContent(String content) {
        int beginIndex = content.indexOf("<title>");
        int endIndex = content.indexOf("</title>");
        return content.substring(beginIndex + 7, endIndex);
    }

    private double calculateRelativeRelevance(int pageId, double maxRelevance) {
        double absRelevance = indexRepository.absoluteRelevanceByPageId(pageId);
        return absRelevance / maxRelevance;
    }


    /*
    Фильтрация из запроса лемм, которые встречаются на слишком большом количестве страниц.
    Если фильтр не нашел редких лемм, возвращаем все леммы из запроса
     */
    private List<Lemma> getFrequencyFilteredLemmas(String query, SiteEntity site) {
        Double maxPercentLemmaOnPage = lemmaRepository.findMaxPercentageLemmaOnPagesBySiteId(site.getId());
        Double maxFrequencyPercentage = 0.75;
        double frequencyLimit = maxPercentLemmaOnPage * maxFrequencyPercentage;

        var lemmas = getLemmatizer().getLemmaSet(query);
        var lemmaEntityList = lemmas.stream().map(lemma -> lemmaRepository.findBySiteIdAndLemma(site.getId(), lemma).orElse(null))
                .filter(Objects::nonNull).toList();
        var filterFrequency = lemmaEntityList.stream().filter(lemma -> lemmaRepository.percentageLemmaOnPagesById(lemma.getId()) < frequencyLimit).toList();

        return filterFrequency.isEmpty() ?
                lemmaEntityList.stream().sorted(Comparator.comparing(Lemma::getFrequency)).toList()
                :
                filterFrequency.stream().sorted(Comparator.comparing(Lemma::getFrequency)).toList();
    }

    private Lemmatizer getLemmatizer() {
        try {
            return Lemmatizer.getInstance();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
