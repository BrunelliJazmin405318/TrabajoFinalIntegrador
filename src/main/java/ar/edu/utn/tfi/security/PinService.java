package ar.edu.utn.tfi.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Service
public class PinService {

    private static final SecureRandom RNG = new SecureRandom();
    private static final BCryptPasswordEncoder ENC = new BCryptPasswordEncoder();

    // Ajustes prod (podés cambiarlos después)
    public static final int MAX_FAILS = 5;
    public static final Duration LOCK_TIME = Duration.ofMinutes(15);
    public static final Duration PIN_TTL   = Duration.ofDays(90);

    public String generarPin6() {
        int n = RNG.nextInt(1_000_000); // 0..999999
        return String.format("%06d", n);
    }

    public String hashPin(String pin) {
        return ENC.encode(pin);
    }

    public boolean matches(String pin, String hash) {
        if (pin == null || hash == null) return false;
        return ENC.matches(pin, hash);
    }

    public Instant now() { return Instant.now(); }

    public Instant expiresAtFromNow() {
        return now().plus(PIN_TTL);
    }

    public Instant lockUntilFromNow() {
        return now().plus(LOCK_TIME);
    }
}