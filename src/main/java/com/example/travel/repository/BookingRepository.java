package com.example.travel.repository;

import com.example.travel.model.Booking;
import com.example.travel.model.Hotel;
import com.example.travel.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT b FROM Booking b JOIN FETCH b.hotel h JOIN FETCH h.serviceProvider WHERE b.user = :user")
    List<Booking> findByUser(@Param("user") User user);

    @Query("SELECT b FROM Booking b JOIN FETCH b.user JOIN FETCH b.hotel h JOIN FETCH h.serviceProvider WHERE b.hotel = :hotel")
    List<Booking> findByHotel(@Param("hotel") Hotel hotel);

    List<Booking> findByCheckInDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT b FROM Booking b WHERE b.hotel = :hotel AND " +
           "((b.checkInDate <= :checkOut AND b.checkOutDate >= :checkIn))")
    List<Booking> findConflictingBookings(@Param("hotel") Hotel hotel,
                                         @Param("checkIn") LocalDate checkIn,
                                         @Param("checkOut") LocalDate checkOut);

    @Query("SELECT b FROM Booking b JOIN FETCH b.hotel h JOIN FETCH h.serviceProvider WHERE b.user = :user ORDER BY b.checkInDate DESC")
    List<Booking> findByUserOrderByCheckInDateDesc(@Param("user") User user);
}
