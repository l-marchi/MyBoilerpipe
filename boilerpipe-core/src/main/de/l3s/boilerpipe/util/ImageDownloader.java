package de.l3s.boilerpipe.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import de.l3s.boilerpipe.demo.WebpageClassifierDemo;
import de.l3s.boilerpipe.document.Image;

/**
 * Utility class for downloading images extracted by ImageExtractor.
 *
 */
public class ImageDownloader {

    /**
     * Downloads images from a list of Image objects to a specified directory.
     * If the directory exists, it will be emptied before downloading.
     *
     * @param images A list of Image objects containing image URLs
     * @param targetDir The directory to save the downloaded images
     * @return A list of successfully downloaded image file paths
     * @throws IOException If there's an error during download
     */
    public static List<File> downloadImages(List<Image> images, File targetDir) throws IOException {
        // Delete directory if it exists
        if (targetDir.exists()) {
            deleteDirectory(targetDir);
        }

        // Create the directory
        if (!targetDir.mkdirs()) {
            throw new IOException("Could not create directory: " + targetDir.getAbsolutePath());
        }

        List<File> downloadedFiles = new ArrayList<>();
        int counter = 0;
        for (Image image : images) {
            String src = image.getSrc();
            File downloadedFile = null;
            
            if (src != null && !src.trim().isEmpty()) {
                try {
                    String newSrc;
                    if (!src.startsWith("http")) {
                        // Relative URL - prepend base URL
                        String baseUrl = WebpageClassifierDemo.TEST_URL;
                        
                        // Ensure base URL has protocol
                        if (!baseUrl.startsWith("http")) {
                            baseUrl = "https://" + baseUrl;
                        }
                        
                        // Ensure base URL doesn't end with slash and src starts with slash
                        if (baseUrl.endsWith("/")) {
                            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                        }
                        
                        newSrc = baseUrl;
                        if (!src.startsWith("/")) {
                            newSrc += "/";
                        }
                        newSrc += src;
                        image.setSource(newSrc);
                    } else {
                        // Absolute URL already present
                        newSrc = src;
                    }

                    URL imageUrl = new URL(newSrc);
                    
                    // Extract proper file extension from URL
                    String extension = getFileExtension(src);
                    String fileName = "image_" + counter + extension;
                    File targetFile = new File(targetDir, fileName);

                    HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0");

                    try (InputStream in = connection.getInputStream();
                         FileOutputStream out = new FileOutputStream(targetFile)) {

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                        downloadedFile = targetFile;
                    }
                } catch (MalformedURLException e) {
                    // Skip malformed URLs
                    System.err.println("Invalid URL: " + src + " - " + e.getMessage());
                } catch (IOException e) {
                    // Log but don't throw to continue with other images
                    System.err.println("Failed to download: " + src + " - " + e.getMessage());
                }
            } else {
                System.err.println("Empty or null URL for image at index " + counter);
            }
            
            // Add file to list (null if download failed) to maintain correspondence
            downloadedFiles.add(downloadedFile);
            counter++;
        }

        return downloadedFiles;
    }

    /**
     * Extracts the file extension from a URL or file path
     *
     * @param url The URL or file path
     * @return The file extension.
     */
    private static String getFileExtension(String url) {
        // Remove query parameters if present
        int queryIndex = url.indexOf('?');
        if (queryIndex != -1) {
            url = url.substring(0, queryIndex);
        }
        
        // Remove fragment if present
        int fragmentIndex = url.indexOf('#');
        if (fragmentIndex != -1) {
            url = url.substring(0, fragmentIndex);
        }
        
        // Extract extension
        int lastDotIndex = url.lastIndexOf('.');
        if (lastDotIndex != -1 && lastDotIndex < url.length() - 1) {
            return url.substring(lastDotIndex).toLowerCase();
        }
        
        return ".jpg"; // Default extension if none found or invalid
    }

    /**
     * Recursively deletes a directory and all its contents
     *
     * @param directory The directory to delete
     * @return true if successful, false otherwise
     */
    private static boolean deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        return directory.delete();
    }
}
