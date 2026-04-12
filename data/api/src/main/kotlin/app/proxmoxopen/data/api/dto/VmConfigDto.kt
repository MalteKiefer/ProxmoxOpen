package app.proxmoxopen.data.api.dto

import kotlinx.serialization.Serializable

/**
 * QEMU VM config from `GET /nodes/{n}/qemu/{vmid}/config`.
 * Only editabe/displayable fields.
 */
@Serializable
data class VmConfigDto(
    // General
    val name: String? = null,
    val description: String? = null,
    val tags: String? = null,
    val onboot: Int? = null,
    val startup: String? = null,
    val protection: Int? = null,
    // Hardware
    val bios: String? = null,          // seabios / ovmf
    val machine: String? = null,       // pc, q35, etc.
    val cpu: String? = null,           // host, kvm64, etc.
    val sockets: Int? = null,
    val cores: Int? = null,
    val memory: Int? = null,           // MB (fixed) or as string for ballooning
    val balloon: Int? = null,          // min memory for ballooning
    val numa: Int? = null,
    val hotplug: String? = null,
    val scsihw: String? = null,        // virtio-scsi-pci, etc.
    val ostype: String? = null,        // l26, win11, other
    val vga: String? = null,           // std, virtio, qxl, etc.
    val agent: String? = null,         // "enabled=1" or "0"
    val boot: String? = null,          // "order=scsi0;ide2;net0"
    // Disks (top-level keys)
    val scsi0: String? = null,
    val scsi1: String? = null,
    val scsi2: String? = null,
    val scsi3: String? = null,
    val virtio0: String? = null,
    val virtio1: String? = null,
    val ide0: String? = null,
    val ide1: String? = null,
    val ide2: String? = null,          // often CDROM
    val sata0: String? = null,
    val sata1: String? = null,
    val efidisk0: String? = null,
    val tpmstate0: String? = null,
    // Network
    val net0: String? = null,
    val net1: String? = null,
    val net2: String? = null,
    val net3: String? = null,
    // Serial / USB
    val serial0: String? = null,
    val usb0: String? = null,
    val usb1: String? = null,
    val usb2: String? = null,
) {
    fun allDisks(): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        scsi0?.let { result += "scsi0" to it }; scsi1?.let { result += "scsi1" to it }
        scsi2?.let { result += "scsi2" to it }; scsi3?.let { result += "scsi3" to it }
        virtio0?.let { result += "virtio0" to it }; virtio1?.let { result += "virtio1" to it }
        ide0?.let { result += "ide0" to it }; ide1?.let { result += "ide1" to it }; ide2?.let { result += "ide2" to it }
        sata0?.let { result += "sata0" to it }; sata1?.let { result += "sata1" to it }
        efidisk0?.let { result += "efidisk0" to it }
        tpmstate0?.let { result += "tpmstate0" to it }
        return result
    }

    fun allNets(): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        net0?.let { result += "net0" to it }; net1?.let { result += "net1" to it }
        net2?.let { result += "net2" to it }; net3?.let { result += "net3" to it }
        return result
    }
}
