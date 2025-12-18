package com.edufelip.meer.web;

import com.edufelip.meer.core.support.SupportContact;
import com.edufelip.meer.domain.repo.SupportContactRepository;
import com.edufelip.meer.dto.SupportContactRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/support")
public class SupportController {

    private static final Logger log = LoggerFactory.getLogger(SupportController.class);
    private static final Pattern SIMPLE_EMAIL_REGEX = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final SupportContactRepository repository;

    public SupportController(SupportContactRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/contact")
    public ResponseEntity<?> contact(@RequestBody(required = false) SupportContactRequest body) {
        String validationError = validate(body);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(Map.of("message", validationError));
        }

        repository.save(new SupportContact(body.name().trim(), body.email().trim(), body.message().trim()));

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
}
