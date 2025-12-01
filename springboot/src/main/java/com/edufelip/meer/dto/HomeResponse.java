package com.edufelip.meer.dto;

import java.util.List;

public record HomeResponse(
        List<FeaturedStoreDto> featured,
        List<NearbyStoreDto> nearby,
        List<GuideContentDto> content
) {}
