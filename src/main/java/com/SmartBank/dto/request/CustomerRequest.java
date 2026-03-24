package com.SmartBank.dto.request;


import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerRequest {
    @NotBlank(message = "FullName can not be null")
    @Size(max = 100, message = "FullName's max length is 100")
    private String fullName;

    @NotBlank(message = "Email can not be null")
    @Email(message = "Invalid email")
    private String email;

    @NotBlank(message = "Phone can not be null")
    @Size(max = 11, min = 10, message = "Invalid phone number")
    private String phone;

    private String address;

    @NotNull(message = "dob can not be null")
    @Past(message = "The dob must be in the past")
    private LocalDate dateOfBirth;
}
