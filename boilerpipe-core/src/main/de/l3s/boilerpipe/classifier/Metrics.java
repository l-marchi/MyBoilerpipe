package de.l3s.boilerpipe.classifier;

public class Metrics {

    // Content metrics
    int totalBlocks = 0;
    int contentBlocks = 0;
    int boilerplateBlocks = 0;
    int totalWords = 0;
    int contentWords = 0;

    // Block size categorization
    int largeContentBlocks = 0;  // >75 words
    int mediumContentBlocks = 0; // 20-75 words
    int smallContentBlocks = 0;  // 1-20 words
    int largestBlockWords = 0;

    // Density metrics and averages (per block)
    double totalLinkDensity = 0;
    double avgLinkDensity = 0;
    double totalTextDensity = 0;
    double avgTextDensity = 0;
    double avgWordsPerContentBlock = 0;
    int highLinkDensityBlocks = 0;   // blocks with link density > HIGH_LINK_DENSITY

    // Image metrics
    int totalImages = 0;
    int largeImages = 0; // >= LARGE_IMAGE_SIZE x LARGE_IMAGE_SIZE pixels
    long totalImageArea = 0;
    
    // Ratios
    double contentRatio = 0;        // contentBlocks / totalBlocks
    double imageToTextRatio = 0;    // totalImages / contentWords
    double largeBlockRatio = 0;     // largeContentBlocks / contentBlocks
         // blocks with 0 words
    
    /**
     * Helper method to calculate derived ratios after basic metrics is computed
     */
    public void calculateDerivedMetrics() {
        contentRatio = totalBlocks > 0 ? (double) contentBlocks / totalBlocks : 0;
        imageToTextRatio = contentWords > 0 ? (double) totalImages / contentWords : 0;
        largeBlockRatio = contentBlocks > 0 ? (double) largeContentBlocks / contentBlocks : 0;
    }
    
    @Override
    public String toString() {
        return String.format(
            "Metrics{totalBlocks=%d, contentBlocks=%d, totalWords=%d, contentWords=%d, " +
            "largeBlocks=%d, mediumBlocks=%d, smallBlocks=%d, largestBlock=%d, " +
            "avgLinkDensity=%.3f, avgTextDensity=%.3f, avgWordsPerBlock=%.1f, " +
            "totalImages=%d, largeImages=%d, contentRatio=%.3f, imageRatio=%.3f}\n",
            totalBlocks, contentBlocks, totalWords, contentWords,
            largeContentBlocks, mediumContentBlocks, smallContentBlocks, largestBlockWords,
            avgLinkDensity, avgTextDensity, avgWordsPerContentBlock,
            totalImages, largeImages, contentRatio, imageToTextRatio
        );
    }
}
