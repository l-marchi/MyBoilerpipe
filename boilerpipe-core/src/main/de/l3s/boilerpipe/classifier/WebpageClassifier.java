package de.l3s.boilerpipe.classifier;

import java.util.*;
import java.util.regex.Pattern;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.demo.WebpageClassifierDemo;
import de.l3s.boilerpipe.document.Image;
import de.l3s.boilerpipe.document.TextBlock;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.extractors.*;
import de.l3s.boilerpipe.sax.BoilerpipeSAXInput;
import de.l3s.boilerpipe.sax.HTMLDocument;
import de.l3s.boilerpipe.sax.ImageExtractor;


/**
 * Webpage classifier that categorizes web pages based on Boilerpipe extracted features.
 */
public class WebpageClassifier {
    private final Map<PageType, List<ExtractorType>> results = new HashMap<>();
    private final Map<ExtractorType, Metrics> metricsMap = new HashMap<>();

    private final List<Pair<ExtractorBase, ExtractorType>> extractors = Arrays.asList(
            new Pair<>(ArticleExtractor.INSTANCE, ExtractorType.ARTICLE),
            new Pair<>(DefaultExtractor.INSTANCE, ExtractorType.DEFAULT),
            new Pair<>(CanolaExtractor.INSTANCE, ExtractorType.CANOLA),
            new Pair<>(ArticleSentencesExtractor.INSTANCE, ExtractorType.ARTICLE_SENTENCES),
            new Pair<>(KeepEverythingExtractor.INSTANCE, ExtractorType.KEEP_EVERYTHING),
            new Pair<>(LargestContentExtractor.INSTANCE, ExtractorType.LARGEST_CONTENT)
    );

    private static final int LARGE_IMAGE_SIZE = 250;
    private static final double HIGH_LINK_DENSITY = 0.5;
    private static final int WORDS_LARGE_BLOCK = 75;
    private static final int WORDS_MEDIUM_BLOCK = 20;

    //PHOTO GALLERY
    private static class PhotoGallery{
        private static final int NUM_IMAGES = 8;
        private static final double IMAGE_TEXT_RATIO = 0.03;
        private static final int AVG_WORDS_PER_BLOCK = 40;
        private static final double MAX_LINK_DENSITY = 0.6;
    }

    private static class Article{
        private static final int LARGE_CONTENT_BLOCKS = 1;
        private static final double LARGE_BLOCK_RATIO = 0.25;
        private static final double CONTENT_RATIO = 0.25;
        private static final double AVG_LINK_DENSITY = 0.25;
        private static final int LARGEST_BLOCK_WORDS = 80;
        private static final int MIN_AVG_WORDS_PER_BLOCK = 30;
    }

    private static class Homepage{
        private static final int CONTENT_BLOCKS = 3;
        private static final int MIN_IMAGES = 1;
        private static final int MAX_IMAGES = 15;
        private static final double MIN_CONTENT_RATIO = 0.2;
        private static final double MAX_CONTENT_RATIO = 0.7;
        private static final int SMALL_CONTENT_BLOCKS = 2;
        private static final double MIN_AVG_LINK_DENSITY = 0.03;
        private static final double MAX_AVG_LINK_DENSITY = 0.5;
    }

    private static class VideoPlayer{
        private static final int NUM_IMAGES = 2;
        private static final int MAX_CONTENT_WORDS = 400;
        private static final double AVG_LINK_DENSITY = 0.25;
        private static final double LARGE_BLOCK_RATIO = 0.4;
    }

    private static class Comic{
        private static final int LARGE_IMAGES = 1;
        private static final int TOTAL_IMAGES = 6;
        private static final int CONTENT_WORDS = 300;
        private static final double MAX_AVG_WORDS_PER_BLOCK = 25;
        private static final double IMAGE_TO_TEXT_RATIO = 0.01;
    }

