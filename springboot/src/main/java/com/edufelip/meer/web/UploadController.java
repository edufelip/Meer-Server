package com.edufelip.meer.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/uploads")
public class UploadController {

    @PostMapping("/communities")
    public ResponseEntity<String> uploadCommunity(@RequestPart("community") String communityJson,
                                                  @RequestPart("community") MultipartFile communityFile) {
        return ResponseEntity.ok("queued community upload");
    }

    @PostMapping("/posts")
    public ResponseEntity<String> uploadPost(@RequestPart("post") String postJson,
                                             @RequestPart(value = "post", required = false) MultipartFile postFile) {
        return ResponseEntity.ok("queued post upload");
    }
}
