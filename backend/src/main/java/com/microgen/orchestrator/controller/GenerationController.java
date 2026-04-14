package com.microgen.orchestrator.controller;

import com.microgen.orchestrator.model.GenerationJob;
import com.microgen.orchestrator.model.GenerationRequest;
import com.microgen.orchestrator.service.PromptOrchestrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GenerationController {

    private final PromptOrchestrationService orchestrationService;

    @PostMapping("/generate")
    public GenerationJob generate(@Valid @RequestBody GenerationRequest request) {
        return orchestrationService.createAndProcess(request);
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<GenerationJob> getStatus(@PathVariable String jobId) {
        GenerationJob job = orchestrationService.getJob(jobId);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(job);
    }

    @GetMapping("/download/{jobId}")
    public ResponseEntity<byte[]> download(@PathVariable String jobId) throws Exception {
        GenerationJob job = orchestrationService.getJob(jobId);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] zipContent = orchestrationService.getProjectZip(jobId);
        String filename = (job.getServiceName() != null ? job.getServiceName() : "microservice") + ".zip";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipContent);
    }
}
