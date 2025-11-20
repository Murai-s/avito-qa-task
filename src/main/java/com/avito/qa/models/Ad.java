package com.avito.qa.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ad {

    private String createdAt;
    private String id;
    private String name;
    private Integer price;
    private Integer sellerId;
    private Statistics statistics;
}
