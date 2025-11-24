package com.edufelip.meer.web;

import com.edufelip.meer.domain.auth.AuthenticatedUser;
import com.edufelip.meer.domain.GetGuideContentUseCase;
import com.edufelip.meer.dto.GuideContentDto;
import com.edufelip.meer.mapper.Mappers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contents")
public class GuideContentController {

    private final GetGuideContentUseCase getGuideContentUseCase;

    public GuideContentController(GetGuideContentUseCase getGuideContentUseCase) {
        this.getGuideContentUseCase = getGuideContentUseCase;
    }

    @GetMapping("/{id}")
    public GuideContentDto getById(@PathVariable Integer id) {
        var content = getGuideContentUseCase.execute(id);
        return content != null ? Mappers.toDto(content) : null;
    }
}