    private static class Forum{
        private static final int MIN_SMALL_BLOCKS = 4;
        private static final double CONTENT_RATIO = 0.35;
        private static final double MIN_AVG_LINK_DENSITY = 0.08;
        private static final double MAX_AVG_LINK_DENSITY = 0.6;
        private static final int AVG_WORDS_PER_BLOCK = 60;
        private static final double LARGE_BLOCK_RATIO = 0.5;
    }
    
    // URL Pattern Regex for different page types
    private static final Pattern VIDEO_PATTERN = Pattern.compile(
        ".*(video|player|play|watch|stream|streaming|embed|youtube|vimeo|" +
        "mp4|webm|mov|avi|mkv|ogg|dailymotion|twitch|netflix|hulu).*"
    );
    
    private static final Pattern FORUM_PATTERN = Pattern.compile(
        ".*(forum|forums|community|board|discussion|discuss|thread|topic|" +
        "viewtopic|post|reddit|stackoverflow|quora|discourse).*"
    );
    
    private static final Pattern PHOTO_GALLERY_PATTERN = Pattern.compile(
        ".*(photo|photos|gallery|galleries|image|images|album|albums|" +
        "slideshow|carousel|instagram|flickr|imgur|pinterest).*"
    );
    
    private static final Pattern COMIC_PATTERN = Pattern.compile(
        ".*(comic|comics|webcomic|manga|graphic-novel|manhwa|webtoons|tapas|" +
        "globalcomix|marvel|dccomics|darkhorse|imagecomics).*"
    );
    
    private static final Pattern HOMEPAGE_PATTERN = Pattern.compile(
        ".*/(index\\.html?|home\\.html?|default\\.html?)?$|"
    );

    /**
     * Classifies a webpage given its URL
     */
    public Map<PageType, List<ExtractorType>> classify(String stringUrl, String rawHtml) throws Exception {
        // Step 1: URL Pattern Analysis
        matchPattern(stringUrl);

        // Step 2: Content Analysis with multiple extractors
        // Process with each extractor using the same HTML input
        for (Pair<ExtractorBase, ExtractorType> extractor : extractors) {
            try {
                HTMLDocument htmlDocument = new HTMLDocument(rawHtml);
                final BoilerpipeSAXInput input = new BoilerpipeSAXInput((htmlDocument).toInputSource());
                TextDocument doc = input.getTextDocument();
                extractor.getFirst().process(doc);
                final ImageExtractor imageExtractor = ImageExtractor.getInstance();
                List<Image> images = imageExtractor.process(doc, rawHtml);
                PageType resType = getType(doc, images, extractor.getSecond());

                if (!results.containsKey(resType)){
                    results.put(
                        resType,
                        new ArrayList<>()
                    );
                }
                results.get(resType).add(extractor.getSecond());

            } catch (BoilerpipeProcessingException e) {
                // Continue with other extractors if one fails
                System.err.println("Warning: Extractor failed for " + extractor.getSecond() + ": " + e.getMessage());
            }
        }

        // Return the most common classification -> TODO
        return results;
    }

    private PageType computeResult(){
        return null;
    }

    /**
     * Classifies a webpage given its TextDocument and images
     */
    public PageType getType(TextDocument doc, List<Image> images, ExtractorType extractor) {
        // Calculate basic metrics
        Metrics metrics = calculateMetrics(doc, images);
        metricsMap.put(
            extractor,
            metrics
        );

        // Apply classification rules
        return classifyBasedOnMetrics(metrics);
    }

    private void matchPattern(String url){
        if (VIDEO_PATTERN.matcher(url).matches()) {
            results.put(
                    PageType.VIDEO_PLAYER,
                    new ArrayList<>()
            );
            results.get(PageType.VIDEO_PLAYER).add(ExtractorType.URL_MATCH);
        }
        if (FORUM_PATTERN.matcher(url).matches()) {
            results.put(
                    PageType.FORUM,
                    new ArrayList<>()
            );
            results.get(PageType.FORUM).add(ExtractorType.URL_MATCH);
        }
        if (PHOTO_GALLERY_PATTERN.matcher(url).matches()) {
            results.put(
                    PageType.PHOTO_GALLERY,
                    new ArrayList<>()
            );
            results.get(PageType.PHOTO_GALLERY).add(ExtractorType.URL_MATCH);
        }
        if (COMIC_PATTERN.matcher(url).matches()) {
            results.put(
                    PageType.COMIC,
                    new ArrayList<>()
            );
            results.get(PageType.COMIC).add(ExtractorType.URL_MATCH);
        }
        if (HOMEPAGE_PATTERN.matcher(url).matches()) {
            results.put(
                    PageType.HOMEPAGE,
                    new ArrayList<>()
            );
            results.get(PageType.HOMEPAGE).add(ExtractorType.URL_MATCH);
        }

    }

