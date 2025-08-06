// src/main/java/com/example/copyrightscanner/CopyrightScannerApplication.java
package com.example.copyrightscanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SpringBootApplication
@RestController
public class CopyrightScannerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CopyrightScannerApplication.class, args);
    }

    private final String UPLOAD_DIR = "uploads/";
    private final String PROCESSED_DIR = "processed/";

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file) {
        try {
            // Create upload directory if it doesn't exist
            Files.createDirectories(Paths.get(UPLOAD_DIR));
            
            // Generate unique filename
            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path path = Paths.get(UPLOAD_DIR + filename);
            
            // Save file
            file.transferTo(path);
            
            return filename;
        } catch (IOException e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/scan")
    public ScanResponse scanVideo(@RequestBody ScanRequest request) {
        // In a real application, this would use FFmpeg and content recognition APIs
        // Here we simulate the scanning process
        
        String filename = request.getFilename();
        int sensitivity = request.getSensitivity();
        String contentType = request.getContentType();
        
        // Simulate processing time
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Generate mock results
        List<CopyrightDetection> detections = new ArrayList<>();
        
        // More detections with higher sensitivity
        int detectionCount = sensitivity * 2 + (int)(Math.random() * 3);
        
        for (int i = 0; i < detectionCount; i++) {
            double start = Math.random() * 120; // up to 2 minutes
            double duration = 5 + Math.random() * 10; // 5-15 seconds
            String type = contentType.equals("all") ? 
                new String[]{"music", "video", "logo"}[(int)(Math.random() * 3)] : 
                contentType;
            
            String[] confidenceLevels = {"high", "medium", "low"};
            String confidence = confidenceLevels[(int)(Math.random() * 3)];
            
            detections.add(new CopyrightDetection(
                start,
                start + duration,
                confidence,
                type,
                getRandomMatch(type)
            ));
        }
        
        return new ScanResponse(filename, detections);
    }

    @PostMapping("/remove")
    public ProcessResponse removeCopyright(@RequestBody ProcessRequest request) {
        // In a real application, this would use FFmpeg to remove segments
        // Here we simulate the process
        
        String filename = request.getFilename();
        List<CopyrightDetection> segmentsToRemove = request.getSegments();
        
        // Simulate processing time
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Generate output filename
        String outputFilename = "cleaned_" + filename;
        
        return new ProcessResponse(outputFilename, "Copyright content removed successfully");
    }

    private String getRandomMatch(String type) {
        switch (type) {
            case "music":
                return new String[]{"Popular Song", "Movie Soundtrack", "Commercial Jingle"}[(int)(Math.random() * 3)];
            case "video":
                return new String[]{"Movie Clip", "TV Show Segment", "Sports Highlight"}[(int)(Math.random() * 3)];
            case "logo":
                return new String[]{"Company Logo", "TV Network Watermark", "Sponsor Graphic"}[(int)(Math.random() * 3)];
            default:
                return "Copyrighted Content";
        }
    }
}

// Request/Response classes
class ScanRequest {
    private String filename;
    private int sensitivity;
    private String contentType;
    
    // Getters and setters
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public int getSensitivity() { return sensitivity; }
    public void setSensitivity(int sensitivity) { this.sensitivity = sensitivity; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
}

class ScanResponse {
    private String filename;
    private List<CopyrightDetection> detections;
    
    public ScanResponse(String filename, List<CopyrightDetection> detections) {
        this.filename = filename;
        this.detections = detections;
    }
    
    // Getters
    public String getFilename() { return filename; }
    public List<CopyrightDetection> getDetections() { return detections; }
}

class CopyrightDetection {
    private double startTime;
    private double endTime;
    private String confidence;
    private String contentType;
    private String matchedContent;
    
    public CopyrightDetection(double startTime, double endTime, String confidence, 
                            String contentType, String matchedContent) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.confidence = confidence;
        this.contentType = contentType;
        this.matchedContent = matchedContent;
    }
    
    // Getters
    public double getStartTime() { return startTime; }
    public double getEndTime() { return endTime; }
    public String getConfidence() { return confidence; }
    public String getContentType() { return contentType; }
    public String getMatchedContent() { return matchedContent; }
}

class ProcessRequest {
    private String filename;
    private List<CopyrightDetection> segments;
    
    // Getters and setters
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public List<CopyrightDetection> getSegments() { return segments; }
    public void setSegments(List<CopyrightDetection> segments) { this.segments = segments; }
}

class ProcessResponse {
    private String filename;
    private String message;
    
    public ProcessResponse(String filename, String message) {
        this.filename = filename;
        this.message = message;
    }
    
    // Getters
    public String getFilename() { return filename; }
    public String getMessage() { return message; }
}
