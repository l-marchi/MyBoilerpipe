package de.l3s.boilerpipe.classifier;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.Image;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.extractors.*;
import de.l3s.boilerpipe.sax.BoilerpipeSAXInput;
import de.l3s.boilerpipe.sax.HTMLFetcher;
import de.l3s.boilerpipe.sax.ImageExtractor;
import org.xml.sax.SAXException;

/**
 * Webpage classifier that categorizes web pages based on Boilerpipe extracted features.
 */
public class WebpageClassifier {
    private final Map<PageType, Integer> results = new HashMap<>();
    private final List<ExtractorBase> extractors = Arrays.asList(
            ArticleExtractor.INSTANCE,
            DefaultExtractor.INSTANCE,
            CanolaExtractor.INSTANCE,
            ArticleSentencesExtractor.INSTANCE,
            KeepEverythingExtractor.INSTANCE,
            LargestContentExtractor.INSTANCE
    );

    /**
     * Classifies a webpage given its URL
     */
    public PageType classify(URL url)  {
        // TODO: 1. analysis on the URL -> add to results the PageType

        // 2. analysis to the content ad return the most common value
        extractors.forEach( extractor -> {
                try {
                    final BoilerpipeSAXInput input = new BoilerpipeSAXInput(HTMLFetcher.fetch(url).toInputSource());
                    final TextDocument doc = input.getTextDocument();
                    extractor.process(doc);
                    final ImageExtractor imageExtractor = ImageExtractor.getInstance();
                    final List<Image> images = imageExtractor.process(url, extractor);
                    PageType res = getType(doc, images);
                    //update the result map
                    results.put(
                            res,
                            results.getOrDefault(res, 0) + 1
                    );
                } catch (BoilerpipeProcessingException | IOException | SAXException e) {
                    throw new RuntimeException(e);
                }
            }
        );

        // return the most common value
        Map.Entry<PageType, Integer> maxType = null;
        for (Map.Entry<PageType, Integer> entry : results.entrySet()){
            if (maxType == null || entry.getValue() > maxType.getValue()){
                maxType = entry;
            }
        }
        return maxType.getKey();
    }

    /**
     * Classifies a webpage given its TextDocument and images
     */
    public PageType getType(TextDocument doc, List<Image> images) {
        // Calculate basic metrics
        Metrics metrics = calculateMetrics(doc, images);

        // Apply classification rules
        return classifyBasedOnMetrics(metrics);
    }

    private Metrics calculateMetrics(TextDocument doc, List<Image> images) {
        Metrics metrics = new Metrics();
        // TODO: add logic to compute metrics
        return metrics;
    }

    private PageType classifyBasedOnMetrics(Metrics metrics) {
        // TODO: add logic to classify the website base of the computed metrics
        return null;
    }


}