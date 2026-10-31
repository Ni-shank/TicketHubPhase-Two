
package com.tickethub.booking.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final Pattern BOOKING_PATTERN =
            Pattern.compile("/bookings/(\\d+)(?:/|$)");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String requestId = request.getHeader(REQUEST_ID_HEADER);

        if (requestId == null ||
                !requestId.matches("[A-Za-z0-9._-]{1,64}")) {
            requestId = UUID.randomUUID().toString();
        }

        MDC.put("requestId", requestId);

        Matcher matcher =
                BOOKING_PATTERN.matcher(request.getRequestURI());

        if (matcher.find()) {
            MDC.put("bookingId", matcher.group(1));
        }

        response.setHeader(REQUEST_ID_HEADER, requestId);

        long start = System.currentTimeMillis();

        try {
            logRequest(request);
            filterChain.doFilter(request, response);
        } finally {
            long duration =
                    System.currentTimeMillis() - start;

            org.slf4j.LoggerFactory
                    .getLogger(RequestLoggingFilter.class)
                    .info("Request completed method={} path={} status={} durationMs={}",
                            request.getMethod(),
                            request.getRequestURI(),
                            response.getStatus(),
                            duration);

            MDC.remove("requestId");
            MDC.remove("bookingId");
        }
    }

    private void logRequest(HttpServletRequest request) {
        org.slf4j.LoggerFactory
                .getLogger(RequestLoggingFilter.class)
                .info("Incoming request method={} path={}",
                        request.getMethod(),
                        request.getRequestURI());
    }
}
