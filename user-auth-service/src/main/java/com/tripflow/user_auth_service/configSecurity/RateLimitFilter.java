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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

//Rate limiting sugli endpoint pubblici attaccabili a forza bruta o usabili per spam di email:
//finestra a scadenza per coppia (IP, endpoint), così un flusso normale (registrazione,
//verifica, login) non esaurisce un unico contatore condiviso.
//Il refresh non è limitato: richiede un refresh token valido e un 429 lì sloggherebbe l'app.
//Oltre il limite -> 429 con body ServiceError (scritto a mano: i filtri girano prima di Spring MVC).
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_TRACKED_KEYS = 1000;
    private static final Set<String> PERCORSI_LIMITATI = Set.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/verify-email",
            "/api/auth/resend-verification"
    );

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
        String uri = request.getRequestURI();
        if (!enabled || !"POST".equalsIgnoreCase(request.getMethod()) || !PERCORSI_LIMITATI.contains(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String chiave = ipDelRichiedente(request) + " " + uri;
        long now = System.currentTimeMillis();
        WindowCounter contatore = conteggi.computeIfAbsent(chiave, k -> new WindowCounter(now));

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

    //Il gateway accoda a X-Forwarded-For l'IP da cui ha ricevuto la richiesta: l'ultimo elemento
    //è quindi quello scritto da lui. I precedenti li manda il client e si possono falsificare.
    private String ipDelRichiedente(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] ips = forwarded.split(",");
            return ips[ips.length - 1].trim();
        }
        return request.getRemoteAddr();
    }

    private void rispondi429(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.warn("Rate limit superato per IP {} su {}", ipDelRichiedente(request), request.getRequestURI());
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ServiceError errore = new ServiceError(new Date(), request.getRequestURI(),
                "Troppe richieste: riprova tra qualche istante");
        objectMapper.writeValue(response.getWriter(), errore);
    }

    //Prune degli IP non più attivi solo quando la mappa cresce: evita memory leak senza overhead per richiesta.
    private void pulisciVociScadute(long now) {
        if (conteggi.size() <= MAX_TRACKED_KEYS) {
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