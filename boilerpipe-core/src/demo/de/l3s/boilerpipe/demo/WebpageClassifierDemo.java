package de.l3s.boilerpipe.demo;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.lang.reflect.Type;

import de.l3s.boilerpipe.classifier.ExtractorType;
import de.l3s.boilerpipe.classifier.PageType;
import de.l3s.boilerpipe.classifier.WebpageClassifier;
import de.l3s.boilerpipe.classifier.Metrics;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.extractors.ExtractorBase;
import de.l3s.boilerpipe.sax.BoilerpipeSAXInput;

import de.l3s.boilerpipe.sax.HTMLDocument;
import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;

/**
 * Demonstrates how to use the WebpageClassifier to categorize different types of web pages.
 */
public final class WebpageClassifierDemo {
    public static final String RAW_HTML_PATH = "boilerpipe-core/src/main/resources/rawHtml.txt";
    public static final String METRICS_OUTPUT_PATH = "boilerpipe-core/src/main/resources/metricsHomePage.json";
    public static final String URL_PATH = "boilerpipe-core/src/main/resources/url.txt";
    public static final String TEST_URL;

    static {
        try {
            TEST_URL = IOUtils.toString(new FileInputStream(URL_PATH), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        classifyAndCalculate(TEST_URL);
//        debugTestDocuments(ArticleExtractor.INSTANCE);
    }

    private static void debugTestDocuments(@NotNull ExtractorBase extractor) throws Exception {
        String rawHtml = getRawHtml();
        HTMLDocument htmlDocument = new HTMLDocument(rawHtml);
        final BoilerpipeSAXInput input = new BoilerpipeSAXInput((htmlDocument).toInputSource());
        final TextDocument parsedDocument = input.getTextDocument();
        extractor.process(parsedDocument);
        System.out.println(parsedDocument);
    }

    private static String getRawHtml() throws IOException {
        FileInputStream fis = new FileInputStream(RAW_HTML_PATH);
        return IOUtils.toString(fis, StandardCharsets.UTF_8);
    }

    private static void classifyAndCalculate(String stringUrl) throws Exception {
        WebpageClassifier classifier = new WebpageClassifier();
        Map<PageType, List<ExtractorType>> result = classifier.classify(stringUrl, getRawHtml());
        System.out.println("List of results is: " + result);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Map<String, Map<ExtractorType, Metrics>> metrics = classifier.getMetrics();

        List<Map<String, Map<ExtractorType, Metrics>>> metricsWebsites;

        // Check if the file exists and is not empty
        File file = new File(METRICS_OUTPUT_PATH);
        if (file.exists() && file.length() > 0) {
            // Read existing data
            try (Reader reader = new FileReader(METRICS_OUTPUT_PATH)) {
                // Use TypeToken to handle generic types
                Type listType = new TypeToken<List<Map<String, Map<ExtractorType, Metrics>>>>(){}.getType();
                metricsWebsites = gson.fromJson(reader, listType);
                if (metricsWebsites == null) {
                    metricsWebsites = new ArrayList<>();
                }
            } catch (Exception e) {
                System.err.println("Error reading existing metrics file: " + e.getMessage());
                // If we can't read the existing file, start with an empty list
                metricsWebsites = new ArrayList<>();
            }
        } else {
            // Create a new list if the file doesn't exist or is empty
            metricsWebsites = new ArrayList<>();
        }

        // Add the new metrics to the list
        metricsWebsites.add(metrics);

        try (FileWriter writer = new FileWriter(METRICS_OUTPUT_PATH)) {
            gson.toJson(metricsWebsites, writer);
            System.out.println("Total metrics entries: " + metricsWebsites.size());
        } catch (IOException e) {
            throw new RuntimeException("Error writing metrics to file", e);
        }
    }
}