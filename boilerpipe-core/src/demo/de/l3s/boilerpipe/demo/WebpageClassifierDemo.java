package de.l3s.boilerpipe.demo;

import java.net.URL;

import de.l3s.boilerpipe.classifier.PageType;
import de.l3s.boilerpipe.classifier.WebpageClassifier;
import de.l3s.boilerpipe.document.TextBlock;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.sax.BoilerpipeSAXInput;
import de.l3s.boilerpipe.sax.HTMLFetcher;

/**
 * Demonstrates how to use the WebpageClassifier to categorize different types of web pages.
 */
public final class WebpageClassifierDemo {
    private static final String TEST_URL = "https://en.wikipedia.org/wiki/Machine_learning";

    public static void main(String[] args) throws Exception {
        classify(TEST_URL);
    }

    private static void debugTestDocuments(String stringUrl) throws Exception {
        URL url = new URL(stringUrl);
        final BoilerpipeSAXInput input = new BoilerpipeSAXInput(HTMLFetcher.fetch(url).toInputSource());
        final TextDocument parsedDocument = input.getTextDocument();
        ArticleExtractor.INSTANCE.process(parsedDocument);
        System.out.println(parsedDocument);
    }

    private static void classify(String stringUrl) throws Exception {
        System.out.println("=== Webpage Classification Demo ===\n");
        WebpageClassifier classifier = new WebpageClassifier();
        PageType result = classifier.classify(stringUrl);
        System.out.println("List of results: ");
        System.out.println(classifier.getResults());
        System.out.println("Result is: " + result);


    }
}