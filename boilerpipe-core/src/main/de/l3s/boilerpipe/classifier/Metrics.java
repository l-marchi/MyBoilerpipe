package de.l3s.boilerpipe.classifier;

public class Metrics {
    // Content metrics
    int totalBlocks = 0;
    int contentBlocks = 0;
    int boilerplateBlocks = 0;
    int totalWords = 0;
    int contentWords = 0;

    // Block size
    int largeContentBlocks = 0;  // >75 words?
    int mediumContentBlocks = 0; // 20-75 words?
    int smallContentBlocks = 0;  // 1-20 words?
    int largestBlockWords = 0;


    // Density metrics and averages
    double totalLinkDensity = 0;
    double avgLinkDensity = 0;
    double totalTextDensity = 0;
    double avgTextDensity = 0;
    double avgWordsPerContentBlock = 0;

    // Image metrics
    int totalImages = 0;
    int largeImages = 0; // (250 x 250)?
    long totalImageArea = 0;
}
