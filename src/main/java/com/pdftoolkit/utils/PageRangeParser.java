package com.pdftoolkit.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for parsing page range specifications like "1,3,5-7,10-12"
 */
public class PageRangeParser {
    
    /**
     * Parses a page range string into a list of individual page numbers.
     * Supports formats like: "1", "1,3,5", "1-5", "1,3-5,7-10"
     * 
     * @param rangeSpec The range specification string
     * @param maxPage Maximum page number allowed
     * @return List of individual page numbers (1-indexed)
     * @throws IllegalArgumentException if format is invalid or pages out of bounds
     */
    public static List<Integer> parse(String rangeSpec, int maxPage) throws IllegalArgumentException {
        if (rangeSpec == null || rangeSpec.trim().isEmpty()) {
            throw new IllegalArgumentException("Page range specification cannot be empty");
        }
        
        List<Integer> pages = new ArrayList<>();
        String[] parts = rangeSpec.split(",");
        
        for (String part : parts) {
            part = part.trim();
            
            if (part.isEmpty()) {
                continue;
            }
            
            if (part.contains("-")) {
                // Range like "5-10"
                String[] rangeParts = part.split("-");
                if (rangeParts.length != 2) {
                    throw new IllegalArgumentException("Invalid range format: " + part);
                }
                
                try {
                    int start = Integer.parseInt(rangeParts[0].trim());
                    int end = Integer.parseInt(rangeParts[1].trim());
                    
                    if (start < 1 || end < 1) {
                        throw new IllegalArgumentException("Page numbers must be >= 1");
                    }
                    
                    if (start > end) {
                        throw new IllegalArgumentException("Invalid range: " + start + "-" + end + " (start > end)");
                    }
                    
                    if (end > maxPage) {
                        throw new IllegalArgumentException("Page " + end + " exceeds document pages (" + maxPage + ")");
                    }
                    
                    for (int i = start; i <= end; i++) {
                        if (!pages.contains(i)) {
                            pages.add(i);
                        }
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid page number in range: " + part);
                }
            } else {
                // Single page like "5"
                try {
                    int page = Integer.parseInt(part);
                    
                    if (page < 1) {
                        throw new IllegalArgumentException("Page numbers must be >= 1");
                    }
                    
                    if (page > maxPage) {
                        throw new IllegalArgumentException("Page " + page + " exceeds document pages (" + maxPage + ")");
                    }
                    
                    if (!pages.contains(page)) {
                        pages.add(page);
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid page number: " + part);
                }
            }
        }
        
        if (pages.isEmpty()) {
            throw new IllegalArgumentException("No valid pages specified");
        }
        
        pages.sort(Integer::compareTo);
        return pages;
    }
    
    /**
     * Validates a page range string without parsing.
     * 
     * @param rangeSpec The range specification
     * @param maxPage Maximum page number
     * @return Error message if invalid, null if valid
     */
    public static String validate(String rangeSpec, int maxPage) {
        try {
            parse(rangeSpec, maxPage);
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }
}
