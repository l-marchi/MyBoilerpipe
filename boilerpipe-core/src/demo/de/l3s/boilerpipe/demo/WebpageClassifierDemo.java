package de.l3s.boilerpipe.demo;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import de.l3s.boilerpipe.classifier.PageType;
import de.l3s.boilerpipe.classifier.WebpageClassifier;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.extractors.ExtractorBase;
import de.l3s.boilerpipe.sax.BoilerpipeSAXInput;
import org.apache.commons.io.IOUtils;

import de.l3s.boilerpipe.sax.HTMLDocument;

/**
 * Demonstrates how to use the WebpageClassifier to categorize different types of web pages.
 */
public final class WebpageClassifierDemo {
    private static final String TEST_URL = "https://yahoo.com";
    private static final String PATH = "boilerpipe-core/src/main/resources/rawHtml.txt";

    public static void main(String[] args) throws Exception {
        classify(TEST_URL);
//        debugTestDocuments(ArticleExtractor.INSTANCE);
//        debugMetrics(TEST_URL);
    }

    private static void debugTestDocuments(ExtractorBase extractor) throws Exception {
        String rawHtml = getRawHtml();
        HTMLDocument htmlDocument = new HTMLDocument(rawHtml);
        final BoilerpipeSAXInput input = new BoilerpipeSAXInput((htmlDocument).toInputSource());
        final TextDocument parsedDocument = input.getTextDocument();
        extractor.process(parsedDocument);
        System.out.println(parsedDocument);
    }

    private static String getRawHtml() throws IOException {
        FileInputStream fis = new FileInputStream(PATH);
        return IOUtils.toString(fis, StandardCharsets.UTF_8);

    }

    private static void classify(String stringUrl) throws Exception {
        System.out.println("=== Webpage Classification Demo ===\n");
        WebpageClassifier classifier = new WebpageClassifier();
        PageType result = classifier.classify(stringUrl, getRawHtml());
        System.out.println("List of results: ");
        System.out.println(classifier.getResults());
        System.out.println("Result is: " + result);
    }

    private static void debugMetrics(String stringUrl) throws Exception {
        WebpageClassifier classifier = new WebpageClassifier();
        classifier.classify(stringUrl, getRawHtml());
        System.out.println(classifier.getMetrics());
    }
}