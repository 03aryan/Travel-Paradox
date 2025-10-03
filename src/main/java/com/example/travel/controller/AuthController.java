package com.example.travel.controller;

import com.example.travel.model.UserRole;
import com.example.travel.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        UserRegistrationDto userRegistrationDto = new UserRegistrationDto();
        userRegistrationDto.setRole(UserRole.PROVIDER);
        model.addAttribute("user", userRegistrationDto);
        return "auth/register";
    }

    @GetMapping("/register/user")
    public String showStandardUserRegistrationForm(Model model) {
        UserRegistrationDto userRegistrationDto = new UserRegistrationDto();
        userRegistrationDto.setRole(UserRole.USER);
        model.addAttribute("user", userRegistrationDto);
        return "auth/register-user";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") UserRegistrationDto userDto,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        userDto.setRole(UserRole.PROVIDER);
        validateProviderDetails(userDto, bindingResult);

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        if (!userDto.getPassword().equals(userDto.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.user", "Passwords do not match");
            return "auth/register";
        }

        try {
            userService.registerUser(userDto.getUsername(), userDto.getEmail(),
                    userDto.getPassword(), userDto.getRole(),
                    userDto.getFullName(), userDto.getContactNumber(), userDto.getBusinessName());
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please login.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }

    @PostMapping("/register/user")
    public String registerStandardUser(@Valid @ModelAttribute("user") UserRegistrationDto userDto,
                                       BindingResult bindingResult,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {

        userDto.setRole(UserRole.USER);

        if (bindingResult.hasErrors()) {
            return "auth/register-user";
        }

        if (!userDto.getPassword().equals(userDto.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.user", "Passwords do not match");
            return "auth/register-user";
        }

        try {
            userService.registerUser(userDto.getUsername(), userDto.getEmail(),
                    userDto.getPassword(), UserRole.USER);
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please login.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register-user";
        }
    }

    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "error", required = false) String error,
                               @RequestParam(value = "logout", required = false) String logout,
                               Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }
        if (logout != null) {
            model.addAttribute("success", "You have been logged out successfully");
        }
        return "auth/login";
    }

    private void validateProviderDetails(UserRegistrationDto userDto, BindingResult bindingResult) {
        if (userDto.getRole() != UserRole.PROVIDER) {
            return;
        }

        if (!StringUtils.hasText(userDto.getFullName())) {
            bindingResult.rejectValue("fullName", "error.user", "Full name is required for hotel providers");
        }

        if (!StringUtils.hasText(userDto.getBusinessName())) {
            bindingResult.rejectValue("businessName", "error.user", "Business name is required for hotel providers");
        }

        if (!StringUtils.hasText(userDto.getContactNumber())) {
            bindingResult.rejectValue("contactNumber", "error.user", "Contact number is required for hotel providers");
        } else if (!userDto.getContactNumber().matches("^[0-9+\\-() ]{7,20}$")) {
            bindingResult.rejectValue("contactNumber", "error.user", "Contact number must be 7-20 digits and can include +, -, parentheses, or spaces");
        }
    }

    // DTO class for registration form
    public static class UserRegistrationDto {
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Username can only contain letters, numbers, dots, underscores, and hyphens")
        private String username;

        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]+$",
        message = "Password must include at least one uppercase letter and one special character (@, $, !, %, *, ?, &, #)")
        private String password;

        @NotBlank(message = "Please confirm your password")
        private String confirmPassword;

    @Size(max = 100, message = "Full name must be at most 100 characters")
    private String fullName;

    @Size(max = 150, message = "Business name must be at most 150 characters")
    private String businessName;

    @Pattern(regexp = "^$|^[0-9+\\-() ]{7,20}$", message = "Contact number must be 7-20 digits and can include +, -, parentheses, or spaces")
    private String contactNumber;

    @NotNull(message = "Please select an account type")
        private UserRole role;

        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getBusinessName() { return businessName; }
        public void setBusinessName(String businessName) { this.businessName = businessName; }

        public String getContactNumber() { return contactNumber; }
        public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

        public UserRole getRole() { return role; }
        public void setRole(UserRole role) { this.role = role; }
    }
}
