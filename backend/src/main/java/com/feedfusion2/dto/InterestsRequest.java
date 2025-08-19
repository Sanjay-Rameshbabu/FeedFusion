package com.feedfusion2.dto;

import lombok.Data;
import java.util.List;

@Data
public class InterestsRequest {
    private List<String> interests;
}