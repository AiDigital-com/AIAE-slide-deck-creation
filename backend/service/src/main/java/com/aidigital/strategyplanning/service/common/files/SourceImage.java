package com.aidigital.strategyplanning.service.common.files;

/**
 * One uploaded image source (for example a screenshot of a reporting dashboard). Images are not
 * reduced to text; they are handed to the AI drafting engine as images so it can read figures
 * straight off the screenshot.
 *
 * @param fileName original filename of the uploaded image
 * @param type     detected image type, which carries the MIME type sent to the AI engine
 * @param bytes    raw image bytes
 */
public record SourceImage(String fileName, SourceImageType type, byte[] bytes) {

}
