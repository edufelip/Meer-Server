package com.edufelip.meer.web;

import com.edufelip.meer.core.support.SupportContact;
import com.edufelip.meer.domain.port.RateLimitPort;
import com.edufelip.meer.domain.repo.SupportContactRepository;
import com.edufelip.meer.dto.SupportContactRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/support")
public class SupportController {

  private static final Logger log = LoggerFactory.getLogger(SupportController.class);
  private static final Pattern SIMPLE_EMAIL_REGEX =
      Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  private final SupportContactRepository repository;
  private final RateLimitPort rateLimitService;

  public SupportController(SupportContactRepository repository, RateLimitPort rateLimitService) {
    this.repository = repository;
    this.rateLimitService = rateLimitService;
  }

  @PostMapping("/contact")
  public ResponseEntity<?> contact(
      @RequestBody(required = false) SupportContactRequest body, HttpServletRequest request) {
    String validationError = validate(body);
    if (validationError != null) {
      return ResponseEntity.badRequest().body(Map.of("message", validationError));
    }
    String clientKey = resolveClientKey(request);
    if (!rateLimitService.allowSupportContact(clientKey)) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
          .body(Map.of("message", "Too many support requests"));
    }

    repository.save(
        new SupportContact(body.name().trim(), body.email().trim(), body.message().trim()));

    // Keep for observability until a helpdesk/email sink is wired.
    log.info("Support contact received from '{}' <{}>", body.name(), body.email());

    return ResponseEntity.noContent().build();
  }

  private String validate(SupportContactRequest body) {
    if (body == null) return "Request body is required";
    if (isBlank(body.name())) return "Name is required";
    if (isBlank(body.email())) return "Email is required";
    if (!SIMPLE_EMAIL_REGEX.matcher(body.email().trim()).matches()) return "Email is invalid";
    if (isBlank(body.message())) return "Message is required";
    return null;
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private String resolveClientKey(HttpServletRequest request) {
    if (request == null) return "unknown";
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    String realIp = request.getHeader("X-Real-IP");
    if (realIp != null && !realIp.isBlank()) {
      return realIp.trim();
    }
    return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
  }
}
