package com.example.api.model;

import java.util.List;
import lombok.Data;

@Data
public class PhishingScanResult  {
    private int riskScore;
    private String riskLevel;
    private List<String> threats;
}
