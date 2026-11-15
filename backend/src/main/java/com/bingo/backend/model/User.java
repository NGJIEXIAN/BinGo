package com.bingo.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private String userId;
    private String fullName;
    private String email;
    private String faculty;
    private int totalPoints;
    private double co2Reduced;
}
