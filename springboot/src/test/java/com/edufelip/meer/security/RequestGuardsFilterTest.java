package com.edufelip.meer.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.edufelip.meer.security.guards.AppHeaderGuard;
import com.edufelip.meer.security.guards.FirebaseAuthGuard;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestGuardsFilterTest {

  @Test
  void publicPathsBypassGuards() throws ServletException, IOException {
    SecurityProperties props = new SecurityProperties();
    props.setRequireAppHeader(true);
    props.setDisableAuth(false);
    props.setAppPackage("com.edufelip.meer");

    RequestGuardsFilter filter = new RequestGuardsFilter(props);
    AtomicBoolean chainCalled = new AtomicBoolean(false);
    FilterChain chain =
        (request, response) -> {
          chainCalled.set(true);
        };

    MockHttpServletRequest loginRequest = new MockHttpServletRequest("POST", "/auth/login");
    loginRequest.setServletPath("/auth/login");
    MockHttpServletResponse loginResponse = new MockHttpServletResponse();
    filter.doFilter(loginRequest, loginResponse, chain);
    assertThat(chainCalled.get()).isTrue();

    chainCalled.set(false);
    MockHttpServletRequest contentsRequest = new MockHttpServletRequest("GET", "/contents");
    contentsRequest.setServletPath("/contents");
    MockHttpServletResponse contentsResponse = new MockHttpServletResponse();
    filter.doFilter(contentsRequest, contentsResponse, chain);
    assertThat(chainCalled.get()).isTrue();
  }

  @Test
  void nonPublicPathsRequireHeaders() throws ServletException, IOException {
    SecurityProperties props = new SecurityProperties();
    props.setRequireAppHeader(true);
    props.setDisableAuth(false);
    props.setAppPackage("com.edufelip.meer");

    RequestGuardsFilter filter = new RequestGuardsFilter(props);
    AtomicBoolean chainCalled = new AtomicBoolean(false);
    FilterChain chain =
        (request, response) -> {
          chainCalled.set(true);
        };

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stores");
    request.setServletPath("/stores");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    assertThat(chainCalled.get()).isFalse();
    assertThat(response.getStatus()).isEqualTo(401);
  }

  @Test
  void validHeadersAllowNonPublicRequests() throws ServletException, IOException {
    SecurityProperties props = new SecurityProperties();
    props.setRequireAppHeader(true);
    props.setDisableAuth(false);
    props.setAppPackage("com.edufelip.meer");

    RequestGuardsFilter filter = new RequestGuardsFilter(props);
    AtomicBoolean chainCalled = new AtomicBoolean(false);
    FilterChain chain =
        (request, response) -> {
          chainCalled.set(true);
        };

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stores");
    request.setServletPath("/stores");
    request.addHeader(AppHeaderGuard.APP_HEADER, "com.edufelip.meer");
    request.addHeader(FirebaseAuthGuard.AUTH_HEADER, "Bearer token");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    assertThat(chainCalled.get()).isTrue();
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void missingAppHeaderIsRejected() throws ServletException, IOException {
    SecurityProperties props = new SecurityProperties();
    props.setRequireAppHeader(true);
    props.setDisableAuth(false);
    props.setAppPackage("com.edufelip.meer");

    RequestGuardsFilter filter = new RequestGuardsFilter(props);
    AtomicBoolean chainCalled = new AtomicBoolean(false);
    FilterChain chain =
        (request, response) -> {
          chainCalled.set(true);
        };

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stores");
    request.setServletPath("/stores");
    request.addHeader(FirebaseAuthGuard.AUTH_HEADER, "Bearer token");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    assertThat(chainCalled.get()).isFalse();
    assertThat(response.getStatus()).isEqualTo(401);
  }

  @Test
  void wrongAppHeaderIsRejected() throws ServletException, IOException {
    SecurityProperties props = new SecurityProperties();
    props.setRequireAppHeader(true);
    props.setDisableAuth(false);
    props.setAppPackage("com.edufelip.meer");

    RequestGuardsFilter filter = new RequestGuardsFilter(props);
    AtomicBoolean chainCalled = new AtomicBoolean(false);
    FilterChain chain =
        (request, response) -> {
          chainCalled.set(true);
        };

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stores");
    request.setServletPath("/stores");
    request.addHeader(AppHeaderGuard.APP_HEADER, "com.other.app");
    request.addHeader(FirebaseAuthGuard.AUTH_HEADER, "Bearer token");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    assertThat(chainCalled.get()).isFalse();
    assertThat(response.getStatus()).isEqualTo(401);
  }

  @Test
  void malformedBearerIsRejected() throws ServletException, IOException {
    SecurityProperties props = new SecurityProperties();
    props.setRequireAppHeader(true);
    props.setDisableAuth(false);
    props.setAppPackage("com.edufelip.meer");

    RequestGuardsFilter filter = new RequestGuardsFilter(props);
    AtomicBoolean chainCalled = new AtomicBoolean(false);
    FilterChain chain =
        (request, response) -> {
          chainCalled.set(true);
        };

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stores");
    request.setServletPath("/stores");
    request.addHeader(AppHeaderGuard.APP_HEADER, "com.edufelip.meer");
    request.addHeader(FirebaseAuthGuard.AUTH_HEADER, "Bearer");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    assertThat(chainCalled.get()).isFalse();
    assertThat(response.getStatus()).isEqualTo(401);
  }

  @Test
  void disableAuthSkipsBearerValidationButStillRequiresAppHeader() throws Exception {
    SecurityProperties props = new SecurityProperties();
    props.setRequireAppHeader(true);
    props.setDisableAuth(true);
    props.setAppPackage("com.edufelip.meer");

    RequestGuardsFilter filter = new RequestGuardsFilter(props);
    AtomicBoolean chainCalled = new AtomicBoolean(false);
    FilterChain chain =
        (request, response) -> {
          chainCalled.set(true);
        };

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stores");
    request.setServletPath("/stores");
    request.addHeader(AppHeaderGuard.APP_HEADER, "com.edufelip.meer");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    assertThat(chainCalled.get()).isTrue();
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void disableAuthAndNoAppHeaderAllowsRequestWhenHeaderNotRequired() throws Exception {
    SecurityProperties props = new SecurityProperties();
    props.setRequireAppHeader(false);
    props.setDisableAuth(true);
    props.setAppPackage("com.edufelip.meer");

    RequestGuardsFilter filter = new RequestGuardsFilter(props);
    AtomicBoolean chainCalled = new AtomicBoolean(false);
    FilterChain chain =
        (request, response) -> {
          chainCalled.set(true);
        };

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stores");
    request.setServletPath("/stores");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    assertThat(chainCalled.get()).isTrue();
    assertThat(response.getStatus()).isEqualTo(200);
  }
}
