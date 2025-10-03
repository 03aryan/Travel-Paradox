package com.example.travel.controller;

import com.example.travel.controller.AuthController.UserRegistrationDto;
import com.example.travel.controller.BookingController.BookingDto;
import com.example.travel.controller.HotelController.HotelDto;
import com.example.travel.model.UserRole;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DtoValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("User registration DTO enforces required fields and formats")
    void userRegistrationDtoShouldValidateFields() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername(" ");
        dto.setEmail("invalid-email");
        dto.setPassword("short");
        dto.setConfirmPassword(" ");
        dto.setRole(null);

        Set<ConstraintViolation<UserRegistrationDto>> violations = validator.validate(dto);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("username", "email", "password", "confirmPassword", "role");
    }

    @Test
    @DisplayName("User registration DTO accepts valid payload")
    void userRegistrationDtoWithValidDataShouldPass() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("valid.user");
        dto.setEmail("user@example.com");
        dto.setPassword("Complex#Pass1");
        dto.setConfirmPassword("Complex#Pass1");
        dto.setRole(UserRole.USER);

        Set<ConstraintViolation<UserRegistrationDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Hotel DTO requires name, location, and positive price")
    void hotelDtoShouldValidateFields() {
        HotelDto dto = new HotelDto();
        dto.setName(" ");
        dto.setLocation(" ");
        dto.setPricePerNight(BigDecimal.ZERO);

        Set<ConstraintViolation<HotelDto>> violations = validator.validate(dto);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("name", "location", "pricePerNight");
    }

    @Test
    @DisplayName("Hotel DTO accepts valid payload")
    void hotelDtoWithValidDataShouldPass() {
        HotelDto dto = new HotelDto();
        dto.setName("Ocean View Resort");
        dto.setLocation("Goa, India");
        dto.setPricePerNight(new BigDecimal("1999.99"));

        Set<ConstraintViolation<HotelDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("User registration DTO enforces uppercase and special character in password")
    void userRegistrationDtoShouldEnforceUppercaseAndSpecialCharacter() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("valid.user");
        dto.setEmail("user@example.com");
        dto.setPassword("lowercase1");
        dto.setConfirmPassword("lowercase1");
        dto.setRole(UserRole.USER);

        Set<ConstraintViolation<UserRegistrationDto>> violations = validator.validate(dto);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("password");
    }

    @Test
    @DisplayName("Provider registration DTO validates contact number format")
    void providerRegistrationDtoShouldValidateContactNumberFormat() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("provider.user");
        dto.setEmail("provider@example.com");
        dto.setPassword("Complex#Pass1");
        dto.setConfirmPassword("Complex#Pass1");
        dto.setRole(UserRole.PROVIDER);
        dto.setFullName("Provider User");
        dto.setBusinessName("Provider Hotels");
        dto.setContactNumber("invalid-number");

        Set<ConstraintViolation<UserRegistrationDto>> violations = validator.validate(dto);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("contactNumber");
    }

    @Test
    @DisplayName("Booking DTO requires future-oriented dates")
    void bookingDtoShouldValidateDates() {
        BookingDto dto = new BookingDto();
        dto.setCheckInDate(null);
        dto.setCheckOutDate(LocalDate.now().minusDays(1));

        Set<ConstraintViolation<BookingDto>> violations = validator.validate(dto);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("checkInDate", "checkOutDate");
    }

    @Test
    @DisplayName("Booking DTO ensures check-out is after check-in")
    void bookingDtoShouldEnforceCheckoutAfterCheckin() {
        BookingDto dto = new BookingDto();
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        dto.setCheckInDate(tomorrow);
        dto.setCheckOutDate(tomorrow);

        Set<ConstraintViolation<BookingDto>> violations = validator.validate(dto);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("checkoutAfterCheckin");
    }

    @Test
    @DisplayName("Booking DTO accepts valid stay window")
    void bookingDtoWithValidDatesShouldPass() {
        BookingDto dto = new BookingDto();
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = checkIn.plusDays(2);
        dto.setCheckInDate(checkIn);
        dto.setCheckOutDate(checkOut);

        Set<ConstraintViolation<BookingDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }
}
