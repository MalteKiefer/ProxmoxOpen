package de.kiefer_networks.proxmoxopen.domain.util

/**
 * Parse a Proxmox `tags` string into a list of trimmed, non-empty tags.
 *
 * Proxmox accepts `;`, `,` and whitespace as tag separators depending on
 * version. This helper tolerates all of them and normalises the result so
 * UI/filter code can rely on a plain `List<String>`.
 */
fun parseTags(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return raw
        .split(';', ',', ' ', '\t', '\n')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
}

/**
 * Render a list of tags back into the canonical Proxmox `tags` string
 * (semicolon-separated). Returns `null` when the list is empty so callers
 * can send an empty payload to clear tags.
 */
fun formatTags(tags: List<String>): String? {
    val cleaned = tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    return if (cleaned.isEmpty()) null else cleaned.joinToString(";")
}
