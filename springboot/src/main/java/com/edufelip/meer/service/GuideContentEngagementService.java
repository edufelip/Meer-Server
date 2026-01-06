package com.edufelip.meer.service;

import com.edufelip.meer.domain.repo.GuideContentCommentRepository;
import com.edufelip.meer.domain.repo.GuideContentLikeRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GuideContentEngagementService {

  public record EngagementSummary(long likeCount, long commentCount, boolean likedByMe) {}

  private final GuideContentLikeRepository guideContentLikeRepository;
  private final GuideContentCommentRepository guideContentCommentRepository;

  public GuideContentEngagementService(
      GuideContentLikeRepository guideContentLikeRepository,
      GuideContentCommentRepository guideContentCommentRepository) {
    this.guideContentLikeRepository = guideContentLikeRepository;
    this.guideContentCommentRepository = guideContentCommentRepository;
  }

  public Map<Integer, EngagementSummary> getEngagement(List<Integer> contentIds, UUID userId) {
    Map<Integer, EngagementSummary> result = new HashMap<>();
    if (contentIds == null || contentIds.isEmpty()) return result;

    Map<Integer, Long> likeCounts = new HashMap<>();
    for (GuideContentLikeRepository.CountView view :
        guideContentLikeRepository.countByContentIds(contentIds)) {
      likeCounts.put(view.getContentId(), view.getCnt());
    }

    Map<Integer, Long> commentCounts = new HashMap<>();
    for (GuideContentCommentRepository.CountView view :
        guideContentCommentRepository.countActiveByContentIds(contentIds)) {
      commentCounts.put(view.getContentId(), view.getCnt());
    }

    Set<Integer> likedIds = new HashSet<>();
    if (userId != null) {
      likedIds.addAll(guideContentLikeRepository.findLikedContentIds(userId, contentIds));
    }

    for (Integer contentId : contentIds) {
      long likes = likeCounts.getOrDefault(contentId, 0L);
      long comments = commentCounts.getOrDefault(contentId, 0L);
      boolean likedByMe = userId != null && likedIds.contains(contentId);
      result.put(contentId, new EngagementSummary(likes, comments, likedByMe));
    }

    return result;
  }
}
