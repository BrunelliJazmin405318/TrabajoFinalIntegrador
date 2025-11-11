package ar.edu.utn.tfi.service;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationHub {

    // key = nroOrden, value = lista de suscriptores SSE
    private final Map<String, Set<SseEmitter>> subs = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String nroOrden) {
        var emitter = new SseEmitter(0L); // sin timeout
        subs.computeIfAbsent(nroOrden, k -> Collections.synchronizedSet(new HashSet<>())).add(emitter);

        emitter.onCompletion(() -> remove(nroOrden, emitter));
        emitter.onTimeout(() -> remove(nroOrden, emitter));
        emitter.onError(e -> remove(nroOrden, emitter));

        // mensaje inicial (handshake)
        try { emitter.send(SseEmitter.event().name("connected").data("ok")); } catch (IOException ignored) {}
        return emitter;
    }

    public void push(String nroOrden, String eventName, Object payload) {
        var set = subs.getOrDefault(nroOrden, Set.of());
        var dead = new ArrayList<SseEmitter>();
        for (var em : set) {
            try { em.send(SseEmitter.event().name(eventName).data(payload)); }
            catch (IOException e) { dead.add(em); }
        }
        dead.forEach(em -> remove(nroOrden, em));
    }

    private void remove(String nroOrden, SseEmitter em) {
        var set = subs.get(nroOrden);
        if (set != null) {
            set.remove(em);
            if (set.isEmpty()) subs.remove(nroOrden);
        }
    }
}