package de.l3s.boilerpipe.classifier;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

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
import de.l3s.boilerpipe.util.ImageDownloader;
import javax.imageio.ImageIO;

import static de.l3s.boilerpipe.classifier.Constants.*;
import static de.l3s.boilerpipe.classifier.Regex.*;


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

    private static final String IMAGE_PATH = "boilerpipe-core/src/main/resources/images";

    /**
     * Classifies a webpage given its URL
     */
    public Map<PageType, List<ExtractorType>> classify(String stringUrl, String rawHtml) throws Exception {
        // Step 1: URL Pattern Analysis
        matchPattern(stringUrl);

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
                
                // Filter images based on common file extensions
                images.removeIf(image -> !hasCommonImageExtension(image.getSrc()));

                // First, try to extract dimensions from URL parameters
                List<Image> imagesToDownload = new ArrayList<>();
                for (Image image : images) {
                    int[] urlDimensions = extractDimensionsFromUrl(image.getSrc());
                    if (urlDimensions != null) {
                        // Set dimensions from URL parameters
                        image.setWidth(urlDimensions[0]);
                        image.setHeight(urlDimensions[1]);
                        image.setArea(urlDimensions[0] * urlDimensions[1]);
                    } else {
                        // Need to download this image to get dimensions
                        imagesToDownload.add(image);
                    }
                }

                // Download only images that don't have URL dimensions
                if (!imagesToDownload.isEmpty()) {
                    List<File> downloadedImages = ImageDownloader.downloadImages(imagesToDownload, new File(IMAGE_PATH));
                    updateImageDimensions(imagesToDownload, downloadedImages);
                }

                // Classify the webpage type based on the extractor and media elements
                PageType resType = getType(doc, images, videos, extractor.getSecond());

                if (!results.containsKey(resType)) {
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
    public PageType getType(TextDocument doc, List<Image> images, List<Video> videos, ExtractorType extractor) throws IOException {
        Metrics metrics = calculateMetrics(doc, images, videos);
        metricsMap.put(extractor, metrics);
        return classifyBasedOnMetrics(metrics, extractor);
    }

    private void matchPattern(String url) {
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
        if (ARTICLE_PATTERN.matcher(url).matches()) {
            results.computeIfAbsent(PageType.ARTICLE, k -> new ArrayList<>()).add(ExtractorType.URL_MATCH);
        }
    }

    private Metrics calculateMetrics(TextDocument doc, List<Image> images, List<Video> videos) throws IOException {
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

        // Process images - count large images based on updated dimensions
        metrics.images = new ArrayList<>();
        metrics.images.addAll(images);
        
        // Count large images based on area
        for (Image image : metrics.images) {
            if (image.getArea() > LARGE_IMAGE_SIZE * LARGE_IMAGE_SIZE) {
                metrics.largeImages++;
            }
        }
        
        metrics.totalImages = metrics.images.size();

        // Process videos - only what we use
        metrics.videos = new ArrayList<>();
        for (Video video : videos) {
            if (Integer.parseInt(video.getWidth()) > 0 && Integer.parseInt(video.getHeight()) > 0) {
                metrics.videos.add(video);
            }
        }
        metrics.totalVideos = metrics.videos.size();


        // Calculate derived metrics
        metrics.calculateDerivedMetrics();

        return metrics;
    }

    private PageType classifyBasedOnMetrics(Metrics metrics, ExtractorType extractor) {
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
        for (Map.Entry<PageType, Double> entry : confidenceScores.entrySet()) {
            if (entry.getValue() > maxValue) {
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

    private double calculatePhotoGalleryScore(Metrics metrics) {
        double score = 0;
        if (metrics.images.isEmpty()) {
            return 0;
        }

        // Primary indicators - image-heavy content
        if (metrics.totalImages >= PhotoGallery.MIN_IMAGES) score += 0.3;
        if (metrics.largeImages >= PhotoGallery.MIN_LARGE_IMAGES) score += 0.2; // More large images than comics
        if (metrics.totalImages >= PhotoGallery.OPTIMAL_IMAGES) score += 0.1;
        if (metrics.imageToTextRatio >= PhotoGallery.MIN_IMAGE_TEXT_RATIO &&
                metrics.imageToTextRatio <= PhotoGallery.MAX_IMAGE_TEXT_RATIO) score += 0.2;
        if (metrics.largeImages >= PhotoGallery.MIN_LARGE_IMAGES) score += 0.1;

        // Secondary indicators - simplicity vs homepages
        if (metrics.contentBlocks <= PhotoGallery.MAX_CONTENT_BLOCKS) score += 0.1;    // Fewer blocks than homepages
        if (metrics.totalBlocks <= PhotoGallery.MAX_TOTAL_BLOCKS)
            score += 0.1;        // Simpler structure than homepages
        if (metrics.avgWordsPerContentBlock < PhotoGallery.MAX_AVG_WORDS_PER_BLOCK) score += 0.1;
        if (metrics.avgLinkDensity < PhotoGallery.MAX_LINK_DENSITY) score += 0.1;      // Less navigation than homepages
        if (metrics.contentRatio >= PhotoGallery.MIN_CONTENT_RATIO) score += 0.1;

        return Math.min(1.0, score);
    }

    private double calculateComicScore(Metrics metrics) {
        double score = 0;
        for (Image image : metrics.images) {
            if (image.getArea() >= Comic.MIN_LARGE_IMAGE_SIZE * Comic.MIN_LARGE_IMAGE_SIZE) {
                score += 0.2;
                break;
            }
        }
        if (metrics.images.isEmpty()) {
            return 0;
        }
        if (metrics.totalImages >= Comic.MIN_TOTAL_IMAGES &&
                metrics.totalImages <= Comic.MAX_TOTAL_IMAGES) score += 0.2;
        if (metrics.contentWords < Comic.MAX_CONTENT_WORDS) score += 0.2;
        if (metrics.totalBlocks < Comic.TOTAL_BLOCKS) score += 0.1;
        if (metrics.contentBlocks < Comic.MAX_CONTENT_BLOCKS) score += 0.1;
        if (metrics.avgWordsPerContentBlock < Comic.MAX_AVG_WORDS_PER_BLOCK) score += 0.1;
        if (metrics.imageToTextRatio >= Comic.MIN_IMAGE_TO_TEXT_RATIO) score += 0.1;
        if (metrics.avgLinkDensity < Comic.MAX_LINK_DENSITY) score += 0.1;
        if (metrics.largeImages / (double) metrics.totalImages >= Comic.MIN_LARGE_IMAGE_RATIO) score += 0.1;

        return Math.min(1.0, score);
    }

    private double calculateVideoPlayerScore(Metrics metrics) {
        double score = 0;

        if (!metrics.videos.isEmpty()) {
            score += 0.3;
        } else {
            return 0;
        }
        if (metrics.contentRatio <= VideoPlayer.MAX_CONTENT_RATIO) score += 0.1;
        if (metrics.contentWords < VideoPlayer.MAX_CONTENT_WORDS) score += 0.2;
        if (metrics.avgLinkDensity >= VideoPlayer.MIN_AVG_LINK_DENSITY &&
                metrics.avgLinkDensity <= VideoPlayer.MAX_AVG_LINK_DENSITY) score += 0.1;
        if (metrics.largeBlockRatio < VideoPlayer.MAX_LARGE_BLOCK_RATIO) score += 0.1;
        if (metrics.mediaToTextRatio >= VideoPlayer.MIN_MEDIA_RATIO) score += 0.2;
        if (metrics.largeContentBlocks <= VideoPlayer.MAX_LARGE_BLOCKS) score += 0.1;

        return Math.min(1.0, score);
    }

    private double calculateArticleScore(Metrics metrics) {
        double score = 0;
        if (metrics.totalWords < Article.MIN_TOTAL_WORDS) {
            return 0;
        }
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

    private double calculateForumScore(Metrics metrics) {
        double score = 0;
        if (metrics.totalWords < Forum.MIN_TOTAL_WORDS) {
            return 0;
        }
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
        if (metrics.blockSizeVariance >= Forum.MIN_BLOCK_SIZE_VARIANCE
                && metrics.blockSizeVariance <= Forum.MAX_BLOCK_SIZE_VARIANCE)
            score += 0.05; // High variance in post lengths

        //images
        if (metrics.totalImages <= Forum.MAX_IMAGES) {
            score += 0.2;
        } else {
            score -= 0.2;
        }
        return Math.min(1.0, score);
    }

    private double calculateHomepageScore(Metrics metrics) {
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

    /**
     * Checks if the image URL has a common image file extension
     *
     * @param url The image URL to check
     * @return true if the URL has a common image extension, false otherwise
     */
    private boolean hasCommonImageExtension(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        // Remove query parameters for extension checking
        String cleanUrl = url;
        int queryIndex = url.indexOf('?');
        if (queryIndex != -1) {
            cleanUrl = url.substring(0, queryIndex);
        }

        String[] commonExtensions = {".jpg", ".jpeg", ".png", ".svg", ".webp", ".bmp"};

        for (String ext : commonExtensions) {
            if (cleanUrl.toLowerCase().endsWith(ext)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Extracts width and height from URL query parameters
     *
     * @param url The image URL to parse
     * @return int array [width, height] or null if not found
     */
    private int[] extractDimensionsFromUrl(String url) {
        if (url == null || !url.contains("?")) {
            return null;
        }

        String queryString = url.substring(url.indexOf('?') + 1);
        String[] params = queryString.split("&");
        
        Integer width = null;
        Integer height = null;

        for (String param : params) {
            if (param.contains("=")) {
                String[] keyValue = param.split("=", 2);
                String key = keyValue[0].toLowerCase();
                String value = keyValue[1];

                try {
                    int intValue = Integer.parseInt(value);
                    
                    // Check for width parameters
                    if (key.equals("w") || key.equals("width")) {
                        width = intValue;
                    }
                    // Check for height parameters
                    else if (key.equals("h") || key.equals("height")) {
                        height = intValue;
                    }
                } catch (NumberFormatException e) {
                    // Skip non-numeric values
                }
            }
        }

        if (width != null && height != null) {
            return new int[]{width, height};
        }
        return null;
    }

    /**
     * Updates the dimensions of Image objects based on downloaded image files
     * Note: This assumes images and downloadedFiles have 1:1 correspondence
     *
     * @param images The list of Image objects to update
     * @param downloadedFiles The list of downloaded image files (must match images order)
     */
    private void updateImageDimensions(List<Image> images, List<File> downloadedFiles) {
        // Process all images - if download failed, file will be null
        for (int i = 0; i < images.size(); i++) {
            Image image = images.get(i);
            File file = (i < downloadedFiles.size()) ? downloadedFiles.get(i) : null;
            
            if (file != null && file.exists()) {
                try {
                    BufferedImage bufferedImage = ImageIO.read(file);
                    if (bufferedImage != null) {
                        int width = bufferedImage.getWidth();
                        int height = bufferedImage.getHeight();
                        // Update the image object with actual dimensions
                        image.setWidth(width);
                        image.setHeight(height);
                        image.setArea(width * height);
                        System.out.println("Updated dimensions: " + image.getSrc() + 
                                         " -> " + width + "x" + height + " (area: " + image.getArea() + ")");
                    } else {
                        System.err.println("Failed to read image file: " + file.getPath());
                    }
                } catch (IOException e) {
                    System.err.println("Error reading image file " + file.getPath() + ": " + e.getMessage());
                }
            } else {
                System.out.println("No downloaded file for image: " + image.getSrc() + " (using default dimensions)");
            }
        }
    }


    public Map<String, Map<ExtractorType, Metrics>> getMetrics(){
        Map<String, Map<ExtractorType, Metrics>> metrics = new HashMap<>();
        metrics.put(WebpageClassifierDemo.TEST_URL, metricsMap);
        return metrics;
    }
}