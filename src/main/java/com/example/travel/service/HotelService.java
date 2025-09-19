package com.example.travel.service;

import com.example.travel.exception.*;
import com.example.travel.model.Hotel;
import com.example.travel.model.User;
import com.example.travel.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HotelService {

    private final HotelRepository hotelRepository;

    public Hotel addHotel(String name, String location, BigDecimal pricePerNight, User serviceProvider) {
        try {
            validateHotelData(name, location, pricePerNight);
            
            Hotel hotel = new Hotel();
            hotel.setName(name);
            hotel.setLocation(location);
            hotel.setPricePerNight(pricePerNight);
            hotel.setServiceProvider(serviceProvider);
            return hotelRepository.save(hotel);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to add hotel: " + e.getMessage(), e);
        }
    }

    private void validateHotelData(String name, String location, BigDecimal pricePerNight) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Hotel name is required");
        }
        if (location == null || location.trim().isEmpty()) {
            throw new ValidationException("Hotel location is required");
        }
        if (pricePerNight == null || pricePerNight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Price per night must be greater than zero");
        }
    }

    public Hotel updateHotel(Long hotelId, String name, String location, BigDecimal pricePerNight, User serviceProvider) {
        try {
            validateHotelData(name, location, pricePerNight);
            
            Hotel hotel = hotelRepository.findById(hotelId)
                    .orElseThrow(() -> new HotelNotFoundException(hotelId));

            validateHotelOwnership(hotel, serviceProvider, "update this hotel");

            hotel.setName(name);
            hotel.setLocation(location);
            hotel.setPricePerNight(pricePerNight);
            return hotelRepository.save(hotel);
        } catch (HotelNotFoundException | UnauthorizedAccessException | ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to update hotel: " + e.getMessage(), e);
        }
    }

    public void deleteHotel(Long hotelId, User serviceProvider) {
        try {
            Hotel hotel = hotelRepository.findById(hotelId)
                    .orElseThrow(() -> new HotelNotFoundException(hotelId));

            validateHotelOwnership(hotel, serviceProvider, "delete this hotel");

            hotelRepository.delete(hotel);
        } catch (HotelNotFoundException | UnauthorizedAccessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to delete hotel: " + e.getMessage(), e);
        }
    }

    private void validateHotelOwnership(Hotel hotel, User serviceProvider, String action) {
        if (!hotel.getServiceProvider().getId().equals(serviceProvider.getId())) {
            throw UnauthorizedAccessException.forAction(action);
        }
    }

    public List<Hotel> getHotelsByProvider(User serviceProvider) {
        try {
            return hotelRepository.findByServiceProvider(serviceProvider);
        } catch (Exception e) {
            throw new RuntimeException("Unable to retrieve hotels for provider: " + serviceProvider.getUsername(), e);
        }
    }

    public List<Hotel> searchHotels(String location, BigDecimal minPrice, BigDecimal maxPrice) {
        try {
            return executeSearch(location, minPrice, maxPrice);
        } catch (Exception e) {
            throw new RuntimeException("Unable to search hotels: " + e.getMessage(), e);
        }
    }

    private List<Hotel> executeSearch(String location, BigDecimal minPrice, BigDecimal maxPrice) {
        String trimmedLocation = (location != null) ? location.trim() : null;
        boolean hasLocation = trimmedLocation != null && !trimmedLocation.isEmpty();
        boolean hasMaxPrice = maxPrice != null;
        
        if (hasLocation && hasMaxPrice) {
            return hotelRepository.findByLocationAndMaxPrice(trimmedLocation, maxPrice);
        } 
        
        if (hasLocation) {
            return hotelRepository.findByLocationContainingIgnoreCase(trimmedLocation);
        } 
        
        if (hasMaxPrice) {
            return hotelRepository.findByPricePerNightLessThanEqual(maxPrice);
        }
        
        return hotelRepository.findAll();
    }

    public Optional<Hotel> findById(Long id) {
        try {
            return hotelRepository.findByIdWithServiceProvider(id);
        } catch (Exception e) {
            throw new RuntimeException("Unable to find hotel with ID: " + id, e);
        }
    }
}
