package com.example.api.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PhishingScanResult  {
    private int riskScore;
    private String riskLevel;
    private List<String> threats;
}
