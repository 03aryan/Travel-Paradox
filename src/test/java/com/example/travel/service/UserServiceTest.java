package com.example.travel.service;

import com.example.travel.model.User;
import com.example.travel.model.UserRole;
import com.example.travel.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(UserRole.USER);
    }

    @Test
    @DisplayName("Test Case 1: Should successfully register a new user")
    void shouldRegisterNewUser() {
        // Given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.registerUser("testuser", "test@example.com", "password", UserRole.USER);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getRole()).isEqualTo(UserRole.USER);
        
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(passwordEncoder).encode("password");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Test Case 2: Should throw exception when username already exists")
    void shouldThrowExceptionWhenUsernameExists() {
        // Given
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> 
            userService.registerUser("testuser", "test@example.com", "password", UserRole.USER))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Username already exists");
        
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Test Case 3: Should throw exception when email already exists")
    void shouldThrowExceptionWhenEmailExists() {
        // Given
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> 
            userService.registerUser("testuser", "test@example.com", "password", UserRole.USER))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Email already exists");
        
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Test Case 4: Should find user by username")
    void shouldFindUserByUsername() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findByUsername("testuser");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
        
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Test Case 5: Should return empty when user not found by username")
    void shouldReturnEmptyWhenUserNotFoundByUsername() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.findByUsername("nonexistent");

        // Then
        assertThat(result).isEmpty();
        
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("Test Case 6: Should get all providers")
    void shouldGetAllProviders() {
        // Given
        User provider1 = new User();
        provider1.setId(2L);
        provider1.setUsername("provider1");
        provider1.setRole(UserRole.PROVIDER);
        
        User provider2 = new User();
        provider2.setId(3L);
        provider2.setUsername("provider2");
        provider2.setRole(UserRole.PROVIDER);
        
        List<User> providers = Arrays.asList(provider1, provider2);
        when(userRepository.findByRole(UserRole.PROVIDER)).thenReturn(providers);

        // When
        List<User> result = userService.listProviders();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(user -> user.getRole() == UserRole.PROVIDER);
        
        verify(userRepository).findByRole(UserRole.PROVIDER);
    }

    @Test
    @DisplayName("Test Case 7: Should validate password correctly")
    void shouldValidatePasswordCorrectly() {
        // Given
        when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);

        // When
        boolean result = userService.checkPassword(testUser, "rawPassword");

        // Then
        assertThat(result).isTrue();
        
        verify(passwordEncoder).matches("rawPassword", testUser.getPassword());
    }

    @Test
    @DisplayName("Test Case 8: Should return false for invalid password")
    void shouldReturnFalseForInvalidPassword() {
        // Given
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        // When
        boolean result = userService.checkPassword(testUser, "wrongPassword");

        // Then
        assertThat(result).isFalse();
        
        verify(passwordEncoder).matches("wrongPassword", testUser.getPassword());
    }
}