package com.tripflow.user_auth_service.configSecurity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripflow.user_auth_service.dto.errors.ServiceError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//Rate limiting per POST /api/auth/register (unico endpoint pubblico): finestra a scadenza per IP.
//Oltre il limite -> 429 con body ServiceError (scritto a mano: i filtri girano prima di Spring MVC).
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_TRACKED_IPS = 1000;
    private static final String REGISTER_PATH = "/api/auth/register";

    private final boolean enabled;
    private final int maxRequestsPerWindow;
    private final long windowMillis;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, WindowCounter> conteggi = new ConcurrentHashMap<>();

    public RateLimitFilter(boolean enabled, int maxRequestsPerWindow, int windowSeconds) {
        this.enabled = enabled;
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.windowMillis = windowSeconds * 1000L;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!enabled || !"POST".equalsIgnoreCase(request.getMethod()) || !REGISTER_PATH.equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = ipDelRichiedente(request);
        long now = System.currentTimeMillis();
        WindowCounter contatore = conteggi.computeIfAbsent(ip, k -> new WindowCounter(now));

        synchronized (contatore) {
            if (now - contatore.windowStart >= windowMillis) {
                contatore.windowStart = now;
                contatore.count = 0;
            }
            contatore.count++;
            if (contatore.count > maxRequestsPerWindow) {
                rispondi429(request, response);
                return;
            }
        }

        pulisciVociScadute(now);
        filterChain.doFilter(request, response);
    }

    //Dentro la rete Docker (gateway) l'IP reale del client è nel primo elemento di X-Forwarded-For.
    private String ipDelRichiedente(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void rispondi429(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.warn("Rate limit superato per IP {} su {}", request.getRemoteAddr(), request.getRequestURI());
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ServiceError errore = new ServiceError(new Date(), request.getRequestURI(),
                "Troppe richieste: riprova tra qualche istante");
        objectMapper.writeValue(response.getWriter(), errore);
    }

    //Prune degli IP non più attivi solo quando la mappa cresce: evita memory leak senza overhead per richiesta.
    private void pulisciVociScadute(long now) {
        if (conteggi.size() <= MAX_TRACKED_IPS) {
            return;
        }
        conteggi.entrySet().removeIf(entry -> now - entry.getValue().windowStart >= windowMillis);
    }

    private static class WindowCounter {
        long windowStart;
        int count;

        WindowCounter(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}