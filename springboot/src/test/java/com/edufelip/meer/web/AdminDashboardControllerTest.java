package com.edufelip.meer.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edufelip.meer.config.TestClockConfig;
import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.auth.Role;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.core.content.GuideContentComment;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.GuideContentSummary;
import com.edufelip.meer.domain.repo.GuideContentCommentRepository;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import com.edufelip.meer.security.DashboardAdminAuthorizer;
import com.edufelip.meer.service.GuideContentEngagementService;
import com.edufelip.meer.service.GuideContentModerationService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminDashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({TestClockConfig.class, DashboardAdminAuthorizer.class})
class AdminDashboardControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private GuideContentRepository guideContentRepository;
  @MockitoBean private GuideContentCommentRepository guideContentCommentRepository;
  @MockitoBean private GuideContentEngagementService guideContentEngagementService;
  @MockitoBean private GuideContentModerationService guideContentModerationService;

  @Test
  void listContentsIncludesCounts() throws Exception {
    UUID adminId = UUID.randomUUID();
    AuthUser admin = new AuthUser();
    admin.setId(adminId);
    admin.setRole(Role.ADMIN);
    admin.setEmail("admin@example.com");

    GuideContentSummary summary =
        new GuideContentSummary(
            10,
            "Guide",
            "Desc",
            "https://example.com/image.jpg",
            null,
            null,
            null,
            java.time.Instant.parse("2024-01-01T00:00:00Z"));

    when(guideContentRepository.findAllSummariesActive(org.mockito.ArgumentMatchers.any()))
        .thenReturn(new SliceImpl<>(List.of(summary)));
    when(guideContentEngagementService.getEngagement(eq(List.of(10)), eq(null)))
        .thenReturn(Map.of(10, new GuideContentEngagementService.EngagementSummary(5L, 7L, false)));

    mockMvc
        .perform(
            get("/dashboard/contents")
                .header("Authorization", "Bearer admin-token")
                .requestAttr("adminUser", admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].likeCount").value(5))
        .andExpect(jsonPath("$.items[0].commentCount").value(7));
  }

  @Test
  void getContentIncludesCounts() throws Exception {
    UUID adminId = UUID.randomUUID();
    AuthUser admin = new AuthUser();
    admin.setId(adminId);
    admin.setRole(Role.ADMIN);
    admin.setEmail("admin@example.com");

    GuideContent content = new GuideContent();
    content.setId(10);

    when(guideContentRepository.findByIdAndDeletedAtIsNull(10)).thenReturn(Optional.of(content));
    when(guideContentEngagementService.getEngagement(eq(List.of(10)), eq(null)))
        .thenReturn(Map.of(10, new GuideContentEngagementService.EngagementSummary(3L, 4L, false)));

    mockMvc
        .perform(
            get("/dashboard/contents/{id}", 10)
                .header("Authorization", "Bearer admin-token")
                .requestAttr("adminUser", admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.likeCount").value(3))
        .andExpect(jsonPath("$.commentCount").value(4));
  }

  @Test
  void listContentCommentsReturnsPage() throws Exception {
    UUID adminId = UUID.randomUUID();
    AuthUser admin = new AuthUser();
    admin.setId(adminId);
    admin.setRole(Role.ADMIN);
    admin.setEmail("admin@example.com");

    GuideContent content = new GuideContent();
    content.setId(10);

    AuthUser commenter = new AuthUser();
    commenter.setId(UUID.randomUUID());
    commenter.setDisplayName("Jane");

    GuideContentComment comment = new GuideContentComment(commenter, content, "Nice!");
    comment.setId(99);

    when(guideContentRepository.findById(10)).thenReturn(Optional.of(content));
    when(guideContentCommentRepository.findByContentId(eq(10), org.mockito.ArgumentMatchers.any()))
        .thenReturn(new PageImpl<>(List.of(comment)));

    mockMvc
        .perform(
            get("/dashboard/contents/{id}/comments", 10)
                .header("Authorization", "Bearer admin-token")
                .requestAttr("adminUser", admin))
        .andExpect(status().isOk());
  }

  @Test
  void adminCanHardDeleteContentComment() throws Exception {
    UUID adminId = UUID.randomUUID();
    AuthUser admin = new AuthUser();
    admin.setId(adminId);
    admin.setRole(Role.ADMIN);
    admin.setEmail("admin@example.com");

    GuideContent content = new GuideContent();
    content.setId(10);

    AuthUser commenter = new AuthUser();
    commenter.setId(UUID.randomUUID());

    GuideContentComment comment = new GuideContentComment(commenter, content, "Spam");
    comment.setId(99);

    when(guideContentCommentRepository.findById(99)).thenReturn(Optional.of(comment));

    mockMvc
        .perform(
            delete("/dashboard/contents/{id}/comments/{commentId}", 10, 99)
                .header("Authorization", "Bearer admin-token")
                .requestAttr("adminUser", admin)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    verify(guideContentModerationService, times(1)).hardDeleteComment(comment);
  }

  @Test
  void listDashboardCommentsSupportsFilters() throws Exception {
    UUID adminId = UUID.randomUUID();
    AuthUser admin = new AuthUser();
    admin.setId(adminId);
    admin.setRole(Role.ADMIN);
    admin.setEmail("admin@example.com");

    ThriftStore store = new ThriftStore();
    store.setId(UUID.randomUUID());
    store.setName("Loja");

    GuideContent content = new GuideContent();
    content.setId(10);
    content.setTitle("Guia");
    content.setThriftStore(store);

    AuthUser commenter = new AuthUser();
    commenter.setId(UUID.randomUUID());
    commenter.setDisplayName("Jane");

    GuideContentComment comment = new GuideContentComment(commenter, content, "Nice!");
    comment.setId(99);

    when(guideContentCommentRepository.findDashboardComments(
            eq(10),
            eq(store.getId()),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(new PageImpl<>(List.of(comment)));

    mockMvc
        .perform(
            get("/dashboard/comments")
                .header("Authorization", "Bearer admin-token")
                .requestAttr("adminUser", admin)
                .param("contentId", "10")
                .param("storeId", store.getId().toString())
                .param("from", "2024-01-01")
                .param("to", "2024-01-31")
                .param("search", " Guia ")
                .param("sort", "oldest"))
        .andExpect(status().isOk());

    ArgumentCaptor<String> searchCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<org.springframework.data.domain.Pageable> pageableCaptor =
        ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
    verify(guideContentCommentRepository)
        .findDashboardComments(
            eq(10),
            eq(store.getId()),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            searchCaptor.capture(),
            pageableCaptor.capture());
    org.assertj.core.api.Assertions.assertThat(searchCaptor.getValue()).isEqualTo("Guia");
    org.assertj.core.api.Assertions.assertThat(
            pageableCaptor.getValue().getSort().getOrderFor("createdAt").getDirection())
        .isEqualTo(org.springframework.data.domain.Sort.Direction.ASC);
  }
}
