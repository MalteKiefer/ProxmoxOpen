package de.kiefer_networks.proxmoxopen.domain.model

enum class Realm(val apiKey: String) {
    PAM("pam"),
    PVE("pve"),
    PVE_TOKEN("pve-token"),
    ;

    companion object {
        fun fromApiKey(key: String): Realm? = entries.firstOrNull { it.apiKey == key }
    }
}
