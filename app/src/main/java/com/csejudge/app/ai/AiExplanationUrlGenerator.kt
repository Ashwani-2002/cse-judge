package com.csejudge.app.ai

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Generates Google AI Mode query URLs using the `udm=50` parameter:
 * `https://www.google.com/search?udm=50&q={ENCODED_TEXT}`
 *
 * Guarantees proper single-time percent encoding of spaces, newlines,
 * Unicode / Hindi text, and special characters. Compatible with pure JVM unit tests
 * and Android runtime environments.
 */
object AiExplanationUrlGenerator {

    const val SEARCH_BASE_URL = "https://www.google.com/search?udm=50&q="

    /**
     * Generates a complete Google AI Mode URL from a raw query string.
     */
    fun generateUrl(query: String): String {
        val trimmed = query.trim()
        val encoded = encodeQuery(trimmed)
        return "$SEARCH_BASE_URL$encoded"
    }

    /**
     * Percent-encodes text using UTF-8 standard encoding with `%20` for spaces.
     */
    fun encodeQuery(text: String): String {
        return URLEncoder.encode(text, StandardCharsets.UTF_8.name())
            .replace("+", "%20")
    }
}
