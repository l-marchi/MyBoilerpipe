package de.l3s.boilerpipe.classifier;

import de.l3s.boilerpipe.document.Image;

import java.util.Collections;
import java.util.List;

public class Metrics {

    // Content metrics
    int totalBlocks = 0;
    int contentBlocks = 0;
    int totalWords = 0;
    int contentWords = 0;

    // Block size categorization
    int largeContentBlocks = 0;  // >60 words
    int mediumContentBlocks = 0; // 15-60 words
    int smallContentBlocks = 0;  // 1-15 words
    int largestBlockWords = 0;

    // Density metrics
    double totalLinkDensity = 0;    // to calculate avgLinkDensity
    double avgLinkDensity = 0;      //  in scoring methods
    double avgWordsPerContentBlock = 0;  // in scoring methods

    // Image metrics
    int totalImages = 0;            // in scoring
    int largeImages = 0;            // in comic/gallery scoring
    List<Image> images;

    // Video metrics
    int totalVideos = 0;            // in video scoring

    // Structural metrics
    int consecutiveLargeBlocks = 0;     // in article scoring
    int verySmallBlocks = 0;            // in contentQualityScore
    int emptyBlocks = 0;                // in contentQualityScore
    
    // Content quality metrics
    double blockSizeVariance = 0;       // in forum/homepage scoring
    double contentQualityScore = 0;     // in article scoring

    // Ratios
    double contentRatio = 0;            // in all scoring methods
    double imageToTextRatio = 0;        // in gallery/comic scoring
    double largeBlockRatio = 0;         // in video/forum/article/homepage scoring
    double mediaToTextRatio = 0;        // in video scoring
    
    /**
     * Helper method to calculate derived ratios after basic metrics is computed
     */
    public void calculateDerivedMetrics() {
        contentRatio = totalBlocks > 0 ? (double) contentBlocks / totalBlocks : 0;
        imageToTextRatio = contentWords > 0 ? (double) totalImages / contentWords : 0;
        largeBlockRatio = contentBlocks > 0 ? (double) largeContentBlocks / contentBlocks : 0;
        mediaToTextRatio = contentWords > 0 ? (double) (totalImages + totalVideos) / contentWords : 0;
                                
        // Calculate content quality score
        calculateContentQualityScore();
    }
    
    /**
     * Calculate content quality score based on multiple factors
     */
    private void calculateContentQualityScore() {
        double score = 0;
        
        // Positive factors
        if (largeContentBlocks > 0) score += 0.3;
        if (avgWordsPerContentBlock > 30) score += 0.2;
        if (avgLinkDensity < 0.3) score += 0.2;
        if (contentRatio > 0.3) score += 0.2;
        if (consecutiveLargeBlocks > 0) score += 0.1;
        
        // Negative factors
        if (avgLinkDensity > 0.7) score -= 0.3;
        if (verySmallBlocks > contentBlocks * 0.5) score -= 0.2;
        if (emptyBlocks > 0) score -= 0.1;
        
        contentQualityScore = Math.max(0, Math.min(1, score));
    }
    
    @Override
    public String toString() {
        return String.format(
            "Metrics{totalBlocks=%d, contentBlocks=%d, totalWords=%d, contentWords=%d, " +
            "largeBlocks=%d, mediumBlocks=%d, smallBlocks=%d, largestBlock=%d, " +
            "avgLinkDensity=%.3f, avgWordsPerBlock=%.1f, " +
            "totalImages=%d, largeImages=%d, totalVideos=%d, " +
            "contentRatio=%.3f, imageRatio=%.3f, qualityScore=%.3f}\n",
            totalBlocks, contentBlocks, totalWords, contentWords,
            largeContentBlocks, mediumContentBlocks, smallContentBlocks, largestBlockWords,
            avgLinkDensity, avgWordsPerContentBlock,
            totalImages, largeImages, totalVideos,
            contentRatio, imageToTextRatio, contentQualityScore
        );
    }
}
