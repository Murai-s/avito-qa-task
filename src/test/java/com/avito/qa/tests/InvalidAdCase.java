package com.avito.qa.tests;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public class InvalidAdCase {

    private String description;
    private Object invalidBody;
    private String expectedMessage;
}