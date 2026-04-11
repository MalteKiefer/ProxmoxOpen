package app.proxmoxopen.domain.model

sealed interface Credentials {
    data class UserPassword(
        val username: String,
        val realm: Realm,
        val password: String,
        val totp: String? = null,
    ) : Credentials

    data class ApiToken(
        val username: String,
        val realm: Realm,
        val tokenId: String,
        val tokenSecret: String,
    ) : Credentials {
        /** Header value: `PVEAPIToken=user@realm!tokenid=secret`. */
        val headerValue: String
            get() = "PVEAPIToken=$username@${realm.apiKey}!$tokenId=$tokenSecret"
    }
}
