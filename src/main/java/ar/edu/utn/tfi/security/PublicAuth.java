package ar.edu.utn.tfi.security;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class PublicAuth {

    public static Long solicitudIdFromTokenOrNull() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a.getDetails() == null) return null;
        if (!(a.getDetails() instanceof Claims c)) return null;

        Object sid = c.get("solicitudId");
        if (sid == null) return null;

        if (sid instanceof Integer i) return i.longValue();
        if (sid instanceof Long l) return l;
        if (sid instanceof String s) return Long.valueOf(s);

        return null;
    }
}