package ar.edu.utn.frc.tup.piii.security;

import ar.edu.utn.frc.tup.piii.entities.Player;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Paths that are permitAll regardless of HTTP method. */
    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/auth/register",
        "/api/auth/login",
        "/api/auth/forgot-password",
        "/api/auth/reset-password",
        "/ping",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/v3/api-docs/**",
        "/h2-console/**"
    );

    /** Paths that are permitAll only for GET (e.g. /api/cards). */
    private static final List<String> PUBLIC_GET_PATHS = List.of(
        "/api/cards/**"
    );

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final JwtService jwtService;
    private final PlayerRepository playerRepository;

    public JwtAuthenticationFilter(JwtService jwtService, PlayerRepository playerRepository) {
        this.jwtService = jwtService;
        this.playerRepository = playerRepository;
    }

    /**
     * Skip the filter entirely for public endpoints so that even if the
     * frontend sends an Authorization header, the token is never processed.
     * This prevents JPA entity loading and SecurityContext contamination
     * on publicly-accessible routes.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        boolean matchesPublic = PUBLIC_PATHS.stream()
                .anyMatch(p -> PATH_MATCHER.match(p, path));
        if (matchesPublic) {
            return true;
        }

        if ("GET".equalsIgnoreCase(request.getMethod())) {
            return PUBLIC_GET_PATHS.stream()
                    .anyMatch(p -> PATH_MATCHER.match(p, path));
        }

        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // SockJS/WebSocket handshakes cannot send headers from the browser, so the
        // token travels as a query param (?token=) — accepted ONLY on /ws/** to avoid
        // leaking tokens into server logs for regular API calls.
        String token = null;
        if (request.getRequestURI().startsWith("/ws")) {
            String paramToken = request.getParameter("token");
            if (paramToken != null && !paramToken.isBlank()) {
                token = paramToken;
            }
        }

        if (token == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }
            token = authHeader.substring(7);
        }

        if (!jwtService.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = jwtService.extractUsername(token);
        Optional<Player> playerOpt = playerRepository.findByUsername(username);

        if (playerOpt.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        Player player = playerOpt.get();
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(player, null, Collections.emptyList());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }
}
