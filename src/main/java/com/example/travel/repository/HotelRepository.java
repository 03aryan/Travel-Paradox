package com.example.travel.repository;

import com.example.travel.model.Hotel;
import com.example.travel.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {

    List<Hotel> findByServiceProvider(User serviceProvider);

    @Query("SELECT h FROM Hotel h JOIN FETCH h.serviceProvider WHERE LOWER(h.location) LIKE LOWER(CONCAT('%', :location, '%'))")
    List<Hotel> findByLocationContainingIgnoreCase(@Param("location") String location);

    @Query("SELECT h FROM Hotel h JOIN FETCH h.serviceProvider WHERE h.pricePerNight <= :maxPrice")
    List<Hotel> findByPricePerNightLessThanEqual(@Param("maxPrice") BigDecimal maxPrice);

    @Query("SELECT h FROM Hotel h JOIN FETCH h.serviceProvider WHERE LOWER(h.location) LIKE LOWER(CONCAT('%', :location, '%')) AND h.pricePerNight <= :maxPrice")
    List<Hotel> findByLocationAndMaxPrice(@Param("location") String location, @Param("maxPrice") BigDecimal maxPrice);

    @Query("SELECT h FROM Hotel h JOIN FETCH h.serviceProvider WHERE LOWER(h.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Hotel> findByNameContainingIgnoreCase(@Param("name") String name);

    @Query("SELECT h FROM Hotel h JOIN FETCH h.serviceProvider")
    @NonNull
    List<Hotel> findAll();

    @Query("SELECT h FROM Hotel h JOIN FETCH h.serviceProvider WHERE h.id = :id")
    Optional<Hotel> findByIdWithServiceProvider(@Param("id") Long id);
}
