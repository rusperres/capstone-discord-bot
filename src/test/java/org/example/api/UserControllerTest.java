package org.example.api;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.example.database.Classes.User;
import org.example.services.AuthService;
import org.example.services.BackendFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class UserControllerTest {

    @Mock
    private BackendFacade facade;

    @Mock
    private HttpExchange exchange;

    private UserController userController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        userController = new UserController(facade);

        // Mock a valid session for all tests
        Headers requestHeaders = new Headers();
        requestHeaders.add("Cookie", "sessionId=valid_session");
        when(exchange.getRequestHeaders()).thenReturn(requestHeaders);
        AuthService.UserSession mockSession = new AuthService.UserSession("123", "did", "username", "av", "token");
        when(facade.getSession("valid_session")).thenReturn(mockSession);
    }

    @Test
    public void testHandleProfile() throws IOException {
        User user = new User("123", "JDoe", "Dev", 5, 2);
        when(facade.getUser(123L)).thenReturn(user);
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/api/profile?id=123"));
        
        Headers responseHeaders = new Headers();
        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        userController.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        assertEquals("application/json", responseHeaders.getFirst("Content-Type"));
    }

    @Test
    public void testHandleMembers() throws IOException {
        User user = new User("123", "JDoe", "Dev", 5, 2);
        List<User> leaderboard = Collections.singletonList(user);
        when(facade.getLeaderboard("dev")).thenReturn(leaderboard);
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/api/user/members?type=dev"));
        
        Headers responseHeaders = new Headers();
        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        userController.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
    }

    @Test
    public void testHandleUpdateRole() throws IOException {
        String json = "{\"role\":\"QA\"}";
        when(exchange.getRequestMethod()).thenReturn("PATCH");
        when(exchange.getRequestURI()).thenReturn(URI.create("/api/user/123"));
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(json.getBytes()));
        
        Headers responseHeaders = new Headers();
        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        userController.handle(exchange);

        verify(facade).setUserRole(123L, "QA");
        verify(exchange).sendResponseHeaders(eq(200), anyLong());
    }
}
