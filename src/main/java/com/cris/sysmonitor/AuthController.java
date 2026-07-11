package com.cris.sysmonitor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * POST /auth/login
     * Body: { "password": "..." }
     * Returns: { "token": "uuid" } on success, 401 on failure
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String password = body.get("password");
        String token = authService.login(password);
        if (token != null) {
            return ResponseEntity.ok(Collections.singletonMap("token", token));
        }
        return ResponseEntity.status(401)
                .body(Collections.singletonMap("error", "Invalid password"));
    }

    /**
     * POST /auth/logout
     * Header: Authorization: Bearer <token>
     * Invalidates the token server-side.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7));
        }
        return ResponseEntity.ok(Collections.singletonMap("status", "logged out"));
    }
}
