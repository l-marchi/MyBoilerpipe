package de.l3s.boilerpipe.classifier;

import java.util.*;
import java.util.regex.Pattern;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.demo.WebpageClassifierDemo;
import de.l3s.boilerpipe.document.Image;
import de.l3s.boilerpipe.document.TextBlock;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.document.Video;
import de.l3s.boilerpipe.extractors.*;

import de.l3s.boilerpipe.sax.BoilerpipeSAXInput;
import de.l3s.boilerpipe.sax.HTMLDocument;
import de.l3s.boilerpipe.sax.ImageExtractor;
import de.l3s.boilerpipe.sax.VideoParser;
import org.jetbrains.annotations.NotNull;


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
            new Pair<>(LargestContentExtractor.INSTANCE, ExtractorType.LARGEST_CONTENT),
            new Pair<>(KeepEverythingExtractor.INSTANCE, ExtractorType.KEEP_EVERYTHING)
    );

    // Global thresholds
    private static final int LARGE_IMAGE_SIZE = 300;
    private static final int WORDS_LARGE_BLOCK = 60;
    private static final int WORDS_MEDIUM_BLOCK = 15;
    private static final int WORDS_SMALL_BLOCK = 5;

    // PHOTO GALLERY - Refined thresholds to distinguish from homepages
    private static class PhotoGallery {
        private static final int MIN_IMAGES = 6;
        private static final int OPTIMAL_IMAGES = 12;
        private static final double MIN_IMAGE_TEXT_RATIO = 0.05;
        private static final double MAX_IMAGE_TEXT_RATIO = 4.0;     // (galleries are very image heavy)
        private static final int MAX_AVG_WORDS_PER_BLOCK = 25;     //  (galleries have less text)
        private static final double MAX_LINK_DENSITY = 0.3;        //  (galleries have less navigation)
        private static final double MIN_CONTENT_RATIO = 0.1;       //  (galleries can have low content ratio)
        private static final int MIN_LARGE_IMAGES = 3;             // (galleries have more large images)
        private static final int MAX_CONTENT_BLOCKS = 50;          // galleries have fewer content blocks than homepages
        private static final int MAX_TOTAL_BLOCKS = 100;
    }

    // ARTICLE - Enhanced criteria
    private static class Article {
        private static final int MIN_LARGE_CONTENT_BLOCKS = 1;
        private static final double MIN_LARGE_BLOCK_RATIO = 0.2;
        private static final double MIN_CONTENT_RATIO = 0.3;
        private static final double MAX_AVG_LINK_DENSITY = 0.2;
        private static final int MIN_LARGEST_BLOCK_WORDS = 60;
        private static final int MIN_AVG_WORDS_PER_BLOCK = 25;
        private static final double MIN_QUALITY_SCORE = 0.4;
        private static final int MIN_CONTENT_WORDS = 100;
        private static final int MIN_CONSECUTIVE_LARGE_BLOCKS = 1;
    }

    // VIDEO PLAYER - Better detection
    private static class VideoPlayer {
        private static final int MIN_VIDEOS = 1;
        private static final double MAX_CONTENT_RATIO = 0.3;
        private static final int MAX_CONTENT_WORDS = 500;
        private static final double MIN_AVG_LINK_DENSITY = 0.1;
        private static final double MAX_AVG_LINK_DENSITY = 0.7;
        private static final double MAX_LARGE_BLOCK_RATIO = 0.3;
        private static final double MIN_MEDIA_RATIO = 0.01;
        private static final int MAX_LARGE_BLOCKS = 2;
    }

    // COMIC - Enhanced detection
    private static class Comic {
        private static final int MIN_LARGE_IMAGE_SIZE = 800;
        private static final int MIN_LARGE_IMAGES = 1;
        private static final int MIN_TOTAL_IMAGES = 3;
        private static final int MAX_TOTAL_IMAGES = 15;
        private static final int MAX_CONTENT_WORDS = 250;
        private static final double MAX_AVG_WORDS_PER_BLOCK = 20;
        private static final double MIN_IMAGE_TO_TEXT_RATIO = 0.02;
        private static final double MAX_LINK_DENSITY = 0.3;
        private static final double MIN_LARGE_IMAGE_RATIO = 0.3;
    }

    // FORUM - Better identification with refined thresholds
    private static class Forum {
        private static final int MIN_SMALL_BLOCKS = 6;
        private static final int MIN_TOTAL_BLOCKS = 15;
        private static final double MIN_CONTENT_RATIO = 0.1;
        private static final double MIN_AVG_LINK_DENSITY = 0.0;
        private static final double MAX_AVG_LINK_DENSITY = 0.4;
        private static final int MAX_AVG_WORDS_PER_BLOCK = 40;
        private static final double MAX_LARGE_BLOCK_RATIO = 0.3;
        private static final int MIN_MEDIUM_BLOCKS = 3;
        private static final double MIN_BLOCK_SIZE_VARIANCE = 25.0;
        private static final int MIN_CONTENT_BLOCKS = 6;
        private static final double MAX_CONTENT_RATIO = 0.4;
        private static final int MAX_CONTENT_BLOCKS = 80;
        private static final int MAX_IMAGES = 10;
    }

    // HOMEPAGE - More precise criteria for news/portal sites
    private static class Homepage {
        private static final int MIN_CONTENT_BLOCKS = 10;           // (news sites have many blocks)
        private static final int MAX_CONTENT_BLOCKS = 300;          //  (news sites are content-heavy)
        private static final int MIN_IMAGES = 1;                    //  (some extractors miss images)
        private static final int MAX_IMAGES = 50;                  //  (news sites have many images)
        private static final double MIN_CONTENT_RATIO = 0.02;       //  (news sites have lots of boilerplate)
        private static final double MAX_CONTENT_RATIO = 0.3;        //  (news sites have lower ratios)
        private static final int MIN_SMALL_CONTENT_BLOCKS = 5;      // (news headlines/teasers)
        private static final double MIN_AVG_LINK_DENSITY = 0.1;     //  (news sites are link-heavy)
        private static final double MAX_AVG_LINK_DENSITY = 1.0;     //  (news sites have very high link density)
        private static final double MAX_LARGE_BLOCK_RATIO = 0.2;    //  (news sites have fewer large blocks)
        private static final int MIN_MEDIUM_BLOCKS = 5;             //  (news sites have many medium blocks)
        private static final int MIN_TOTAL_BLOCKS = 40;
    }
    
    // Enhanced URL Pattern Regex for different page types
    private static final Pattern VIDEO_PATTERN = Pattern.compile(
        ".*?/(?:video|videos|player|play|watch|stream|streaming|embed|live|channel|channels)(?:/[^/]+\\.[a-z0-9]+)?(?:/[^/]+)?.*$|" +
        ".*(?:youtube|video|vimeo|dailymotion|twitch|netflix|hulu|tiktok|mp4|webm|mov|avi|mkv|ogg)\\.com/.+.*",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern FORUM_PATTERN = Pattern.compile(
            ".*?/(?:forum|forums|community|board|discussion|discuss|thread|threads|topic|topics|viewtopic|post|reply|comment|comments)(?:/[^/]+\\.[a-z0-9]+)?(?:/[^/]+)?.*$|" +
                    ".*(?:reddit|stackoverflow|quora|discourse|phpbb|vbulletin|xenforo|invision)\\.com/.+.*",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern PHOTO_GALLERY_PATTERN = Pattern.compile(
    ".*?/(?:photo|photos|gallery|galleries|image|images|album|albums|slideshow|carousel|portfolio)(?:/[^/]+\\.[a-z0-9]+)?(?:/[^/]+)?.*$|" +
          ".*(?:instagram|flickr|imgur|pinterest|500px|smugmug|photobucket)\\.com/.+.*",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern COMIC_PATTERN = Pattern.compile(
            ".*?/(?:comic|comics|webcomic|manga|graphic|chapter|page)(?:/[^/]+\\.[a-z0-9]+)?(?:/[^/]+)?.*$|" +
                    ".*(?:webtoons|tapas|globalcomix|marvel|dccomics|darkhorse|imagecomics|mangaplus)\\.com/.+.*",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ARTICLE_PATTERN = Pattern.compile(
            ".*?/(?:article|articles|art|blog)(?:/[^/]+\\.[a-z0-9]+)?(?:/[^/]+)?.*$|" +
                    ".*(?:quora|medium|substack|huffpost|ezine-articles|hubpages|businessinsider|vocal\\.media)\\.com/.+.*",
            Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern HOMEPAGE_PATTERN = Pattern.compile(
            "^(?:https?://)?(?:www\\.)?(?:[^/]+\\.)+[^/]+/?$",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Classifies a webpage given its URL
     */
    public Map<PageType, List<ExtractorType>> classify(String stringUrl, String rawHtml) throws Exception {
        // Step 1: URL Pattern Analysis
        matchPattern(stringUrl);

        // process the images with Canola
//        HTMLDocument htmlDocument = new HTMLDocument(rawHtml);
//        BoilerpipeSAXInput input = new BoilerpipeSAXInput((htmlDocument).toInputSource());
//        TextDocument doc = input.getTextDocument();
//        CanolaExtractor.INSTANCE.process(doc);
//        final ImageExtractor imageExtractor = ImageExtractor.getInstance();
//        List<Image> images = imageExtractor.process(doc, rawHtml);

        // Extract videos
        List<Video> videos = VideoParser.getInstance().extractVideos(rawHtml);

        // Step 2: Content Analysis with multiple extractors
        for (Pair<ExtractorBase, ExtractorType> extractor : extractors) {
            try {
                // parse the document
                HTMLDocument htmlDocument = new HTMLDocument(rawHtml);
                BoilerpipeSAXInput input = new BoilerpipeSAXInput((htmlDocument).toInputSource());
                TextDocument doc = input.getTextDocument();
                extractor.getFirst().process(doc);

                // extract images after parse
                List<Image> images = ImageExtractor.INSTANCE.process(doc, rawHtml);

                PageType resType = getType(doc, images, videos, extractor.getSecond());

                if (!results.containsKey(resType)){
                    results.put(resType, new ArrayList<>());
                }
                results.get(resType).add(extractor.getSecond());

            } catch (BoilerpipeProcessingException e) {
                System.err.println("Warning: Extractor failed for " + extractor.getSecond() + ": " + e.getMessage());
            }
        }

        return results;
    }

    /**
     * Classifies a webpage given its TextDocument and media elements
     */
    public PageType getType(TextDocument doc, List<Image> images, List<Video> videos, ExtractorType extractor) {
        Metrics metrics = calculateMetrics(doc, images, videos);
        metricsMap.put(extractor, metrics);
        return classifyBasedOnMetrics(metrics, extractor);
    }

    private void matchPattern(String url){
        if (VIDEO_PATTERN.matcher(url).matches()) {
            results.computeIfAbsent(PageType.VIDEO_PLAYER, k -> new ArrayList<>()).add(ExtractorType.URL_MATCH);
        }
        if (FORUM_PATTERN.matcher(url).matches()) {
            results.computeIfAbsent(PageType.FORUM, k -> new ArrayList<>()).add(ExtractorType.URL_MATCH);
        }
        if (PHOTO_GALLERY_PATTERN.matcher(url).matches()) {
            results.computeIfAbsent(PageType.PHOTO_GALLERY, k -> new ArrayList<>()).add(ExtractorType.URL_MATCH);
        }
        if (COMIC_PATTERN.matcher(url).matches()) {
            results.computeIfAbsent(PageType.COMIC, k -> new ArrayList<>()).add(ExtractorType.URL_MATCH);
        }
        if (HOMEPAGE_PATTERN.matcher(url).matches()) {
            results.computeIfAbsent(PageType.HOMEPAGE, k -> new ArrayList<>()).add(ExtractorType.URL_MATCH);
        }
        if (ARTICLE_PATTERN.matcher(url).matches()){
            results.computeIfAbsent(PageType.ARTICLE, k -> new ArrayList<>()).add(ExtractorType.URL_MATCH);
        }
    }

    private @NotNull Metrics calculateMetrics(@NotNull TextDocument doc, List<Image> images, List<Video> videos) {
        Metrics metrics = new Metrics();
        metrics.totalBlocks = doc.getTextBlocks().size();
        
        // Text block processing - only calculate what we use
        List<Integer> blockSizes = new ArrayList<>();
        int consecutiveLargeCount = 0;
        int maxConsecutiveLarge = 0;
        
        // Process text blocks
        for (TextBlock block : doc.getTextBlocks()) {
            int numWords = block.getNumWords();
            double linkDensity = block.getLinkDensity();
            
            blockSizes.add(numWords);
            
            if (numWords == 0) {
                metrics.emptyBlocks++;
                consecutiveLargeCount = 0;
            } else if (numWords < WORDS_SMALL_BLOCK) {
                metrics.verySmallBlocks++;
                consecutiveLargeCount = 0;
            }
            
            if (block.isContent()) {
                metrics.contentBlocks++;
                metrics.contentWords += numWords;
                
                // Categorize content blocks by size
                if (numWords > WORDS_LARGE_BLOCK) {
                    metrics.largeContentBlocks++;
                    consecutiveLargeCount++;
                    maxConsecutiveLarge = Math.max(maxConsecutiveLarge, consecutiveLargeCount);
                } else if (numWords >= WORDS_MEDIUM_BLOCK) {
                    metrics.mediumContentBlocks++;
                    consecutiveLargeCount = 0;
                } else {
                    metrics.smallContentBlocks++;
                    consecutiveLargeCount = 0;
                }
                
                // Track the largest block
                if (numWords > metrics.largestBlockWords) {
                    metrics.largestBlockWords = numWords;
                }
                
                // Accumulate link density for average calculation
                metrics.totalLinkDensity += linkDensity;
            } else {
                consecutiveLargeCount = 0;
            }
            metrics.totalWords += numWords;
        }
        
        metrics.consecutiveLargeBlocks = maxConsecutiveLarge;
        
        // Calculate averages
        if (metrics.contentBlocks > 0) {
            metrics.avgLinkDensity = metrics.totalLinkDensity / metrics.contentBlocks;
            metrics.avgWordsPerContentBlock = (double) metrics.contentWords / metrics.contentBlocks;
        }
        
        // Calculate block size variance (used in forum/homepage scoring)
        if (blockSizes.size() > 1) {
            double meanSize = blockSizes.stream().mapToInt(Integer::intValue).average().orElse(0);
            metrics.blockSizeVariance = blockSizes.stream()
                .mapToDouble(size -> Math.pow(size - meanSize, 2))
                .average().orElse(0);
        }
        
        // Process images - only what we use
        metrics.totalImages = images.size();
        metrics.images = new ArrayList<>();
        for (Image image : images) {
            metrics.images.add(image);
            if (image.getArea() >= LARGE_IMAGE_SIZE * LARGE_IMAGE_SIZE) {
                metrics.largeImages++;
            }
        }

        // Process videos - only what we use
        metrics.totalVideos = videos.size();
        metrics.videos = new ArrayList<>();
        metrics.videos.addAll(videos);
        
        // Calculate derived metrics
        metrics.calculateDerivedMetrics();
        
        return metrics;
    }

    private PageType classifyBasedOnMetrics(@NotNull Metrics metrics, ExtractorType extractor) {
        // Early return for empty content
        if (metrics.contentBlocks == 0) {
            return PageType.UNKNOWN;
        }
        
        // Calculate classification confidence for each type
        Map<PageType, Double> confidenceScores = new HashMap<>();
        
        // PHOTO GALLERY: High image count, low text, many small blocks
        double galleryScore = calculatePhotoGalleryScore(metrics);
        confidenceScores.put(PageType.PHOTO_GALLERY, galleryScore);
        
        // COMIC: Few large images, minimal text, very low word count
        double comicScore = calculateComicScore(metrics);
        confidenceScores.put(PageType.COMIC, comicScore);
        
        // VIDEO PLAYER: Videos present, moderate content, specific patterns
        double videoScore = calculateVideoPlayerScore(metrics);
        confidenceScores.put(PageType.VIDEO_PLAYER, videoScore);
        
        // FORUM: Many small blocks, high content ratio, moderate link density, variance
        double forumScore = calculateForumScore(metrics);
        confidenceScores.put(PageType.FORUM, forumScore);
        
        // ARTICLE: Dominant large blocks, good content ratio, low link density
        double articleScore = calculateArticleScore(metrics);
        confidenceScores.put(PageType.ARTICLE, articleScore);
        
        // HOMEPAGE: Mixed content, moderate images, balanced metrics
        double homepageScore = calculateHomepageScore(metrics);
        confidenceScores.put(PageType.HOMEPAGE, homepageScore);
        
        // find the highest score
        double maxValue = -1;
        PageType result = null;
        for (Map.Entry<PageType, Double> entry : confidenceScores.entrySet()){
            if (entry.getValue() > maxValue){
                maxValue = entry.getValue();
                result = entry.getKey();
            }
        }

        // Only return classification if confidence is above the threshold
        if (maxValue > 0.6) {
            return result;
        }
        
        // Fallback classification with lower thresholds
//        return fallbackClassification(metrics);
        return PageType.UNKNOWN;
    }
    
    private double calculatePhotoGalleryScore(@NotNull Metrics metrics) {
        double score = 0;

        // Primary indicators - image-heavy content
        if (metrics.totalImages >= PhotoGallery.MIN_IMAGES) score += 0.3;
        if (metrics.totalImages >= PhotoGallery.OPTIMAL_IMAGES) score += 0.1;
        if (metrics.imageToTextRatio >= PhotoGallery.MIN_IMAGE_TEXT_RATIO && 
            metrics.imageToTextRatio <= PhotoGallery.MAX_IMAGE_TEXT_RATIO) score += 0.2;
        if (metrics.largeImages >= PhotoGallery.MIN_LARGE_IMAGES) score += 0.1;
        
        // Secondary indicators - simplicity vs homepages
        if (metrics.contentBlocks <= PhotoGallery.MAX_CONTENT_BLOCKS) score += 0.1;    // Fewer blocks than homepages
        if (metrics.totalBlocks <= PhotoGallery.MAX_TOTAL_BLOCKS) score += 0.1;        // Simpler structure than homepages
        if (metrics.avgWordsPerContentBlock < PhotoGallery.MAX_AVG_WORDS_PER_BLOCK) score += 0.1;
        if (metrics.avgLinkDensity < PhotoGallery.MAX_LINK_DENSITY) score += 0.1;      // Less navigation than homepages
        if (metrics.contentRatio >= PhotoGallery.MIN_CONTENT_RATIO) score += 0.1;
        
        return Math.min(1.0, score);
    }
    
    private double calculateComicScore(@NotNull Metrics metrics) {
        double score = 0;
        for (Image image: metrics.images){
            if (image.getArea() >= Comic.MIN_LARGE_IMAGE_SIZE * Comic.MIN_LARGE_IMAGE_SIZE){
                score += 0.3;
                break;
            }
        }
        if (metrics.totalImages >= Comic.MIN_TOTAL_IMAGES && 
            metrics.totalImages <= Comic.MAX_TOTAL_IMAGES) score += 0.2;
        if (metrics.contentWords < Comic.MAX_CONTENT_WORDS) score += 0.2;
        if (metrics.avgWordsPerContentBlock < Comic.MAX_AVG_WORDS_PER_BLOCK) score += 0.1;
        if (metrics.imageToTextRatio >= Comic.MIN_IMAGE_TO_TEXT_RATIO) score += 0.1;
        if (metrics.avgLinkDensity < Comic.MAX_LINK_DENSITY) score += 0.1;
        if (metrics.largeImages / (double) metrics.totalImages >= Comic.MIN_LARGE_IMAGE_RATIO) score += 0.1;
        
        return Math.min(1.0, score);
    }
    
    private double calculateVideoPlayerScore(@NotNull Metrics metrics) {
        double score = 0;
        
        if (metrics.totalVideos >= VideoPlayer.MIN_VIDEOS) score += 0.4;
        if (metrics.contentRatio <= VideoPlayer.MAX_CONTENT_RATIO) score += 0.1;
        if (metrics.contentWords < VideoPlayer.MAX_CONTENT_WORDS) score += 0.2;
        if (metrics.avgLinkDensity >= VideoPlayer.MIN_AVG_LINK_DENSITY && 
            metrics.avgLinkDensity <= VideoPlayer.MAX_AVG_LINK_DENSITY) score += 0.1;
        if (metrics.largeBlockRatio < VideoPlayer.MAX_LARGE_BLOCK_RATIO) score += 0.1;
        if (metrics.mediaToTextRatio >= VideoPlayer.MIN_MEDIA_RATIO) score += 0.2;
        if (metrics.largeContentBlocks <= VideoPlayer.MAX_LARGE_BLOCKS) score += 0.1;
        
        return Math.min(1.0, score);
    }

    private double calculateArticleScore(@NotNull Metrics metrics) {
        double score = 0;
        
        if (metrics.contentQualityScore >= Article.MIN_QUALITY_SCORE) score += 0.2;
        if (metrics.largeContentBlocks >= Article.MIN_LARGE_CONTENT_BLOCKS) score += 0.2;
        if (metrics.largeBlockRatio >= Article.MIN_LARGE_BLOCK_RATIO) score += 0.2;
        if (metrics.contentRatio >= Article.MIN_CONTENT_RATIO) score += 0.2;
        if (metrics.avgLinkDensity <= Article.MAX_AVG_LINK_DENSITY) score += 0.1;
        if (metrics.largestBlockWords >= Article.MIN_LARGEST_BLOCK_WORDS) score += 0.1;
        if (metrics.avgWordsPerContentBlock >= Article.MIN_AVG_WORDS_PER_BLOCK) score += 0.1;
        if (metrics.contentWords >= Article.MIN_CONTENT_WORDS) score += 0.1;
        if (metrics.consecutiveLargeBlocks >= Article.MIN_CONSECUTIVE_LARGE_BLOCKS) score += 0.1;
        
        return Math.min(1.0, score);
    }

    private double calculateForumScore(@NotNull Metrics metrics) {
        double score = 0;
        // Primary indicators for forum content structure
        if (metrics.contentBlocks >= Forum.MIN_CONTENT_BLOCKS &&
                metrics.contentBlocks <= Forum.MAX_CONTENT_BLOCKS) score += 0.15;        // Added upper limit
        if (metrics.totalBlocks >= Forum.MIN_TOTAL_BLOCKS) score += 0.1;
        // Content ratio - forums should have moderate ratios
        if (metrics.contentRatio >= Forum.MIN_CONTENT_RATIO &&
                metrics.contentRatio <= Forum.MAX_CONTENT_RATIO) score += 0.25;
        // Block structure characteristics
        if (metrics.mediumContentBlocks >= Forum.MIN_MEDIUM_BLOCKS) score += 0.1;
        if (metrics.largeBlockRatio < Forum.MAX_LARGE_BLOCK_RATIO) score += 0.1;     // Forums have fewer large blocks
        // Forum-specific text patterns
        if (metrics.avgLinkDensity >= Forum.MIN_AVG_LINK_DENSITY &&
                metrics.avgLinkDensity <= Forum.MAX_AVG_LINK_DENSITY) score += 0.1;
        if (metrics.avgWordsPerContentBlock < Forum.MAX_AVG_WORDS_PER_BLOCK) score += 0.05;
        if (metrics.blockSizeVariance >= Forum.MIN_BLOCK_SIZE_VARIANCE) score += 0.05; // High variance in post lengths

        //images
        if (metrics.totalImages <= Forum.MAX_IMAGES){
            score += 0.2;
        }else {
            score -= 0.2;
        }
        return Math.min(1.0, score);
    }

    private double calculateHomepageScore(@NotNull Metrics metrics) {
        double score = 0;
        
        // Primary indicators for news/portal homepages
        if (metrics.contentBlocks >= Homepage.MIN_CONTENT_BLOCKS && 
            metrics.contentBlocks <= Homepage.MAX_CONTENT_BLOCKS) score += 0.3;
        if (metrics.totalBlocks >= Homepage.MIN_TOTAL_BLOCKS) score += 0.2;
        if (metrics.contentRatio >= Homepage.MIN_CONTENT_RATIO && 
            metrics.contentRatio <= Homepage.MAX_CONTENT_RATIO) score += 0.2;
        
        // Secondary indicators
        if (metrics.totalImages >= Homepage.MIN_IMAGES && 
            metrics.totalImages <= Homepage.MAX_IMAGES) score += 0.1;
        if (metrics.smallContentBlocks >= Homepage.MIN_SMALL_CONTENT_BLOCKS) score += 0.1;
        if (metrics.mediumContentBlocks >= Homepage.MIN_MEDIUM_BLOCKS) score += 0.1;
        if (metrics.avgLinkDensity >= Homepage.MIN_AVG_LINK_DENSITY && 
            metrics.avgLinkDensity <= Homepage.MAX_AVG_LINK_DENSITY) score += 0.1;
        if (metrics.largeBlockRatio <= Homepage.MAX_LARGE_BLOCK_RATIO) score += 0.1;
        
        return Math.min(1.0, score);
    }
    
    private PageType fallbackClassification(Metrics metrics) {
        System.out.println("FALLBACK");
        
        // forum detection
        if (metrics.smallContentBlocks >= 8 &&                      // More small blocks required
            metrics.contentRatio >= 0.1 && metrics.contentRatio <= 0.35 && // Moderate content ratio
            metrics.totalBlocks >= 20 &&                            // More total blocks
            metrics.avgLinkDensity <= 0.4 &&                        // Not too link-heavy
            metrics.blockSizeVariance >= 30.0 &&                    // High variance in post lengths
            metrics.contentBlocks <= 100                            // Not too many content blocks (homepage)
        ) {
            System.out.println("FORUM");
            return PageType.FORUM;
        }
        
        // Additional specific forum pattern
        if (metrics.smallContentBlocks >= 10 &&                     // Many small blocks
            metrics.mediumContentBlocks >= 4 &&                     // Some medium blocks
            metrics.largeBlockRatio < 0.25 &&                       // Few large blocks
            metrics.avgWordsPerContentBlock <= 35 &&                // Short posts
            metrics.contentBlocks >= 8 && metrics.contentBlocks <= 80 // Reasonable content block count
        ) {
            System.out.println("FORUM");
            return PageType.FORUM;
        }
        
        // Gallery with more specific thresholds to avoid homepage confusion
        if (metrics.totalImages >= 8 &&                            // Substantial images
            metrics.imageToTextRatio >= 0.05 &&                    // High image-to-text ratio
            metrics.contentBlocks <= 50 &&                         // Fewer blocks than homepages
            metrics.avgLinkDensity < 0.3                           // Less navigation than homepages
        ) {
            System.out.println("GALLERY");
            return PageType.PHOTO_GALLERY;
        }
        
        // Strong article indicators
        if (metrics.contentRatio > 0.5 && 
            metrics.largestBlockWords > 100 &&
            metrics.avgLinkDensity < 0.15) {
            return PageType.ARTICLE;
        }
        
        // Strong homepage indicators
        if (metrics.contentBlocks >= 20 &&                          // Many content blocks
            metrics.totalBlocks >= 50 &&                            // Many total blocks
            metrics.contentRatio >= 0.05 && metrics.contentRatio <= 0.3 &&  // Low content ratio due to boilerplate
            metrics.smallContentBlocks >= 10) {                     // Many small blocks (headlines)
            System.out.println("HOMEPAGE");
            return PageType.HOMEPAGE;
        }
        
        // Alternative homepage detection for high link density sites
        if (metrics.contentBlocks >= 15 &&
            metrics.avgLinkDensity >= 0.4 &&                        // High link density (news sites)
            metrics.mediumContentBlocks >= 5 &&                     // Mix of content sizes
            metrics.totalImages >= 1) {                             // Some images
            System.out.println("HOMEPAGE");
            return PageType.HOMEPAGE;
        }

        
        // Default to ARTICLE for significant content
        if (metrics.contentWords > 100 && metrics.contentRatio > 0.2) {
            System.out.println("ARTICLE");
            return PageType.ARTICLE;
        }
        
        return PageType.UNKNOWN;
    }

    public Map<String, Map<ExtractorType, Metrics>> getMetrics(){
        Map<String, Map<ExtractorType, Metrics>> metrics = new HashMap<>();
        metrics.put(WebpageClassifierDemo.TEST_URL, metricsMap);
        return metrics;
    }
}