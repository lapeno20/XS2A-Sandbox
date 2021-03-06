package de.adorsys.ledgers.oba.rest.server.auth.oba;

import de.adorsys.ledgers.middleware.api.domain.sca.SCALoginResponseTO;
import de.adorsys.ledgers.middleware.api.domain.um.AccessTokenTO;
import de.adorsys.ledgers.middleware.api.domain.um.BearerTokenTO;
import de.adorsys.ledgers.middleware.client.rest.UserMgmtRestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAuthenticationFilterTest {

    @InjectMocks
    private LoginAuthenticationFilter filter;

    @Mock
    private HttpServletRequest request = new MockHttpServletRequest();
    @Mock
    private HttpServletResponse response = new MockHttpServletResponse();
    @Mock
    private FilterChain chain;

    @Mock
    private UserMgmtRestClient userMgmtRestClient;

    @Test
    void doFilter() throws IOException, ServletException {
        // Given
        SecurityContextHolder.clearContext();
        when(request.getHeader("login")).thenReturn("anton.brueckner");
        when(request.getHeader("pin")).thenReturn("12345");
        when(userMgmtRestClient.authorise(anyString(), anyString(), any())).thenReturn(ResponseEntity.ok(getScaLoginResponse()));

        // When
        filter.doFilter(request, response, chain);

        // Then
        verify(userMgmtRestClient, times(1)).authorise(anyString(), anyString(), any());
    }

    @Test
    void doFilter_null_bearer() throws IOException, ServletException {
        // Given
        SecurityContextHolder.clearContext();

        // When
        filter.doFilter(request, response, chain);

        // Then
        verify(userMgmtRestClient, times(0)).validate(anyString());
        verify(chain, times(1)).doFilter(any(), any());
    }

    private SCALoginResponseTO getScaLoginResponse() {
        SCALoginResponseTO resp = new SCALoginResponseTO();
        AccessTokenTO token = new AccessTokenTO();
        token.setLogin("anton.brueckner");
        resp.setBearerToken(new BearerTokenTO(null, null, 600, null, token));
        return resp;
    }
}
