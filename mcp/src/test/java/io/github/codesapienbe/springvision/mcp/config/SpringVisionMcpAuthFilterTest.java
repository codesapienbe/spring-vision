package io.github.codesapienbe.springvision.mcp.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Unit tests for {@link SpringVisionMcpAuthFilter}.
 */
@ExtendWith(MockitoExtension.class)
class SpringVisionMcpAuthFilterTest {

    private static final String TOKEN = "test-token-12345";

    @Nested
    @DisplayName("Constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("Should reject a null token")
        void shouldRejectNullToken() {
            assertThatThrownBy(() -> new SpringVisionMcpAuthFilter(null))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Should reject a blank token")
        void shouldRejectBlankToken() {
            assertThatThrownBy(() -> new SpringVisionMcpAuthFilter("   "))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Should accept a non-blank token")
        void shouldAcceptNonBlankToken() {
            assertThat(new SpringVisionMcpAuthFilter(TOKEN)).isNotNull();
        }
    }

    @Nested
    @DisplayName("shouldNotFilter")
    class ShouldNotFilter {

        private final SpringVisionMcpAuthFilter filter = new SpringVisionMcpAuthFilter(TOKEN);

        @Test
        @DisplayName("Should filter requests to /mcp")
        void shouldFilterMcpRequests() {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
            assertThat(filter.shouldNotFilter(request)).isFalse();
        }

        @Test
        @DisplayName("Should not filter requests outside /mcp")
        void shouldNotFilterOtherRequests() {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
            assertThat(filter.shouldNotFilter(request)).isTrue();
        }
    }

    @Nested
    @DisplayName("doFilterInternal")
    class DoFilterInternal {

        private final SpringVisionMcpAuthFilter filter = new SpringVisionMcpAuthFilter(TOKEN);

        @Test
        @DisplayName("Should pass through with a matching bearer token")
        void shouldPassThroughWithMatchingToken() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
            request.addHeader("Authorization", "Bearer " + TOKEN);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        }

        @Test
        @DisplayName("Should reject a mismatched bearer token")
        void shouldRejectMismatchedToken() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
            request.addHeader("Authorization", "Bearer wrong-token");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should reject a missing Authorization header")
        void shouldRejectMissingHeader() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should reject a non-Bearer Authorization header")
        void shouldRejectNonBearerHeader() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
            request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("Mocked chain interactions")
    class MockedChainInteractions {

        @Mock
        private HttpServletRequest request;

        @Mock
        private HttpServletResponse response;

        @Mock
        private FilterChain chain;

        @Test
        @DisplayName("Should invoke the chain exactly once on success")
        void shouldInvokeChainOnceOnSuccess() throws Exception {
            SpringVisionMcpAuthFilter filter = new SpringVisionMcpAuthFilter(TOKEN);
            when(request.getRequestURI()).thenReturn("/mcp");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + TOKEN);

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should never invoke the chain when unauthorized")
        void shouldNeverInvokeChainWhenUnauthorized() throws Exception {
            SpringVisionMcpAuthFilter filter = new SpringVisionMcpAuthFilter(TOKEN);
            when(request.getRequestURI()).thenReturn("/mcp");
            when(request.getHeader("Authorization")).thenReturn(null);

            filter.doFilter(request, response, chain);

            verifyNoInteractions(chain);
        }
    }
}
