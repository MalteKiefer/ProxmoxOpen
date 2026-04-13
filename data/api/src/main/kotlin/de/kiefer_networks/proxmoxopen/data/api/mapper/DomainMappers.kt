package de.kiefer_networks.proxmoxopen.data.api.mapper

import de.kiefer_networks.proxmoxopen.data.api.dto.ClusterResourceDto
import de.kiefer_networks.proxmoxopen.data.api.dto.ClusterStatusDto
import de.kiefer_networks.proxmoxopen.data.api.dto.BackupVolumeDto
import de.kiefer_networks.proxmoxopen.data.api.dto.StorageInfoDto
import de.kiefer_networks.proxmoxopen.data.api.dto.ContainerCurrentStatusDto
import de.kiefer_networks.proxmoxopen.data.api.dto.GuestConfigDto
import de.kiefer_networks.proxmoxopen.data.api.dto.InterfaceDto
import de.kiefer_networks.proxmoxopen.data.api.dto.SnapshotDto
import de.kiefer_networks.proxmoxopen.domain.model.ContainerStatus
import de.kiefer_networks.proxmoxopen.domain.model.Backup
import de.kiefer_networks.proxmoxopen.domain.model.GuestConfig
import de.kiefer_networks.proxmoxopen.domain.model.InterfaceIp
import de.kiefer_networks.proxmoxopen.domain.model.NetworkInterface
import de.kiefer_networks.proxmoxopen.domain.model.Snapshot
import de.kiefer_networks.proxmoxopen.data.api.dto.GuestStatusDto
import de.kiefer_networks.proxmoxopen.data.api.dto.NodeListDto
import de.kiefer_networks.proxmoxopen.data.api.dto.NodeStatusDto
import de.kiefer_networks.proxmoxopen.data.api.dto.RrdPointDto
import de.kiefer_networks.proxmoxopen.data.api.dto.TaskDto
import de.kiefer_networks.proxmoxopen.data.api.dto.TaskStatusDto
import de.kiefer_networks.proxmoxopen.domain.model.Cluster
import de.kiefer_networks.proxmoxopen.domain.model.StorageInfo
import de.kiefer_networks.proxmoxopen.domain.model.Guest
import de.kiefer_networks.proxmoxopen.domain.model.GuestStatus
import de.kiefer_networks.proxmoxopen.domain.model.GuestType
import de.kiefer_networks.proxmoxopen.domain.model.Node
import de.kiefer_networks.proxmoxopen.domain.model.NodeStatus
import de.kiefer_networks.proxmoxopen.domain.model.ProxmoxTask
import de.kiefer_networks.proxmoxopen.domain.model.RrdPoint
import de.kiefer_networks.proxmoxopen.domain.model.TaskState

fun buildCluster(statusEntries: List<ClusterStatusDto>, nodeEntries: List<NodeListDto>): Cluster {
    val clusterEntry = statusEntries.firstOrNull { it.type == "cluster" }
    val nodes = nodeEntries.map { it.toDomain() }
    return Cluster(
        name = clusterEntry?.name ?: "standalone",
        quorate = (clusterEntry?.quorate ?: 1) == 1,
        version = clusterEntry?.version ?: 0,
        nodes = nodes,
    )
}

fun NodeListDto.toDomain(): Node = Node(
    name = node,
    status = when (status.lowercase()) {
        "online" -> NodeStatus.ONLINE
        "offline" -> NodeStatus.OFFLINE
        else -> NodeStatus.UNKNOWN
    },
    cpuUsage = cpu ?: 0.0,
    cpuCount = maxcpu ?: 0,
    cpuModel = null,
    memUsed = mem ?: 0,
    memTotal = maxmem ?: 0,
    diskUsed = disk ?: 0,
    diskTotal = maxdisk ?: 0,
    swapUsed = 0,
    swapTotal = 0,
    uptimeSeconds = uptime ?: 0,
    loadAverage = emptyList(),
    ioDelay = null,
    ksmShared = null,
    kernelVersion = null,
    pveVersion = null,
)

