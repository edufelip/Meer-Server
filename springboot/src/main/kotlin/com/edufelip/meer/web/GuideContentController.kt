package com.edufelip.meer.web

import com.edufelip.meer.domain.GetGuideContentUseCase
import com.edufelip.meer.dto.GuideContentDto
import com.edufelip.meer.mapper.toDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/contents")
class GuideContentController(
    private val getGuideContentUseCase: GetGuideContentUseCase,
) {

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Int): GuideContentDto? {
        return getGuideContentUseCase.execute(id)?.toDto()
    }
}
