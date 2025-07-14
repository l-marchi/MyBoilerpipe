package de.l3s.boilerpipe.classifier;

public class Constants {
    // Global thresholds
    static final int LARGE_IMAGE_SIZE = 700;
    static final int WORDS_LARGE_BLOCK = 60;
    static final int WORDS_MEDIUM_BLOCK = 15;
    static final int WORDS_SMALL_BLOCK = 5;

    // PHOTO GALLERY - Refined thresholds to distinguish from homepages
    static class PhotoGallery {
        static final int MIN_IMAGES = 6;
        static final int OPTIMAL_IMAGES = 12;
        static final double MIN_IMAGE_TEXT_RATIO = 0.05;
        static final double MAX_IMAGE_TEXT_RATIO = 4.0;     // (galleries are very image heavy)
        static final int MAX_AVG_WORDS_PER_BLOCK = 25;     //  (galleries have less text)
        static final double MAX_LINK_DENSITY = 0.3;        //  (galleries have less navigation)
        static final double MIN_CONTENT_RATIO = 0.1;       //  (galleries can have low content ratio)
        static final int MIN_LARGE_IMAGES = 1;             // (galleries have more large images)
        static final int MAX_CONTENT_BLOCKS = 50;          // galleries have fewer content blocks than homepages
        static final int MAX_TOTAL_BLOCKS = 100;
    }

    // ARTICLE - Enhanced criteria
    static class Article {
        static final int MIN_TOTAL_WORDS = 100;
        static final int MIN_LARGE_CONTENT_BLOCKS = 1;
        static final double MIN_LARGE_BLOCK_RATIO = 0.2;
        static final double MIN_CONTENT_RATIO = 0.3;
        static final double MAX_AVG_LINK_DENSITY = 0.2;
        static final int MIN_LARGEST_BLOCK_WORDS = 60;
        static final int MIN_AVG_WORDS_PER_BLOCK = 25;
        static final double MIN_QUALITY_SCORE = 0.4;
        static final int MIN_CONTENT_WORDS = 100;
        static final int MIN_CONSECUTIVE_LARGE_BLOCKS = 1;
    }

    // VIDEO PLAYER - Better detection
    static class VideoPlayer {
        private static final int MIN_VIDEOS = 1;
        static final double MAX_CONTENT_RATIO = 0.3;
        static final int MAX_CONTENT_WORDS = 200;
        static final double MIN_AVG_LINK_DENSITY = 0.1;
        static final double MAX_AVG_LINK_DENSITY = 0.7;
        static final double MAX_LARGE_BLOCK_RATIO = 0.3;
        static final double MIN_MEDIA_RATIO = 0.01;
        static final int MAX_LARGE_BLOCKS = 2;
    }

    // COMIC - Enhanced detection
    static class Comic {
        public static final int TOTAL_BLOCKS = 30;
        public static final int MAX_CONTENT_BLOCKS = 10;
        static final int MIN_LARGE_IMAGE_SIZE = 500;
        static final int MIN_TOTAL_IMAGES = 1;
        static final int MAX_TOTAL_IMAGES = 30;
        static final int MAX_CONTENT_WORDS = 200;
        static final double MAX_AVG_WORDS_PER_BLOCK = 20;
        static final double MIN_IMAGE_TO_TEXT_RATIO = 0.02;
        static final double MAX_LINK_DENSITY = 0.2;
        static final double MIN_LARGE_IMAGE_RATIO = 0.3;
    }

    // FORUM - Better identification with refined thresholds
    static class Forum {
        static final int MIN_TOTAL_WORDS = 100;
        static final double MAX_BLOCK_SIZE_VARIANCE = 150;
        static final int MIN_TOTAL_BLOCKS = 15;
        static final double MIN_CONTENT_RATIO = 0.1;
        static final double MIN_AVG_LINK_DENSITY = 0.0;
        static final double MAX_AVG_LINK_DENSITY = 0.4;
        static final int MAX_AVG_WORDS_PER_BLOCK = 40;
        static final double MAX_LARGE_BLOCK_RATIO = 0.3;
        static final int MIN_MEDIUM_BLOCKS = 3;
        static final double MIN_BLOCK_SIZE_VARIANCE = 25.0;
        static final int MIN_CONTENT_BLOCKS = 6;
        static final double MAX_CONTENT_RATIO = 0.4;
        static final int MAX_CONTENT_BLOCKS = 80;
        static final int MAX_IMAGES = 10;
    }

    // HOMEPAGE - More precise criteria for news/portal sites
    static class Homepage {
        static final int MIN_CONTENT_BLOCKS = 10;           // (news sites have many blocks)
        static final int MAX_CONTENT_BLOCKS = 300;          //  (news sites are content-heavy)
        static final int MIN_IMAGES = 1;                    //  (some extractors miss images)
        static final int MAX_IMAGES = 50;                  //  (news sites have many images)
        static final double MIN_CONTENT_RATIO = 0.02;       //  (news sites have lots of boilerplate)
        static final double MAX_CONTENT_RATIO = 0.3;        //  (news sites have lower ratios)
        static final int MIN_SMALL_CONTENT_BLOCKS = 5;      // (news headlines/teasers)
        static final double MIN_AVG_LINK_DENSITY = 0.1;     //  (news sites are link-heavy)
        static final double MAX_AVG_LINK_DENSITY = 1.0;     //  (news sites have very high link density)
        static final double MAX_LARGE_BLOCK_RATIO = 0.2;    //  (news sites have fewer large blocks)
        static final int MIN_MEDIUM_BLOCKS = 5;             //  (news sites have many medium blocks)
        static final int MIN_TOTAL_BLOCKS = 40;
    }
}
