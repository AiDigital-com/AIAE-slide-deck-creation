package com.aidigital.strategyplanning.service.casestudy.models;

/**
 * One reviewed One-Page Case Study field value.
 *
 * @param key   template token key
 * @param value reviewed value; blank clears the field's box
 */
public record OnePageFieldValue(String key, String value) {

}