    private Metrics calculateMetrics(TextDocument doc, List<Image> images) {
        Metrics metrics = new Metrics();
        metrics.totalBlocks = doc.getTextBlocks().size();
        // Process text blocks
        for (TextBlock block : doc.getTextBlocks()) {
            int numWords = block.getNumWords();

            // Count high link density blocks
            if (block.getLinkDensity() > HIGH_LINK_DENSITY) {
                metrics.highLinkDensityBlocks++;
            }
            
            if (block.isContent()) {
                metrics.contentBlocks++;
                metrics.contentWords += numWords;
                
                // Categorize content blocks by size
                if (numWords > WORDS_LARGE_BLOCK) {
                    metrics.largeContentBlocks++;
                } else if (numWords >= WORDS_MEDIUM_BLOCK) {
                    metrics.mediumContentBlocks++;
                } else {
                    metrics.smallContentBlocks++;
                }
                
                // Track the largest block
                if (numWords > metrics.largestBlockWords) {
                    metrics.largestBlockWords = numWords;
                }
                
                // Accumulate density metrics
                metrics.totalLinkDensity += block.getLinkDensity();
                metrics.totalTextDensity += block.getTextDensity();
            } else {
                metrics.boilerplateBlocks++;
            }
            metrics.totalWords += numWords;
        }
        
        // Calculate averages
        if (metrics.contentBlocks > 0) {
            metrics.avgLinkDensity = metrics.totalLinkDensity / metrics.contentBlocks;
            metrics.avgTextDensity = metrics.totalTextDensity / metrics.contentBlocks;
            metrics.avgWordsPerContentBlock = (double) metrics.contentWords / metrics.contentBlocks;
        }
        
        // Process images
        metrics.totalImages = images.size();
        for (Image image : images) {
            if (image.getArea() >= LARGE_IMAGE_SIZE * LARGE_IMAGE_SIZE) {
                metrics.largeImages++;
            }
            if (image.getArea() > 0) {
                metrics.totalImageArea += image.getArea();
            }
        }
        
        // Calculate derived metrics
        metrics.calculateDerivedMetrics();
        
        return metrics;
    }

