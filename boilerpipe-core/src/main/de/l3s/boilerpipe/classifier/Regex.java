package de.l3s.boilerpipe.classifier;

import java.util.regex.Pattern;

public class Regex {

    static final Pattern FORUM_PATTERN = Pattern.compile(
            ".*?/(?:forum|forums|community|board|discussion|discuss|thread|threads|topic|topics|viewtopic|post|reply|comment|comments)(?:/[^/]+\\.[a-z0-9]+)?(?:/[^/]+)?.*$|" +
                    ".*(?:reddit|stackoverflow|quora|discourse|phpbb|vbulletin|xenforo|invision)\\.com/.+.*",
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
