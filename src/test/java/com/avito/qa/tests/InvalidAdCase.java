package com.avito.qa.tests;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class InvalidAdCase {

    private String description;
    private Object invalidBody;
    private String expectedMessage;
}