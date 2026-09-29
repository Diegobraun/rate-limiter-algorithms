package br.com.diegobraun.ratelimiter.web;

import br.com.diegobraun.ratelimiter.core.Algorithm;
import br.com.diegobraun.ratelimiter.core.RateLimitDecision;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    static final String CLIENT_ID_HEADER = "X-Client-Id";

    private final RateLimiterRegistry registry;
    private final ObjectMapper objectMapper;

    public RateLimitInterceptor(RateLimiterRegistry registry, ObjectMapper objectMapper) {
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Algorithm algorithm = Algorithm.fromSlug(pathVariable(request, "algorithm"));
        RateLimitDecision decision = registry.get(algorithm).tryAcquire(clientKey(request));

        response.setHeader("X-RateLimit-Algorithm", algorithm.slug());
        response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));

        if (!decision.allowed()) {
            long retryAfterSeconds = Math.max(1, (long) Math.ceil(decision.retryAfter().toMillis() / 1000.0));
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), Map.of(
                    "error", "Too Many Requests",
                    "algorithm", algorithm.slug(),
                    "retryAfterMs", decision.retryAfter().toMillis()));
            return false;
        }

        if (!decision.delay().isZero()) {
            response.setHeader("X-RateLimit-Delay-Ms", String.valueOf(decision.delay().toMillis()));
            Thread.sleep(decision.delay());
        }
        return true;
    }

    private static String clientKey(HttpServletRequest request) {
        String clientId = request.getHeader(CLIENT_ID_HEADER);
        return clientId != null && !clientId.isBlank() ? clientId : request.getRemoteAddr();
    }

    @SuppressWarnings("unchecked")
    private static String pathVariable(HttpServletRequest request, String name) {
        Map<String, String> variables =
                (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        return variables == null ? "" : variables.getOrDefault(name, "");
    }
}
