package de.l3s.boilerpipe.sax;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.l3s.boilerpipe.document.Video;

/**
 * Simple video parser that uses regex patterns to extract video elements from HTML.
 */
public final class VideoParser {
	public static final VideoParser INSTANCE = new VideoParser();
	
	// Simple regex patterns for video elements
	private static final Pattern VIDEO_TAG_PATTERN = Pattern.compile(
		"<video[^>]*(?:src=[\"']([^\"']+)[\"'][^>]*|[^>]*)>(?:.*?<source[^>]*src=[\"']([^\"']+)[\"'][^>]*>)?.*?</video>", 
		Pattern.CASE_INSENSITIVE);
	
	private static final Pattern IFRAME_PATTERN = Pattern.compile(
		"<iframe[^>]*src=[\"']([^\"']+)[\"'][^>]*>", 
		Pattern.CASE_INSENSITIVE);
	
	private static final Pattern OBJECT_EMBED_PATTERN = Pattern.compile(
		"<(?:object|embed)[^>]*(?:data|src)=[\"']([^\"']+)[\"'][^>]*>", 
		Pattern.CASE_INSENSITIVE);

	private static final Pattern VIDEO_LINK_PATTERN = Pattern.compile(
			"<a[^>]*href=[\"'](/video/[^\"']+)[\"'][^>]*>",
			Pattern.CASE_INSENSITIVE
	);
	
	private VideoParser() {
	}
	
	/**
	 * Returns the singleton instance of VideoParser.
	 *
     */
	public static VideoParser getInstance() {
		return INSTANCE;
	}

	/**
	 * Extracts videos from HTML content using simple regex patterns.
	 * 
	 * @param htmlContent The HTML content to parse
	 * @return List of extracted videos
	 */
	public List<Video> extractVideos(final String htmlContent) {
		List<Video> videos = new ArrayList<Video>();
		
		// Extract video tags
		extractVideoTags(htmlContent, videos);
		
		// Extract iframe
		extractIframe(htmlContent, videos);
		
		// Extract object/embed tags
		extractObjectEmbeds(htmlContent, videos);

		
		return videos;
	}

    // matcher.group(0) = entire matched text
    // matcher.group(1) = first capture group (src attribute in video tag)
    // matcher.group(2) = second capture group (src attribute in source tag)
	
	private void extractVideoTags(final String htmlContent, List<Video> videos) {
		Matcher matcher = VIDEO_TAG_PATTERN.matcher(htmlContent);
		
		while (matcher.find()) {
			String src = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
			if (src != null && !src.isEmpty()) {
				String videoTag = matcher.group(0);
				String width = extractAttribute(videoTag, "width");
				String height = extractAttribute(videoTag, "height");
				videos.add(new Video(src, width, height));
			}
		}
	}
	
	private void extractIframe(final String htmlContent, List<Video> videos) {
		Matcher matcher = IFRAME_PATTERN.matcher(htmlContent);
		
		while (matcher.find()) {
			String src = matcher.group(1);
			if (src != null && !src.isEmpty()) {
				String iframeTag = matcher.group(0);
				String width = extractAttribute(iframeTag, "width");
				String height = extractAttribute(iframeTag, "height");
				videos.add(new Video(src, width, height));
			}
		}
	}
	
	private void extractObjectEmbeds(final String htmlContent, List<Video> videos) {
		Matcher matcher = OBJECT_EMBED_PATTERN.matcher(htmlContent);
		
		while (matcher.find()) {
			String src = matcher.group(1);
			if (src != null && !src.isEmpty()) {
				String embedTag = matcher.group(0);
				String width = extractAttribute(embedTag, "width");
				String height = extractAttribute(embedTag, "height");
				videos.add(new Video(src, width, height));
			}
		}
	}

	private void extractVideoLinks(final String htmlContent, List<Video> videos){
		Matcher matcher = VIDEO_LINK_PATTERN.matcher(htmlContent);

		while (matcher.find()) {
			String src = matcher.group(1);
			if (src != null && !src.isEmpty()) {
				String embedTag = matcher.group(0);
				String width = extractAttribute(embedTag, "width");
				String height = extractAttribute(embedTag, "height");
				videos.add(new Video(src, width, height));
			}
		}

	}
	
	private String extractAttribute(final String tag, final String attributeName) {
		Pattern pattern = Pattern.compile(
			"\\b" + attributeName + "=[\"']([^\"']*)[\"']", 
			Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(tag);
		
		if (matcher.find()) {
			return matcher.group(1);
		}
		return null;
	}
} 