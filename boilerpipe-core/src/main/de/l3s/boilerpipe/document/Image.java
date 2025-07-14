package de.l3s.boilerpipe.document;

/**
 * Represents an Image resource that is contained in the document.
 * 
 * Any of the attributes may be null, except for "src".
 *  
 * @author Christian Kohlschuetter
 */
public class Image implements Comparable<Image> {
	private String src;
	private String width;
	private String height;
	private final String alt;
	private int area;

	public Image(final String src, final String width, final String height, final String alt) {
		if(src == null) {
			throw new NullPointerException("src attribute must not be null");
		}

		this.src = src;
		this.width = nullTrim(width);
		this.height = nullTrim(height);
		this.alt = nullTrim(alt);
		
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

	public String getAlt() {
		return alt;
	}

	public void setWidth(int width) {
		this.width = nullTrim(String.valueOf(width));
	}

	public void setHeight(int height) {
		this.height = nullTrim(String.valueOf(height));
	}

	public void setArea(int area) {
		this.area = area;
	}

	private static String nullTrim(String s) {
		if(s == null) {
			return null;
		}
		s = s.trim();
		if(s.length() == 0) {
			return null;
		}
		return s;
	}
	
	/**
	 * Returns the image's area (specified by width * height), or -1 if width/height weren't both specified or could not be parsed.
	 * 
	 * @return
	 */
	public int getArea() {
		return area;
	}
	
	public String toString() {
		return src+"\twidth="+width+"\theight="+height+"\talt="+alt+"\tarea="+area;
	}

	@Override
	public int compareTo(Image o) {
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

    public void setSource(String newSrc) {
		this.src = newSrc;
    }
}
