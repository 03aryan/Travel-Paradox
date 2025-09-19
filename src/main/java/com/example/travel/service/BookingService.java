package com.example.travel.service;

import com.example.travel.exception.*;
import com.example.travel.model.Booking;
import com.example.travel.model.Hotel;
import com.example.travel.model.User;
import com.example.travel.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final HotelService hotelService;

    public Booking bookHotel(Long hotelId, LocalDate checkInDate, LocalDate checkOutDate, User user) {
        try {
            // Basic date validation - convert if statements to exception handling
            validateBookingDates(checkInDate, checkOutDate);
            
            Hotel hotel = hotelService.findById(hotelId)
                    .orElseThrow(() -> new HotelNotFoundException(hotelId));

            // Check for date conflicts - this is for hotel management, not user restriction
            validateDateAvailability(hotel, checkInDate, checkOutDate);

            Booking booking = new Booking();
            booking.setHotel(hotel);
            booking.setUser(user);
            booking.setCheckInDate(checkInDate);
            booking.setCheckOutDate(checkOutDate);

            return bookingRepository.save(booking);
        } catch (HotelNotFoundException | InvalidBookingException e) {
            throw e; // Re-throw domain exceptions
        } catch (Exception e) {
            throw new InvalidBookingException("Unable to process booking: " + e.getMessage(), e);
        }
    }

    private void validateBookingDates(LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null || checkOutDate == null) {
            throw new ValidationException("Check-in and check-out dates are required");
        }

        if (checkInDate.isBefore(LocalDate.now())) {
            throw new InvalidBookingException("Check-in date cannot be in the past");
        }

        if (checkInDate.isAfter(checkOutDate) || checkInDate.isEqual(checkOutDate)) {
            throw new InvalidBookingException("Check-out date must be after check-in date");
        }
    }

    private void validateDateAvailability(Hotel hotel, LocalDate checkInDate, LocalDate checkOutDate) {
        List<Booking> conflictingBookings = bookingRepository.findConflictingBookings(
            hotel, checkInDate, checkOutDate);

        if (!conflictingBookings.isEmpty()) {
            throw new InvalidBookingException("These dates are already booked. Please contact the hotel for availability or choose different dates.");
        }
    }

    public void cancelBooking(Long bookingId, User user) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new BookingNotFoundException(bookingId));

            validateCancellationPermission(booking, user);
            validateCancellationTiming(booking);

            bookingRepository.delete(booking);
        } catch (BookingNotFoundException | UnauthorizedAccessException | BookingCancellationException e) {
            throw e; // Re-throw domain exceptions
        } catch (Exception e) {
            throw new BookingCancellationException("Unable to cancel booking: " + e.getMessage(), e);
        }
    }

    private void validateCancellationPermission(Booking booking, User user) {
        if (!booking.getUser().getId().equals(user.getId())) {
            throw UnauthorizedAccessException.forAction("cancel bookings that don't belong to you");
        }
    }

    private void validateCancellationTiming(Booking booking) {
        if (booking.getCheckInDate().isBefore(LocalDate.now().plusDays(1))) {
            throw new BookingCancellationException("Cannot cancel bookings less than 24 hours before check-in");
        }
    }

    public List<Booking> getBookingsByUser(User user) {
        try {
            return bookingRepository.findByUserOrderByCheckInDateDesc(user);
        } catch (Exception e) {
            throw new RuntimeException("Unable to retrieve bookings for user: " + user.getUsername(), e);
        }
    }

    public List<Booking> getBookingsByHotel(Hotel hotel) {
        try {
            return bookingRepository.findByHotel(hotel);
        } catch (Exception e) {
            throw new RuntimeException("Unable to retrieve bookings for hotel: " + hotel.getName(), e);
        }
    }

    public List<Booking> getBookingsByProvider(User provider) {
        try {
            // Get all bookings for hotels owned by this provider
            List<Hotel> providerHotels = hotelService.getHotelsByProvider(provider);
            return providerHotels.stream()
                    .flatMap(hotel -> bookingRepository.findByHotel(hotel).stream())
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Unable to retrieve bookings for provider: " + provider.getUsername(), e);
        }
    }

    public Optional<Booking> findById(Long id) {
        try {
            return bookingRepository.findById(id);
        } catch (Exception e) {
            throw new RuntimeException("Unable to find booking with ID: " + id, e);
        }
    }

    // Method to get available dates for a hotel (for future enhancement)
    public List<LocalDate> getUnavailableDates(Long hotelId, LocalDate startDate, LocalDate endDate) {
        try {
            Hotel hotel = hotelService.findById(hotelId)
                    .orElseThrow(() -> new HotelNotFoundException(hotelId));

            List<Booking> bookings = bookingRepository.findConflictingBookings(hotel, startDate, endDate);

            return bookings.stream()
                    .flatMap(booking -> booking.getCheckInDate().datesUntil(booking.getCheckOutDate().plusDays(1)))
                    .distinct()
                    .sorted()
                    .toList();
        } catch (HotelNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to retrieve unavailable dates for hotel: " + hotelId, e);
        }
    }

    // Method to check if specific dates are available
    public boolean areDatesAvailable(Long hotelId, LocalDate checkIn, LocalDate checkOut) {
        try {
            Hotel hotel = hotelService.findById(hotelId)
                    .orElseThrow(() -> new HotelNotFoundException(hotelId));

            List<Booking> conflictingBookings = bookingRepository.findConflictingBookings(hotel, checkIn, checkOut);
            return conflictingBookings.isEmpty();
        } catch (HotelNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to check date availability for hotel: " + hotelId, e);
        }
    }
}