    private PageType classifyBasedOnMetrics(Metrics metrics) {
        // Early return for empty content
        if (metrics.contentBlocks == 0) {
            return PageType.UNKNOWN;
        }
        
        // PHOTO GALLERY: High image count, low text, many small blocks
        if (metrics.totalImages >= PhotoGallery.NUM_IMAGES &&
            metrics.imageToTextRatio > PhotoGallery.IMAGE_TEXT_RATIO &&
            metrics.avgWordsPerContentBlock < PhotoGallery.AVG_WORDS_PER_BLOCK &&
            metrics.smallContentBlocks > metrics.largeContentBlocks &&
            metrics.avgLinkDensity < PhotoGallery.MAX_LINK_DENSITY) {
            return PageType.PHOTO_GALLERY;
        }
        
        // COMIC: Few large images, minimal text, very low word count
        if (metrics.largeImages >= Comic.LARGE_IMAGES &&
            metrics.totalImages <= Comic.TOTAL_IMAGES &&
            metrics.contentWords < Comic.CONTENT_WORDS &&
            metrics.avgWordsPerContentBlock < Comic.MAX_AVG_WORDS_PER_BLOCK &&
            metrics.imageToTextRatio > Comic.IMAGE_TO_TEXT_RATIO) {
            return PageType.COMIC;
        }
        
        // VIDEO PLAYER: Moderate images, low content, high link density
        if (metrics.totalImages >= VideoPlayer.NUM_IMAGES &&
            metrics.contentWords < VideoPlayer.MAX_CONTENT_WORDS &&
            (metrics.avgLinkDensity > VideoPlayer.AVG_LINK_DENSITY || metrics.highLinkDensityBlocks > 0) &&
            metrics.smallContentBlocks >= metrics.mediumContentBlocks &&
            metrics.largeBlockRatio < VideoPlayer.LARGE_BLOCK_RATIO) {
            return PageType.VIDEO_PLAYER;
        }
        
        // FORUM: Many small blocks, high content ratio, moderate link density
        if (metrics.smallContentBlocks >= Forum.MIN_SMALL_BLOCKS &&
            metrics.contentRatio > Forum.CONTENT_RATIO &&
            metrics.avgLinkDensity > Forum.MIN_AVG_LINK_DENSITY && metrics.avgLinkDensity < Forum.MAX_AVG_LINK_DENSITY &&
            metrics.avgWordsPerContentBlock < Forum.AVG_WORDS_PER_BLOCK &&
            metrics.largeBlockRatio < Forum.LARGE_BLOCK_RATIO
        ) {
            return PageType.FORUM;
        }
        
        // ARTICLE: Dominant large blocks, good content ratio, low link density
        if (metrics.largeContentBlocks >= Article.LARGE_CONTENT_BLOCKS &&
            metrics.largeBlockRatio > Article.LARGE_BLOCK_RATIO &&
            metrics.contentRatio > Article.CONTENT_RATIO &&
            metrics.avgLinkDensity < Article.AVG_LINK_DENSITY &&
            metrics.largestBlockWords > Article.LARGEST_BLOCK_WORDS &&
            metrics.avgWordsPerContentBlock > Article.MIN_AVG_WORDS_PER_BLOCK) {
            return PageType.ARTICLE;
        }
        
        // HOMEPAGE: Mixed content, moderate images, balanced metrics
        if (metrics.contentBlocks >= Homepage.CONTENT_BLOCKS &&
            metrics.totalImages >= Homepage.MIN_IMAGES && metrics.totalImages <= Homepage.MAX_IMAGES &&
            metrics.contentRatio > Homepage.MIN_CONTENT_RATIO && metrics.contentRatio < Homepage.MAX_CONTENT_RATIO &&
            (metrics.mediumContentBlocks >= metrics.largeContentBlocks || 
             metrics.smallContentBlocks > Homepage.SMALL_CONTENT_BLOCKS) &&
            metrics.avgLinkDensity > Homepage.MIN_AVG_LINK_DENSITY && metrics.avgLinkDensity < Homepage.MAX_AVG_LINK_DENSITY) {
            return PageType.HOMEPAGE;
        }
        
        // Additional fallback rules based on specific characteristics
        
        // Strong article indicators
        if (metrics.contentRatio > 0.6 && 
            metrics.largestBlockWords > 100 &&
            metrics.avgLinkDensity < 0.15) {
            return PageType.ARTICLE;
        }
        
        // Strong homepage indicators  
        if (metrics.contentBlocks > 5 &&
            metrics.contentRatio > 0.3 && metrics.contentRatio < 0.5 &&
            metrics.totalImages > 3) {
            return PageType.HOMEPAGE;
        }
        
        // Weak content, likely unknown or special page
        if (metrics.contentWords < 50 && metrics.totalImages < 2) {
            return PageType.UNKNOWN;
        }
        
        // Default to ARTICLE for substantial content with reasonable structure
        if (metrics.contentWords > 150 && metrics.contentRatio > 0.2) {
            return PageType.ARTICLE;
        }
        
        return PageType.UNKNOWN;
    }

    public Map<String, Map<ExtractorType, Metrics>> getMetrics(){
        Map<String, Map<ExtractorType, Metrics>> metrics = new HashMap<>();
        metrics.put(WebpageClassifierDemo.TEST_URL, metricsMap);
        return  metrics;
    }
}