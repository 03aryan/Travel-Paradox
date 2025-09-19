package com.example.travel.controller;

import com.example.travel.model.User;
import com.example.travel.model.UserRole;
import com.example.travel.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        User currentUser = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (currentUser.getRole() == UserRole.PROVIDER) {
            return "redirect:/hotels/manage";
        } else {
            return "redirect:/hotels/search";
        }
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }
}