fun NodeStatusDto.toDomain(nodeName: String): Node = Node(
    name = nodeName,
    status = NodeStatus.ONLINE,
    cpuUsage = cpu ?: 0.0,
    cpuCount = cpuinfo?.cpus ?: 0,
    cpuModel = cpuinfo?.model,
    memUsed = memory?.used ?: 0,
    memTotal = memory?.total ?: 0,
    diskUsed = rootfs?.used ?: 0,
    diskTotal = rootfs?.total ?: 0,
    swapUsed = swap?.used ?: 0,
    swapTotal = swap?.total ?: 0,
    uptimeSeconds = uptime ?: 0,
    loadAverage = loadavg?.mapNotNull { it.toDoubleOrNull() } ?: emptyList(),
    ioDelay = iowait,
    ksmShared = ksm?.shared,
    kernelVersion = kversion,
    pveVersion = pveversion,
)

fun ClusterResourceDto.toGuestOrNull(): Guest? {
    val vm = vmid ?: return null
    val nodeName = node ?: return null
    val typeEnum = GuestType.fromApiPath(type) ?: return null
    return Guest(
        vmid = vm,
        name = name ?: "vm-$vm",
        node = nodeName,
        type = typeEnum,
        status = GuestStatus.fromProxmox(status),
        cpuUsage = cpu ?: 0.0,
        cpuCount = maxcpu ?: 0,
        memUsed = mem ?: 0,
        memTotal = maxmem ?: 0,
        diskUsed = disk ?: 0,
        diskTotal = maxdisk ?: 0,
        uptimeSeconds = uptime ?: 0,
        tags = tags?.split(';', ',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
    )
}

fun GuestStatusDto.toDomain(node: String, type: GuestType): Guest = Guest(
    vmid = vmid ?: 0,
    name = name ?: "vm-${vmid ?: 0}",
    node = node,
    type = type,
    status = GuestStatus.fromProxmox(status ?: qmpstatus),
    cpuUsage = cpu ?: 0.0,
    cpuCount = cpus ?: 0,
    memUsed = mem ?: 0,
    memTotal = maxmem ?: 0,
    diskUsed = disk ?: 0,
    diskTotal = maxdisk ?: 0,
    uptimeSeconds = uptime ?: 0,
    tags = tags?.split(';', ',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
)

fun GuestConfigDto.toGuestConfig(): GuestConfig {
    val nets = allNetworkInterfaces().map { (id, raw) -> NetworkInterface.parse(id, raw) }
    return GuestConfig(
        name = name ?: hostname ?: "",
        hostname = hostname,
        onboot = onboot == 1,
        startup = startup,
        description = description,
        protection = protection == 1,
        unprivileged = unprivileged == 1,
        cores = cores,
        cpulimit = cpulimit,
        cpuunits = cpuunits,
        memory = memory,
        swap = swap,
        nameserver = nameserver,
        searchdomain = searchdomain,
        networkInterfaces = nets,
        tags = tags,
        features = features,
        arch = arch,
        ostype = ostype,
    )
}

fun RrdPointDto.toDomain(): RrdPoint = RrdPoint(
    time = time,
    cpu = cpu,
    memUsed = mem ?: memused,
    memTotal = maxmem ?: memtotal,
    netIn = netin,
    netOut = netout,
    diskRead = diskread,
    diskWrite = diskwrite,
    ioWait = iowait,
)

fun TaskDto.toDomain(): ProxmoxTask = ProxmoxTask(
    upid = upid,
    node = node,
    type = type,
    id = id,
    user = user,
    state = status.toTaskState(exitstatus),
    startTime = starttime,
    endTime = endtime,
    exitStatus = exitstatus,
)

fun TaskStatusDto.toDomain(): ProxmoxTask = ProxmoxTask(
    upid = upid,
    node = node,
    type = type,
    id = null,
    user = user,
    state = status.toTaskState(exitstatus),
    startTime = starttime,
    endTime = null,
    exitStatus = exitstatus,
)

fun SnapshotDto.toSnapshot(): Snapshot = Snapshot(
    name = name,
    description = description,
    snaptime = snaptime,
    parent = parent,
    vmstate = vmstate == 1,
)

fun ContainerCurrentStatusDto.toContainerStatus(
    nodeName: String,
    interfaces: List<InterfaceDto>,
): ContainerStatus = ContainerStatus(
    vmid = vmid ?: 0,
    name = name ?: "ct-${vmid ?: 0}",
    status = GuestStatus.fromProxmox(status),
    uptime = uptime ?: 0,
    haState = ha?.state,
    node = nodeName,
    type = GuestType.LXC,
    unprivileged = true,
    ostype = type,
    pid = pid,
    cpuUsage = cpu ?: 0.0,
    cpuCount = cpus ?: 0,
    memUsed = mem ?: 0,
    memTotal = maxmem ?: 0,
    swapUsed = swap ?: 0,
    swapTotal = maxswap ?: 0,
    diskUsed = disk ?: 0,
    diskTotal = maxdisk ?: 0,
    netIn = netin ?: 0,
    netOut = netout ?: 0,
    diskRead = diskread ?: 0,
    diskWrite = diskwrite ?: 0,
    ipAddresses = interfaces.map { iface ->
        InterfaceIp(
            name = iface.name ?: "?",
            hwaddr = iface.hwaddr,
            inet = iface.inet,
            inet6 = iface.inet6,
        )
    },
)

private fun String?.toTaskState(exit: String?): TaskState = when {
    equals("running", ignoreCase = true) -> TaskState.RUNNING
    equals("stopped", ignoreCase = true) || equals("OK", ignoreCase = true) ->
        if (exit.isNullOrBlank() || exit.equals("OK", ignoreCase = true)) TaskState.OK
        else TaskState.FAILED
    else -> TaskState.UNKNOWN
}

fun BackupVolumeDto.toBackup(storage: String): Backup = Backup(
    volid = volid,
    vmid = vmid ?: 0,
    createdAt = ctime ?: 0,
    size = size ?: 0,
    format = format,
    notes = notes,
    protected = protected == 1,
    storage = storage,
)

fun de.kiefer_networks.proxmoxopen.data.api.dto.VmCurrentStatusDto.toVmStatus(
    nodeName: String,
    agentInterfaces: List<de.kiefer_networks.proxmoxopen.data.api.dto.AgentInterfaceDto>,
): de.kiefer_networks.proxmoxopen.domain.model.VmStatus = de.kiefer_networks.proxmoxopen.domain.model.VmStatus(
    vmid = vmid ?: 0,
    name = name ?: "vm-${vmid ?: 0}",
    status = GuestStatus.fromProxmox(status ?: qmpstatus),
    qmpStatus = qmpstatus,
    uptime = uptime ?: 0,
    haState = ha?.state,
    node = nodeName,
    pid = pid,
    agentEnabled = agent == 1,
    cpuUsage = cpu ?: 0.0,
    cpuCount = cpus ?: 0,
    memUsed = mem ?: 0,
    memTotal = maxmem ?: 0,
    diskUsed = disk ?: 0,
    diskTotal = maxdisk ?: 0,
    netIn = netin ?: 0,
    netOut = netout ?: 0,
    diskRead = diskread ?: 0,
    diskWrite = diskwrite ?: 0,
    ipAddresses = agentInterfaces.flatMap { iface ->
        val inet4 = iface.ip_addresses?.firstOrNull { it.ip_address_type == "ipv4" }
        val inet6 = iface.ip_addresses?.firstOrNull { it.ip_address_type == "ipv6" }
        if (inet4 != null || inet6 != null) {
            listOf(InterfaceIp(
                name = iface.name ?: "?",
                hwaddr = iface.hardware_address,
                inet = inet4?.let { "${it.ip_address}/${it.prefix}" },
                inet6 = inet6?.let { "${it.ip_address}/${it.prefix}" },
            ))
        } else emptyList()
    },
    runningMachine = running_machine,
    runningQemu = running_qemu,
)

fun de.kiefer_networks.proxmoxopen.data.api.dto.VmConfigDto.toVmConfig(): de.kiefer_networks.proxmoxopen.domain.model.VmConfig {
    val nets = allNets().map { (id, raw) -> de.kiefer_networks.proxmoxopen.domain.model.VmNetInterface.parse(id, raw) }
    return de.kiefer_networks.proxmoxopen.domain.model.VmConfig(
        name = name ?: "",
        description = description,
        onboot = onboot == 1,
        startup = startup,
        protection = protection == 1,
        bios = bios,
        machine = machine,
        cpuType = cpu,
        sockets = sockets,
        cores = cores,
        memory = memory,
        balloon = balloon,
        numa = numa == 1,
        scsihw = scsihw,
        ostype = ostype,
        vga = vga,
        agentEnabled = agent?.contains("enabled=1") == true || agent == "1",
        bootOrder = boot,
        disks = allDisks(),
        networkInterfaces = nets,
        tags = tags,
    )
}

fun StorageInfoDto.toStorageInfo(): StorageInfo = StorageInfo(
    name = storage,
    type = type ?: "unknown",
    content = content ?: "",
    enabled = (enabled ?: 1) == 1,
    active = (active ?: 0) == 1,
    total = total ?: 0,
    used = used ?: 0,
    available = avail ?: 0,
)
