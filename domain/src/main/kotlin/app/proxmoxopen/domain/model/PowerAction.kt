package app.proxmoxopen.domain.model

enum class PowerAction(val apiPath: String, val destructive: Boolean) {
    START("start", destructive = false),
    STOP("stop", destructive = true),
    SHUTDOWN("shutdown", destructive = false),
    REBOOT("reboot", destructive = false),
    SUSPEND("suspend", destructive = false),
    RESUME("resume", destructive = false),
    RESET("reset", destructive = true),
}
