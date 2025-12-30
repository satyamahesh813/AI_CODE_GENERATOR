package com.microgen.orchestrator.controller;

import com.microgen.orchestrator.model.GenerationJob;
import com.microgen.orchestrator.service.PromptOrchestrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GenerationController {

    private final PromptOrchestrationService orchestrationService;

    @PostMapping("/generate")
    public GenerationJob generate(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        GenerationJob job = orchestrationService.createJob(prompt);
        return orchestrationService.processJob(job.getId());
    }

    @GetMapping("/status/{jobId}")
    public GenerationJob getStatus(@PathVariable String jobId) {
        return orchestrationService.getJob(jobId);
    }

    @GetMapping("/download/{jobId}")
    public ResponseEntity<byte[]> download(@PathVariable String jobId) throws Exception {
        GenerationJob job = orchestrationService.getJob(jobId);
        byte[] zipContent = orchestrationService.getProjectZip(jobId);

        String filename = (job.getServiceName() != null ? job.getServiceName() : "microservice") + ".zip";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipContent);
    }
}
