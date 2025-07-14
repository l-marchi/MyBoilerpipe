package de.l3s.boilerpipe.classifier;

import java.util.regex.Pattern;

public class Regex {
    static final Pattern VIDEO_PATTERN = Pattern.compile(
            ".*?/(?:video|videos|player|play|watch|stream|streaming|embed|live|channel|channels)(?:/[^/]+\\.[a-z0-9]+)?(?:/[^/]+)?.*$|" +
                    ".*(?:youtube|video|vimeo|dailymotion|twitch|netflix|hulu|tiktok|mp4|webm|mov|avi|mkv|ogg)\\.com/.+.*",
            Pattern.CASE_INSENSITIVE
    );

    static final Pattern FORUM_PATTERN = Pattern.compile(
            ".*?/(?:forum|forums|community|board|discussion|discuss|thread|threads|topic|topics|viewtopic|post|reply|comment|comments)(?:/[^/]+\\.[a-z0-9]+)?(?:/[^/]+)?.*$|" +
                    ".*(?:reddit|stackoverflow|quora|discourse|phpbb|vbulletin|xenforo|invision)\\.com/.+.*",
            Pattern.CASE_INSENSITIVE
    );

    static final Pattern PHOTO_GALLERY_PATTERN = Pattern.compile(
            ".*?/(?:photo|photos|gallery|galleries|image|images|album|albums|slideshow|carousel|portfolio)(?:/[^/]+\\.[a-z0-9]+)?(?:/[^/]+)?.*$|" +
                    ".*(?:instagram|flickr|imgur|pinterest|500px|smugmug|photobucket)\\.com/.+.*",
            Pattern.CASE_INSENSITIVE
    );

    static final Pattern COMIC_PATTERN = Pattern.compile(
            ".*?/(?:comic|comics|webcomic|manga|graphic|chapter|page)(?:/[^/]+\\.[a-z0-9]+)?(?:/[^/]+)?.*$|" +
                    ".*(?:webtoons|tapas|globalcomix|marvel|dccomics|darkhorse|imagecomics|mangaplus)\\.com/.+.*",
            Pattern.CASE_INSENSITIVE
    );

    static final Pattern ARTICLE_PATTERN = Pattern.compile(
            ".*?/(?:article|articles|art|blog)(?:/[^/]+\\.[a-z0-9]+)?(?:/[^/]+)?.*$|" +
                    ".*(?:quora|medium|substack|huffpost|ezine-articles|hubpages|businessinsider|vocal\\.media)\\.com/.+.*",
            Pattern.CASE_INSENSITIVE
    );

    static final Pattern HOMEPAGE_PATTERN = Pattern.compile(
            "^(?:https?://)?(?:www\\.)?(?:[^/]+\\.)+[^/]+/?$",
            Pattern.CASE_INSENSITIVE
    );
}
