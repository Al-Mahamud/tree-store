package com.shobuj.store.shared;

import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Hands out the CSRF token (and sets its cookie) before the first write. */
@RestController
class CsrfController {

    @GetMapping("/api/v1/csrf")
    Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }
}
