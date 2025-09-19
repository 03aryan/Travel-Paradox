package com.example.travel.service;

import com.example.travel.model.Hotel;
import com.example.travel.model.User;
import com.example.travel.model.UserRole;
import com.example.travel.exception.*;
import com.example.travel.repository.HotelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HotelService Tests")
class HotelServiceTest {

    @Mock
    private HotelRepository hotelRepository;

    @InjectMocks
    private HotelService hotelService;

    private Hotel testHotel;
    private User testProvider;

    @BeforeEach
    void setUp() {
        testProvider = new User();
        testProvider.setId(1L);
        testProvider.setUsername("provider");
        testProvider.setEmail("provider@example.com");
        testProvider.setRole(UserRole.PROVIDER);

        testHotel = new Hotel();
        testHotel.setId(1L);
        testHotel.setName("Test Hotel");
        testHotel.setLocation("Test Location");
        testHotel.setPricePerNight(new BigDecimal("100.00"));
        testHotel.setServiceProvider(testProvider);
    }

    @Test
    @DisplayName("Test Case 9: Should create a new hotel successfully")
    void shouldCreateHotelSuccessfully() {
        // Given
        when(hotelRepository.save(any(Hotel.class))).thenReturn(testHotel);

        // When
        Hotel result = hotelService.addHotel("Test Hotel", "Test Location", 
                                              new BigDecimal("100.00"), testProvider);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Hotel");
        assertThat(result.getLocation()).isEqualTo("Test Location");
        assertThat(result.getPricePerNight()).isEqualTo(new BigDecimal("100.00"));
        assertThat(result.getServiceProvider()).isEqualTo(testProvider);
        
        verify(hotelRepository).save(any(Hotel.class));
    }

    @Test
    @DisplayName("Test Case 10: Should find hotel by ID with service provider")
    void shouldFindHotelByIdWithServiceProvider() {
        // Given
        when(hotelRepository.findByIdWithServiceProvider(1L)).thenReturn(Optional.of(testHotel));

        // When
        Optional<Hotel> result = hotelService.findById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getServiceProvider()).isNotNull();
        
        verify(hotelRepository).findByIdWithServiceProvider(1L);
    }

    @Test
    @DisplayName("Test Case 11: Should return empty when hotel not found")
    void shouldReturnEmptyWhenHotelNotFound() {
        // Given
        when(hotelRepository.findByIdWithServiceProvider(999L)).thenReturn(Optional.empty());

        // When
        Optional<Hotel> result = hotelService.findById(999L);

        // Then
        assertThat(result).isEmpty();
        
        verify(hotelRepository).findByIdWithServiceProvider(999L);
    }

    @Test
    @DisplayName("Test Case 12: Should search hotels by location")
    void shouldSearchHotelsByLocation() {
        // Given
        Hotel hotel2 = new Hotel();
        hotel2.setId(2L);
        hotel2.setName("Another Hotel");
        hotel2.setLocation("Test Location");
        hotel2.setPricePerNight(new BigDecimal("150.00"));
        
        List<Hotel> hotels = Arrays.asList(testHotel, hotel2);
        when(hotelRepository.findByLocationContainingIgnoreCase("Test Location")).thenReturn(hotels);

        // When
        List<Hotel> result = hotelService.searchHotels("Test Location", null, null);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(hotel -> hotel.getLocation().contains("Test Location"));
        
        verify(hotelRepository).findByLocationContainingIgnoreCase("Test Location");
    }

    @Test
    @DisplayName("Test Case 13: Should update hotel successfully when user is owner")
    void shouldUpdateHotelWhenUserIsOwner() {
        // Given
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(testHotel));
        when(hotelRepository.save(any(Hotel.class))).thenReturn(testHotel);

        // When
        Hotel result = hotelService.updateHotel(1L, "Updated Hotel", "Updated Location", 
                                              new BigDecimal("200.00"), testProvider);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Hotel");
        assertThat(result.getLocation()).isEqualTo("Updated Location");
        assertThat(result.getPricePerNight()).isEqualTo(new BigDecimal("200.00"));
        
        verify(hotelRepository).findById(1L);
        verify(hotelRepository).save(any(Hotel.class));
    }

    @Test
    @DisplayName("Test Case 14: Should throw exception when updating hotel as non-owner")
    void shouldThrowExceptionWhenUpdatingHotelAsNonOwner() {
        // Given
        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setUsername("another");
        anotherUser.setRole(UserRole.PROVIDER);
        
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(testHotel));

        // When & Then
        assertThatThrownBy(() -> 
            hotelService.updateHotel(1L, "Updated Hotel", "Updated Location", 
                                   new BigDecimal("200.00"), anotherUser))
            .isInstanceOf(UnauthorizedAccessException.class)
            .hasMessageContaining("You are not authorized to update this hotel");
        
        verify(hotelRepository).findById(1L);
        verify(hotelRepository, never()).save(any(Hotel.class));
    }

    @Test
    @DisplayName("Test Case 15: Should delete hotel successfully when user is owner")
    void shouldDeleteHotelWhenUserIsOwner() {
        // Given
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(testHotel));

        // When
        hotelService.deleteHotel(1L, testProvider);

        // Then
        verify(hotelRepository).findById(1L);
        verify(hotelRepository).delete(testHotel);
    }

    @Test
    @DisplayName("Test Case 16: Should throw exception when deleting hotel as non-owner")
    void shouldThrowExceptionWhenDeletingHotelAsNonOwner() {
        // Given
        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setUsername("another");
        anotherUser.setRole(UserRole.PROVIDER);
        
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(testHotel));

        // When & Then
        assertThatThrownBy(() -> hotelService.deleteHotel(1L, anotherUser))
            .isInstanceOf(UnauthorizedAccessException.class)
            .hasMessageContaining("You are not authorized to delete this hotel");
        
        verify(hotelRepository).findById(1L);
        verify(hotelRepository, never()).delete(any(Hotel.class));
    }

    @Test
    @DisplayName("Test Case 17: Should get hotels by service provider")
    void shouldGetHotelsByServiceProvider() {
        
        Hotel hotel2 = new Hotel();
        hotel2.setId(2L);
        hotel2.setName("Provider's Second Hotel");
        hotel2.setServiceProvider(testProvider);
        
        List<Hotel> hotels = Arrays.asList(testHotel, hotel2);
        when(hotelRepository.findByServiceProvider(testProvider)).thenReturn(hotels);

       
        List<Hotel> result = hotelService.getHotelsByProvider(testProvider);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(hotel -> hotel.getServiceProvider().equals(testProvider));
        
        verify(hotelRepository).findByServiceProvider(testProvider);
    }
}