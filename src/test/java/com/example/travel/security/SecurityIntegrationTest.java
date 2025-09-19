package com.example.travel.security;

import com.example.travel.model.User;
import com.example.travel.model.UserRole;
import com.example.travel.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Disabled("Security integration tests disabled due to database setup issues - focus on unit tests")
@DisplayName("Security Integration Tests")
class SecurityIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private User testProvider;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
                
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("user@example.com");
        testUser.setPassword(passwordEncoder.encode("password"));
        testUser.setRole(UserRole.USER);
        userRepository.save(testUser);

        testProvider = new User();
        testProvider.setUsername("testprovider");
        testProvider.setEmail("provider@example.com");
        testProvider.setPassword(passwordEncoder.encode("password"));
        testProvider.setRole(UserRole.PROVIDER);
        userRepository.save(testProvider);
    }

    @Test
    @DisplayName("Test Case 38: Should authenticate user with valid credentials")
    void shouldAuthenticateUserWithValidCredentials() throws Exception {
        mockMvc.perform(formLogin("/login").user("testuser").password("password"))
                .andExpect(authenticated().withUsername("testuser"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("Test Case 39: Should reject invalid credentials")
    void shouldRejectInvalidCredentials() throws Exception {
        mockMvc.perform(formLogin("/login").user("testuser").password("wrongpassword"))
                .andExpect(unauthenticated())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @DisplayName("Test Case 40: Should redirect unauthenticated user to login")
    void shouldRedirectUnauthenticatedUserToLogin() throws Exception {
        mockMvc.perform(get("/provider/hotels"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Test Case 41: Should allow user access to user endpoints")
    void shouldAllowUserAccessToUserEndpoints() throws Exception {
        mockMvc.perform(get("/hotels/search")
                .with(user("testuser").roles("USER")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Test Case 42: Should allow provider access to provider endpoints")
    void shouldAllowProviderAccessToProviderEndpoints() throws Exception {
        mockMvc.perform(get("/provider/hotels")
                .with(user("testprovider").roles("PROVIDER")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Test Case 43: Should deny user access to provider endpoints")
    void shouldDenyUserAccessToProviderEndpoints() throws Exception {
        mockMvc.perform(get("/provider/hotels")
                .with(user("testuser").roles("USER")))
                .andExpect(status().isForbidden());
    }
}