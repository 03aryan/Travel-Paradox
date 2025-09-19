package com.example.travel.service;

import com.example.travel.model.Booking;
import com.example.travel.model.Hotel;
import com.example.travel.model.User;
import com.example.travel.model.UserRole;
import com.example.travel.exception.*;
import com.example.travel.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService Tests")
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock 
    private HotelService hotelService;

    @InjectMocks
    private BookingService bookingService;

    private Booking testBooking;
    private User testUser;
    private Hotel testHotel;
    private User testProvider;

    @BeforeEach
    void setUp() {
        testProvider = new User();
        testProvider.setId(1L);
        testProvider.setUsername("provider");
        testProvider.setRole(UserRole.PROVIDER);

        testUser = new User();
        testUser.setId(2L);
        testUser.setUsername("user");
        testUser.setRole(UserRole.USER);

        testHotel = new Hotel();
        testHotel.setId(1L);
        testHotel.setName("Test Hotel");
        testHotel.setLocation("Test Location");
        testHotel.setPricePerNight(new BigDecimal("100.00"));
        testHotel.setServiceProvider(testProvider);

        testBooking = new Booking();
        testBooking.setId(1L);
        testBooking.setCheckInDate(LocalDate.now().plusDays(1));
        testBooking.setCheckOutDate(LocalDate.now().plusDays(3));
        testBooking.setUser(testUser);
        testBooking.setHotel(testHotel);
    }

    @Test
    @DisplayName("Test Case 18: Should create booking successfully when dates are valid")
    void shouldCreateBookingSuccessfully() {
        // Given
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);
        
        when(hotelService.findById(1L)).thenReturn(Optional.of(testHotel));
        when(bookingRepository.findConflictingBookings(eq(testHotel), eq(checkIn), eq(checkOut)))
            .thenReturn(Arrays.asList());
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

        // When
        Booking result = bookingService.bookHotel(1L, checkIn, checkOut, testUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(testUser);
        assertThat(result.getHotel()).isEqualTo(testHotel);
        assertThat(result.getCheckInDate()).isEqualTo(checkIn);
        assertThat(result.getCheckOutDate()).isEqualTo(checkOut);
        
        verify(hotelService).findById(1L);
        verify(bookingRepository).findConflictingBookings(testHotel, checkIn, checkOut);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("Test Case 19: Should throw exception when check-in date is in the past")
    void shouldThrowExceptionWhenCheckInDateIsInPast() {
        // Given
        LocalDate pastDate = LocalDate.now().minusDays(1);
        LocalDate futureDate = LocalDate.now().plusDays(1);

        // When & Then
        assertThatThrownBy(() -> 
            bookingService.bookHotel(1L, pastDate, futureDate, testUser))
            .isInstanceOf(InvalidBookingException.class)
            .hasMessageContaining("Check-in date cannot be in the past");
        
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Test Case 20: Should throw exception when check-out date is before check-in")
    void shouldThrowExceptionWhenCheckOutDateIsBeforeCheckIn() {
        // Given
        LocalDate checkIn = LocalDate.now().plusDays(3);
        LocalDate checkOut = LocalDate.now().plusDays(1);

        // When & Then
        assertThatThrownBy(() -> 
            bookingService.bookHotel(1L, checkIn, checkOut, testUser))
            .isInstanceOf(InvalidBookingException.class)
            .hasMessageContaining("Check-out date must be after check-in date");
        
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Test Case 21: Should throw exception when booking conflicts with existing booking")
    void shouldThrowExceptionWhenBookingConflicts() {
        // Given
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);
        
        Booking existingBooking = new Booking();
        existingBooking.setCheckInDate(LocalDate.now().plusDays(2));
        existingBooking.setCheckOutDate(LocalDate.now().plusDays(4));
        
        when(hotelService.findById(1L)).thenReturn(Optional.of(testHotel));
        when(bookingRepository.findConflictingBookings(eq(testHotel), eq(checkIn), eq(checkOut)))
            .thenReturn(Arrays.asList(existingBooking));

        // When & Then
        assertThatThrownBy(() -> 
            bookingService.bookHotel(1L, checkIn, checkOut, testUser))
            .isInstanceOf(InvalidBookingException.class)
            .hasMessageContaining("These dates are already booked");
        
        verify(hotelService).findById(1L);
        verify(bookingRepository).findConflictingBookings(testHotel, checkIn, checkOut);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Test Case 22: Should get bookings by user")
    void shouldGetBookingsByUser() {
        // Given
        Booking booking2 = new Booking();
        booking2.setId(2L);
        booking2.setUser(testUser);
        booking2.setHotel(testHotel);
        
        List<Booking> bookings = Arrays.asList(testBooking, booking2);
        when(bookingRepository.findByUserOrderByCheckInDateDesc(testUser)).thenReturn(bookings);

        // When
        List<Booking> result = bookingService.getBookingsByUser(testUser);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(booking -> booking.getUser().equals(testUser));
        
        verify(bookingRepository).findByUserOrderByCheckInDateDesc(testUser);
    }

    @Test
    @DisplayName("Test Case 23: Should get bookings by hotel")
    void shouldGetBookingsByHotel() {
        // Given
        Booking booking2 = new Booking();
        booking2.setId(2L);
        booking2.setUser(testUser);
        booking2.setHotel(testHotel);
        
        List<Booking> bookings = Arrays.asList(testBooking, booking2);
        when(bookingRepository.findByHotel(testHotel)).thenReturn(bookings);

        // When
        List<Booking> result = bookingService.getBookingsByHotel(testHotel);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(booking -> booking.getHotel().equals(testHotel));
        
        verify(bookingRepository).findByHotel(testHotel);
    }

    @Test
    @DisplayName("Test Case 24: Should cancel booking successfully")
    void shouldCancelBookingSuccessfully() {
        // Given
        // Set check-in date to more than 24 hours from now
        testBooking.setCheckInDate(LocalDate.now().plusDays(2));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        // When
        bookingService.cancelBooking(1L, testUser);

        // Then
        verify(bookingRepository).findById(1L);
        verify(bookingRepository).delete(testBooking);
    }

    @Test
    @DisplayName("Test Case 25: Should throw exception when canceling booking as non-owner")
    void shouldThrowExceptionWhenCancelingBookingAsNonOwner() {
        // Given
        User anotherUser = new User();
        anotherUser.setId(3L);
        anotherUser.setUsername("another");
        
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        // When & Then
        assertThatThrownBy(() -> bookingService.cancelBooking(1L, anotherUser))
            .isInstanceOf(UnauthorizedAccessException.class)
            .hasMessageContaining("You are not authorized to cancel bookings that don't belong to you");
        
        verify(bookingRepository).findById(1L);
        verify(bookingRepository, never()).delete(any(Booking.class));
    }

    @Test
    @DisplayName("Test Case 26: Should check if dates are available")
    void shouldCheckIfDatesAreAvailable() {
        // Given
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);
        
        when(hotelService.findById(1L)).thenReturn(Optional.of(testHotel));
        when(bookingRepository.findConflictingBookings(eq(testHotel), eq(checkIn), eq(checkOut)))
            .thenReturn(Arrays.asList());

        // When
        boolean result = bookingService.areDatesAvailable(1L, checkIn, checkOut);

        // Then
        assertThat(result).isTrue();
        
        verify(hotelService).findById(1L);
        verify(bookingRepository).findConflictingBookings(testHotel, checkIn, checkOut);
    }

    @Test
    @DisplayName("Test Case 27: Should return false when dates are not available")
    void shouldReturnFalseWhenDatesAreNotAvailable() {
        // Given
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);
        
        Booking conflictingBooking = new Booking();
        when(hotelService.findById(1L)).thenReturn(Optional.of(testHotel));
        when(bookingRepository.findConflictingBookings(eq(testHotel), eq(checkIn), eq(checkOut)))
            .thenReturn(Arrays.asList(conflictingBooking));

        // When
        boolean result = bookingService.areDatesAvailable(1L, checkIn, checkOut);

        // Then
        assertThat(result).isFalse();
        
        verify(hotelService).findById(1L);
        verify(bookingRepository).findConflictingBookings(testHotel, checkIn, checkOut);
    }
}