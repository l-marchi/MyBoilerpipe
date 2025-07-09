package de.l3s.boilerpipe.document;

/**
 * Represents a Video resource that is contained in the document.
 *
 */
public class Video implements Comparable<Video> {
	private final String src;
	private final String width;
	private final String height;
	private final int area;

	public Video(final String src, final String width, final String height) {
		this.src = src;

		this.width = nullTrim(width);
		this.height = nullTrim(height);
		
		if(width != null && height != null) {
			int a;
			try {
				a = Integer.parseInt(width) * Integer.parseInt(height);
			} catch(NumberFormatException e) {
				a = -1;
			}
			this.area = a;
		} else {
			this.area = -1;
		}
	}

	public String getSrc() {
		return src;
	}

	public String getWidth() {
		return width;
	}

	public String getHeight() {
		return height;
	}
	
	private static String nullTrim(String s) {
		if(s == null) {
			return null;
		}
		s = s.trim();
		if(s.isEmpty()) {
			return null;
		}
		return s;
	}
	
	/**
	 * Returns the video's area (specified by width * height), or -1 if width/height weren't both specified or could not be parsed.
	 * 
	 * @return
	 */
	public int getArea() {
		return area;
	}
	
	public String toString() {
		return src + "\twidth=" + width + "\theight=" + height + "\tarea=" + area;
	}

	@Override
	public int compareTo(Video o) {
		if(o == this) {
			return 0;
		}
		if(area > o.area) {
			return -1;
		} else if(area == o.area) {
			return src.compareTo(o.src);
		} else {
			return 1;
		}
	}
} 