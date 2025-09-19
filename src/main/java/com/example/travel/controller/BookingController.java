package com.example.travel.controller;

import com.example.travel.exception.*;
import com.example.travel.model.Booking;
import com.example.travel.model.Hotel;
import com.example.travel.model.User;
import com.example.travel.model.UserRole;
import com.example.travel.service.BookingService;
import com.example.travel.service.HotelService;
import com.example.travel.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final HotelService hotelService;
    private final UserService userService;

    // USER endpoints
    @GetMapping("/create/{hotelId}")
    public String showBookingForm(@PathVariable Long hotelId, Authentication authentication, Model model) {
        try {
            User currentUser = getCurrentUser(authentication);
            validateUserRole(currentUser, UserRole.USER);

            Hotel hotel = hotelService.findById(hotelId)
                    .orElseThrow(() -> new HotelNotFoundException(hotelId));

            model.addAttribute("hotel", hotel);
            model.addAttribute("booking", new BookingDto());

            // Add helpful information for users
            model.addAttribute("today", LocalDate.now());
            model.addAttribute("minCheckout", LocalDate.now().plusDays(1));

            return "bookings/create";
        } catch (HotelNotFoundException e) {
            return "redirect:/hotels/search";
        } catch (UnauthorizedAccessException e) {
            return "redirect:/hotels/search";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load booking form: " + e.getMessage());
            return "redirect:/hotels/search";
        }
    }

    @PostMapping("/create/{hotelId}")
    public String createBooking(@PathVariable Long hotelId,
                               @Valid @ModelAttribute("booking") BookingDto bookingDto,
                               BindingResult bindingResult,
                               Authentication authentication,
                               Model model,
                               RedirectAttributes redirectAttributes) {

        try {
            User currentUser = getCurrentUser(authentication);
            validateUserRole(currentUser, UserRole.USER);

            if (bindingResult.hasErrors()) {
                Hotel hotel = hotelService.findById(hotelId)
                        .orElseThrow(() -> new HotelNotFoundException(hotelId));
                model.addAttribute("hotel", hotel);
                return "bookings/create";
            }

            bookingService.bookHotel(hotelId, bookingDto.getCheckInDate(),
                                   bookingDto.getCheckOutDate(), currentUser);
            redirectAttributes.addFlashAttribute("success", "Booking created successfully!");
            return "redirect:/bookings/my-bookings";
        } catch (HotelNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "Hotel not found");
            return "redirect:/hotels/search";
        } catch (InvalidBookingException | ValidationException e) {
            try {
                Hotel hotel = hotelService.findById(hotelId)
                        .orElseThrow(() -> new HotelNotFoundException(hotelId));
                model.addAttribute("hotel", hotel);
                model.addAttribute("error", e.getMessage());
                return "bookings/create";
            } catch (HotelNotFoundException ex) {
                redirectAttributes.addFlashAttribute("error", "Hotel not found");
                return "redirect:/hotels/search";
            }
        } catch (UnauthorizedAccessException e) {
            return "redirect:/hotels/search";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Unable to create booking: " + e.getMessage());
            return "redirect:/hotels/search";
        }
    }

    @GetMapping("/my-bookings")
    public String listMyBookings(Authentication authentication, Model model) {
        try {
            User currentUser = getCurrentUser(authentication);
            validateUserRole(currentUser, UserRole.USER);

            List<Booking> bookings = bookingService.getBookingsByUser(currentUser);
            model.addAttribute("bookings", bookings);
            return "bookings/my-bookings";
        } catch (UnauthorizedAccessException e) {
            return "redirect:/hotels/search";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load bookings: " + e.getMessage());
            return "redirect:/hotels/search";
        }
    }

    @PostMapping("/cancel/{id}")
    public String cancelBooking(@PathVariable Long id,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {

        try {
            User currentUser = getCurrentUser(authentication);
            validateUserRole(currentUser, UserRole.USER);

            bookingService.cancelBooking(id, currentUser);
            redirectAttributes.addFlashAttribute("success", "Booking cancelled successfully!");
        } catch (BookingNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "Booking not found");
        } catch (BookingCancellationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (UnauthorizedAccessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error cancelling booking: " + e.getMessage());
        }

        return "redirect:/bookings/my-bookings";
    }

    // PROVIDER endpoints
    @GetMapping("/hotel-bookings")
    public String listHotelBookings(Authentication authentication, Model model) {
        try {
            User currentUser = getCurrentUser(authentication);
            validateUserRole(currentUser, UserRole.PROVIDER);

            List<Booking> bookings = bookingService.getBookingsByProvider(currentUser);
            model.addAttribute("bookings", bookings);
            return "bookings/hotel-bookings";
        } catch (UnauthorizedAccessException e) {
            return "redirect:/hotels/search";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load bookings: " + e.getMessage());
            return "redirect:/hotels/search";
        }
    }

    @GetMapping("/hotel/{hotelId}")
    public String listBookingsForHotel(@PathVariable Long hotelId,
                                      Authentication authentication,
                                      Model model) {

        try {
            User currentUser = getCurrentUser(authentication);
            validateUserRole(currentUser, UserRole.PROVIDER);

            Hotel hotel = hotelService.findById(hotelId)
                    .orElseThrow(() -> new HotelNotFoundException(hotelId));
            
            // Validate hotel ownership
            if (!hotel.getServiceProvider().getId().equals(currentUser.getId())) {
                throw UnauthorizedAccessException.forAction("view bookings for hotels you don't own");
            }

            List<Booking> bookings = bookingService.getBookingsByHotel(hotel);

            // Split bookings into past and upcoming based on checkInDate
            LocalDate today = LocalDate.now();
            List<Booking> pastBookings = bookings.stream()
                .filter(b -> b.getCheckInDate().isBefore(today))
                .toList();
            List<Booking> upcomingBookings = bookings.stream()
                .filter(b -> b.getCheckInDate().isAfter(today))
                .toList();

            model.addAttribute("hotel", hotel);
            model.addAttribute("bookings", bookings);
            model.addAttribute("pastBookings", pastBookings);
            model.addAttribute("upcomingBookings", upcomingBookings);
            return "bookings/hotel-specific";
        } catch (HotelNotFoundException e) {
            return "redirect:/hotels/manage";
        } catch (UnauthorizedAccessException e) {
            return "redirect:/hotels/manage";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load hotel bookings: " + e.getMessage());
            return "redirect:/hotels/manage";
        }
    }

    private User getCurrentUser(Authentication authentication) {
        try {
            String username = authentication.getName();
            return userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        } catch (Exception e) {
            throw new RuntimeException("Unable to get current user", e);
        }
    }
    
    private void validateUserRole(User user, UserRole expectedRole) {
        if (user.getRole() != expectedRole) {
            throw UnauthorizedAccessException.forAction("access this resource with role: " + expectedRole);
        }
    }

    // DTO class for booking form
    public static class BookingDto {
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate checkInDate;

        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate checkOutDate;

        // Getters and setters
        public LocalDate getCheckInDate() { return checkInDate; }
        public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }

        public LocalDate getCheckOutDate() { return checkOutDate; }
        public void setCheckOutDate(LocalDate checkOutDate) { this.checkOutDate = checkOutDate; }
    }
}
