package com.example.majorproject;

import lombok.*;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {
    // it is UserRequestDto
    private String userName;
    private String email;
    private String name;
    private int age;
}
