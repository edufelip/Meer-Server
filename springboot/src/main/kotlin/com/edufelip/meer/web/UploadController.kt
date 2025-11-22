package com.edufelip.meer.web

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/uploads")
class UploadController {

    // Placeholder endpoints to mirror expected upload surface; replace with real storage integration.

    @PostMapping("/communities")
    fun uploadCommunity(
        @RequestPart("community") communityJson: String,
        @RequestPart("community") communityFile: MultipartFile
    ): ResponseEntity<String> = ResponseEntity.ok("queued community upload")

    @PostMapping("/posts")
    fun uploadPost(
        @RequestPart("post") postJson: String,
        @RequestPart("post", required = false) postFile: MultipartFile?
    ): ResponseEntity<String> = ResponseEntity.ok("queued post upload")
}
