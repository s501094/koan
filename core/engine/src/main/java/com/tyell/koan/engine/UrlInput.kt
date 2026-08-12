package com.tyell.koan.engine

/**
 * Turns whatever the user typed into something loadable.
 *
 * Deliberately hand-rolled instead of pulling in mozac's search subsystem:
 * that drags in region detection, the search-engine bundle and a middleware
 * stack, all to answer a question we can answer in twenty lines.
 */
object UrlInput {

    /** Reasonable default for a privacy-minded browser. */
    const val DEFAULT_SEARCH_TEMPLATE = "https://duckduckgo.com/?q={query}"

    private val SCHEME = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")
    private val LOOKS_LIKE_HOST = Regex("^[^\\s/?#]+\\.[a-zA-Z]{2,}(:\\d+)?([/?#].*)?$")

    fun toUrl(raw: String, searchTemplate: String = DEFAULT_SEARCH_TEMPLATE): String {
        val input = raw.trim()
        if (input.isEmpty()) return "about:blank"

        if (SCHEME.containsMatchIn(input)) return input
        if (input.startsWith("about:") || input.startsWith("data:")) return input
        if (input == "localhost" || input.startsWith("localhost:")) return "http://$input"

        // A single token with a dot and no spaces is almost certainly a host.
        if (!input.contains(' ') && LOOKS_LIKE_HOST.matches(input)) return "https://$input"

        return searchTemplate.replace("{query}", encode(input))
    }

    fun isSearch(raw: String, searchTemplate: String = DEFAULT_SEARCH_TEMPLATE): Boolean =
        toUrl(raw, searchTemplate).startsWith(searchTemplate.substringBefore("{query}"))

    /** Display form for the URL bar — drop the scheme and a leading www. */
    fun prettify(url: String): String = url
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.")
        .removeSuffix("/")

    fun host(url: String): String =
        runCatching { java.net.URI(url).host?.removePrefix("www.") }.getOrNull() ?: url

    private fun encode(s: String): String =
        java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20")
}
