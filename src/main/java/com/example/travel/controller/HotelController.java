package com.example.travel.controller;

import com.example.travel.exception.*;
import com.example.travel.model.Hotel;
import com.example.travel.model.User;
import com.example.travel.model.UserRole;
import com.example.travel.service.HotelService;
import com.example.travel.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;
    private final UserService userService;

    // PROVIDER endpoints
    @GetMapping("/manage")
    public String manageHotels(Authentication authentication, Model model) {
        try {
            User currentUser = getCurrentUser(authentication);
            validateUserRole(currentUser, UserRole.PROVIDER);

            List<Hotel> hotels = hotelService.getHotelsByProvider(currentUser);
            model.addAttribute("hotels", hotels);
            return "hotels/manage";
        } catch (UnauthorizedAccessException e) {
            return "redirect:/hotels/search";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load hotels: " + e.getMessage());
            return "redirect:/hotels/search";
        }
    }

    @GetMapping("/add")
    public String showAddHotelForm(Authentication authentication, Model model) {
        try {
            User currentUser = getCurrentUser(authentication);
            validateUserRole(currentUser, UserRole.PROVIDER);

            model.addAttribute("hotel", new HotelDto());
            return "hotels/add";
        } catch (UnauthorizedAccessException e) {
            return "redirect:/hotels/search";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load form: " + e.getMessage());
            return "redirect:/hotels/search";
        }
    }

    @PostMapping("/add")
    public String addHotel(@Valid @ModelAttribute("hotel") HotelDto hotelDto,
                          BindingResult bindingResult,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {

        try {
            if (bindingResult.hasErrors()) {
                return "hotels/add";
            }

            User currentUser = getCurrentUser(authentication);
            validateUserRole(currentUser, UserRole.PROVIDER);

            hotelService.addHotel(hotelDto.getName(), hotelDto.getLocation(),
                                hotelDto.getPricePerNight(), currentUser);
            redirectAttributes.addFlashAttribute("success", "Hotel added successfully!");
            return "redirect:/hotels/manage";
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/hotels/add";
        } catch (UnauthorizedAccessException e) {
            return "redirect:/hotels/search";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding hotel: " + e.getMessage());
            return "redirect:/hotels/add";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditHotelForm(@PathVariable Long id, Authentication authentication, Model model) {
        try {
            User currentUser = getCurrentUser(authentication);
            validateUserRole(currentUser, UserRole.PROVIDER);

            Hotel hotel = hotelService.findById(id)
                    .orElseThrow(() -> new HotelNotFoundException(id));
            
            // Validate hotel ownership
            if (!hotel.getServiceProvider().getId().equals(currentUser.getId())) {
                throw UnauthorizedAccessException.forAction("edit hotels you don't own");
            }

            HotelDto hotelDto = new HotelDto();
            hotelDto.setName(hotel.getName());
            hotelDto.setLocation(hotel.getLocation());
            hotelDto.setPricePerNight(hotel.getPricePerNight());

            model.addAttribute("hotel", hotelDto);
            model.addAttribute("hotelId", id);
            return "hotels/edit";
        } catch (HotelNotFoundException | UnauthorizedAccessException e) {
            return "redirect:/hotels/manage";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load hotel for editing: " + e.getMessage());
            return "redirect:/hotels/manage";
        }
    }

    @PostMapping("/edit/{id}")
    public String updateHotel(@PathVariable Long id,
                             @Valid @ModelAttribute("hotel") HotelDto hotelDto,
                             BindingResult bindingResult,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {

        try {
            if (bindingResult.hasErrors()) {
                return "hotels/edit";
            }

            User currentUser = getCurrentUser(authentication);
            validateUserRole(currentUser, UserRole.PROVIDER);

            // Validate hotel exists and ownership
            Hotel existingHotel = hotelService.findById(id)
                    .orElseThrow(() -> new HotelNotFoundException(id));
            
            if (!existingHotel.getServiceProvider().getId().equals(currentUser.getId())) {
                throw UnauthorizedAccessException.forAction("edit hotels you don't own");
            }

            hotelService.updateHotel(id, hotelDto.getName(), hotelDto.getLocation(),
                                   hotelDto.getPricePerNight(), currentUser);
            redirectAttributes.addFlashAttribute("success", "Hotel updated successfully!");
            return "redirect:/hotels/manage";
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/hotels/edit/" + id;
        } catch (HotelNotFoundException | UnauthorizedAccessException e) {
            return "redirect:/hotels/manage";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating hotel: " + e.getMessage());
            return "redirect:/hotels/edit/" + id;
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteHotel(@PathVariable Long id,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {

        try {
            User currentUser = getCurrentUser(authentication);
            validateUserRole(currentUser, UserRole.PROVIDER);

            hotelService.deleteHotel(id, currentUser);
            redirectAttributes.addFlashAttribute("success", "Hotel deleted successfully!");
        } catch (HotelNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "Hotel not found");
        } catch (UnauthorizedAccessException e) {
            redirectAttributes.addFlashAttribute("error", "You can only delete your own hotels");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting hotel: " + e.getMessage());
        }

        return "redirect:/hotels/manage";
    }

    // USER endpoints
    @GetMapping("/search")
    public String searchHotels(@RequestParam(required = false) String location,
                              @RequestParam(required = false) BigDecimal maxPrice,
                              Model model) {

        List<Hotel> hotels = hotelService.searchHotels(location, null, maxPrice);
        model.addAttribute("hotels", hotels);
        model.addAttribute("location", location);
        model.addAttribute("maxPrice", maxPrice);
        return "hotels/search";
    }

    @GetMapping("/view/{id}")
    public String viewHotel(@PathVariable Long id, Model model) {
        try {
            Hotel hotel = hotelService.findById(id)
                    .orElseThrow(() -> new HotelNotFoundException(id));
            
            model.addAttribute("hotel", hotel);
            return "hotels/view";
        } catch (HotelNotFoundException e) {
            model.addAttribute("error", "Hotel not found");
            return "redirect:/hotels/search";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading hotel: " + e.getMessage());
            return "redirect:/hotels/search";
        }
    }

    private User getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void validateUserRole(User user, UserRole requiredRole) {
        if (user.getRole() != requiredRole) {
            throw UnauthorizedAccessException.forAction("perform this action");
        }
    }

    // DTO class for hotel form
    public static class HotelDto {
        @NotBlank(message = "Hotel name is required")
        @Size(min = 3, max = 100, message = "Hotel name must be between 3 and 100 characters")
        private String name;

        @NotBlank(message = "Location is required")
        @Size(min = 2, max = 150, message = "Location must be between 2 and 150 characters")
        private String location;

        @NotNull(message = "Price per night is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price per night must be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "Price must be a valid amount with up to 2 decimal places")
        private BigDecimal pricePerNight;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public BigDecimal getPricePerNight() { return pricePerNight; }
        public void setPricePerNight(BigDecimal pricePerNight) { this.pricePerNight = pricePerNight; }
    }
}
