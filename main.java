import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

@RestController
@RequestMapping("/api/bulk-process")
public class BulkVideoProcessorController {

    private static final String TEMP_DIR = "temp_bulk_processing";
    private static final int MAX_CONCURRENT_PROCESSES = 4; // Limit concurrent FFmpeg processes
    private static final ExecutorService PROCESSING_POOL = Executors.newFixedThreadPool(MAX_CONCURRENT_PROCESSES);
    
    @PostMapping("/")
    public ResponseEntity<Map<String, String>> processBulkVideos(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("silenceThreshold") float silenceThreshold,
            @RequestParam("minDuration") int minDuration,
            @RequestParam("removeSilence") boolean removeSilence,
            @RequestParam("removeStatic") boolean removeStatic,
            @RequestParam("autoPacing") boolean autoPacing,
            @RequestParam("autoCrop") boolean autoCrop,
            @RequestParam("stabilize") boolean stabilize,
            @RequestParam("colorCorrect") boolean colorCorrect,
            @RequestParam("autoVolume") boolean autoVolume,
            @RequestParam("faceFocus") boolean faceFocus) {
        
        // Validate input
        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "No files uploaded"));
        }
        
        if (files.length > 100) {
            return ResponseEntity.badRequest().body(Map.of("error", "Maximum 100 files allowed"));
        }
        
        // Create temp directory
        Path tempDir = Paths.get(TEMP_DIR);
        try {
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
            }
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Could not create temp directory"));
        }
        
        // Prepare response with processing IDs
        Map<String, String> response = new HashMap<>();
        response.put("batchId", UUID.randomUUID().toString());
        response.put("totalFiles", String.valueOf(files.length));
        
        // Start processing in background
        PROCESSING_POOL.submit(() -> {
            processFilesInBackground(
                files, 
                silenceThreshold,
                minDuration,
                removeSilence,
                removeStatic,
                autoPacing,
                autoCrop,
                stabilize,
                colorCorrect,
                autoVolume,
                faceFocus
            );
        });
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/status/{batchId}")
    public ResponseEntity<Map<String, Object>> getBatchStatus(@PathVariable String batchId) {
        // In a real implementation, this would check the actual processing status
        // For this demo, we'll return simulated progress
        
        Map<String, Object> response = new HashMap<>();
        response.put("batchId", batchId);
        response.put("processed", 25);
        response.put("success", 20);
        response.put("failed", 5);
        response.put("remaining", 75);
        response.put("status", "processing");
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/download/{batchId}")
    public void downloadProcessedBatch(
            @PathVariable String batchId,
            HttpServletResponse response) throws IOException {
        
        // In a real implementation, this would stream the processed files
        // For this demo, we'll just return a sample file
        
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=processed_videos.zip");
        
        // Create a sample ZIP file in a real implementation
        // For demo, we'll just send an empty response
        response.getOutputStream().flush();
    }
    
    private void processFilesInBackground(
            MultipartFile[] files,
            float silenceThreshold,
            int minDuration,
            boolean removeSilence,
            boolean removeStatic,
            boolean autoPacing,
            boolean autoCrop,
            boolean stabilize,
            boolean colorCorrect,
            boolean autoVolume,
            boolean faceFocus) {
        
        // Process each file with limited concurrency
        List<Future<Boolean>> futures = new ArrayList<>();
        
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            int fileIndex = i;
            
            futures.add(PROCESSING_POOL.submit(() -> {
                try {
                    return processSingleFile(
                        file,
                        fileIndex,
                        silenceThreshold,
                        minDuration,
                        removeSilence,
                        removeStatic,
                        autoPacing,
                        autoCrop,
                        stabilize,
                        colorCorrect,
                        autoVolume,
                        faceFocus
                    );
                } catch (Exception e) {
                    System.err.println("Error processing file " + file.getOriginalFilename() + ": " + e.getMessage());
                    return false;
                }
            }));
        }
        
        // Wait for all tasks to complete (in a real app, you'd track progress)
        for (Future<Boolean> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    private boolean processSingleFile(
            MultipartFile file,
            int fileIndex,
            float silenceThreshold,
            int minDuration,
            boolean removeSilence,
            boolean removeStatic,
            boolean autoPacing,
            boolean autoCrop,
            boolean stabilize,
            boolean colorCorrect,
            boolean autoVolume,
            boolean faceFocus) throws IOException {
        
        // Create unique filenames
        String inputFilename = "input_" + fileIndex + "_" + UUID.randomUUID() + getFileExtension(file.getOriginalFilename());
        String outputFilename = "output_" + fileIndex + "_" + UUID.randomUUID() + ".mp4";
        Path inputPath = Paths.get(TEMP_DIR, inputFilename);
        Path outputPath = Paths.get(TEMP_DIR, outputFilename);
        
        try {
            // Save uploaded file
            file.transferTo(inputPath.toFile());
            
            // Build FFmpeg command
            String command = buildProcessingCommand(
                inputPath.toString(),
                outputPath.toString(),
                silenceThreshold,
                minDuration,
                removeSilence,
                removeStatic,
                autoPacing,
                autoCrop,
                stabilize,
                colorCorrect,
                autoVolume,
                faceFocus
            );
            
            // Execute command with timeout
            boolean success = executeCommandWithTimeout(command, 300); // 5 minute timeout
            
            if (success && Files.exists(outputPath)) {
                // In a real app, you'd store the processed file
                System.out.println("Successfully processed: " + file.getOriginalFilename());
                return true;
            } else {
                System.err.println("Failed to process: " + file.getOriginalFilename());
                return false;
            }
            
        } finally {
            // Clean up temp files
            Files.deleteIfExists(inputPath);
            Files.deleteIfExists(outputPath);
        }
    }
    
    private String buildProcessingCommand(
            String inputPath,
            String outputPath,
            float silenceThreshold,
            int minDuration,
            boolean removeSilence,
            boolean removeStatic,
            boolean autoPacing,
            boolean autoCrop,
            boolean stabilize,
            boolean colorCorrect,
            boolean autoVolume,
            boolean faceFocus) {
        
        StringBuilder cmd = new StringBuilder("ffmpeg -i ")
            .append(escapePath(inputPath));
        
        // Audio processing
        if (removeSilence) {
            cmd.append(" -af \"silenceremove=start_periods=1:start_duration=1:start_threshold=-")
               .append(silenceThreshold * 60)
               .append("dB,areverse,silenceremove=start_periods=1:start_duration=1:start_threshold=-")
               .append(silenceThreshold * 60)
               .append("dB,areverse\"");
        }
        
        if (autoVolume) {
            cmd.append(" -af \"loudnorm=I=-16:TP=-1.5:LRA=11\"");
        }
        
        // Video processing
        List<String> videoFilters = new ArrayList<>();
        
        if (stabilize) {
            videoFilters.add("deshake");
        }
        
        if (colorCorrect) {
            videoFilters.add("colorbalance=rs=0.1:gs=0.1:bs=0.1");
        }
        
        if (autoCrop) {
            videoFilters.add("cropdetect=limit=0.1:round=2");
        }
        
        if (faceFocus) {
            videoFilters.add("zoompan=z='min(zoom+0.001,1.2)':d=25");
        }
        
        if (removeStatic || autoPacing) {
            videoFilters.add("select='gt(scene,0.1)',setpts=N/FRAME_RATE/TB");
        }
        
        if (!videoFilters.isEmpty()) {
            cmd.append(" -vf \"").append(String.join(",", videoFilters)).append("\"");
        }
        
        // Minimum clip duration
        cmd.append(" -min_seg_duration ").append(minDuration).append("000000");
        
        // Output settings
        cmd.append(" -c:v libx264 -preset fast -crf 23 -c:a aac -strict experimental ")
           .append(escapePath(outputPath));
        
        return cmd.toString();
    }
    
    private boolean executeCommandWithTimeout(String command, int timeoutSeconds) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            
            Future<?> future = PROCESSING_POOL.submit(() -> {
                try (BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        System.out.println("[FFmpeg] " + line);
                    }
                    return process.waitFor();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return -1;
                } catch (IOException e) {
                    return -1;
                }
            });
            
            try {
                future.get(timeoutSeconds, TimeUnit.SECONDS);
                return process.exitValue() == 0;
            } catch (TimeoutException e) {
                process.destroy();
                future.cancel(true);
                return false;
            } catch (Exception e) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
    }
    
    private String escapePath(String path) {
        return "\"" + path + "\"";
    }
    
    private String getFileExtension(String filename) {
        return filename != null ? filename.substring(filename.lastIndexOf(".")) : ".mp4";
    }
}
