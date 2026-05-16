package org.example.api;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.example.services.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private HttpExchange exchange;

    private AuthController authController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        authController = new AuthController(authService);
    }

    @Test
    public void testHandleLogin() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/api/auth/login?id=user123"));
        when(authService.generateState(anyString(), eq("user123"))).thenReturn("test_state");
        when(authService.getOAuthUrl("test_state")).thenReturn("https://discord.com/oauth");

        Headers headers = new Headers();
        when(exchange.getResponseHeaders()).thenReturn(headers);

        authController.handle(exchange);

        assertEquals("https://discord.com/oauth", headers.getFirst("Location"));
        verify(exchange).sendResponseHeaders(eq(302), eq(-1L));
    }

    @Test
    public void testHandleCallbackSuccess() throws IOException, InterruptedException {
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/api/auth/callback?code=test_code&state=test_state"));
        when(authService.getPendingData("test_state")).thenReturn("session123:user123");
        when(authService.exchangeCodeForToken("test_code")).thenReturn("test_token");
        
        AuthService.UserSession session = new AuthService.UserSession("user123", "dId", "dName", "dAv", "test_token");
        when(authService.fetchUserInfo("test_token", "user123")).thenReturn(session);

        Headers headers = new Headers();
        when(exchange.getResponseHeaders()).thenReturn(headers);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        authController.handle(exchange);

        verify(authService).createSession(eq("session123"), any());
        assertTrue(headers.getFirst("Set-Cookie").contains("sessionId=session123"));
        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        assertEquals("{\"authenticated\":true}", os.toString());
    }

    @Test
    public void testHandleCallbackInvalidState() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/api/auth/callback?code=test_code&state=invalid_state"));
        when(authService.getPendingData("invalid_state")).thenReturn(null);

        Headers headers = new Headers();
        when(exchange.getResponseHeaders()).thenReturn(headers);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        authController.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(403), anyLong());
        assertTrue(os.toString().contains("Invalid or expired state"));
    }
}
