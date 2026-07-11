package com.cris.sysmonitor;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    // ── Change this to set the dashboard password ──
    private static final String PASSWORD = "cris@2026";

    // In-memory token store. Tokens survive until the JAR is restarted.
    private final Set<String> validTokens =
            Collections.synchronizedSet(new HashSet<String>());

    /**
     * Validates the password and, if correct, issues a new session token.
     * @return token string on success, null on failure
     */
    public String login(String password) {
        if (PASSWORD.equals(password)) {
            String token = UUID.randomUUID().toString();
            validTokens.add(token);
            return token;
        }
        return null;
    }

    /**
     * Returns true if the token was issued by this instance and has not been revoked.
     */
    public boolean isValidToken(String token) {
        return token != null && validTokens.contains(token);
    }

    /**
     * Invalidates a token (logout).
     */
    public void logout(String token) {
        validTokens.remove(token);
    }
}
