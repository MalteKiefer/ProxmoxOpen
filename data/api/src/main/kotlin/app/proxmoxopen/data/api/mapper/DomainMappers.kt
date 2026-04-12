package app.proxmoxopen.data.api.mapper

import app.proxmoxopen.data.api.dto.ClusterResourceDto
import app.proxmoxopen.data.api.dto.ClusterStatusDto
import app.proxmoxopen.data.api.dto.BackupVolumeDto
import app.proxmoxopen.data.api.dto.ContainerCurrentStatusDto
import app.proxmoxopen.data.api.dto.GuestConfigDto
import app.proxmoxopen.data.api.dto.InterfaceDto
import app.proxmoxopen.data.api.dto.SnapshotDto
import app.proxmoxopen.domain.model.ContainerStatus
import app.proxmoxopen.domain.model.Backup
import app.proxmoxopen.domain.model.GuestConfig
import app.proxmoxopen.domain.model.InterfaceIp
import app.proxmoxopen.domain.model.NetworkInterface
import app.proxmoxopen.domain.model.Snapshot
import app.proxmoxopen.data.api.dto.GuestStatusDto
import app.proxmoxopen.data.api.dto.NodeListDto
import app.proxmoxopen.data.api.dto.NodeStatusDto
import app.proxmoxopen.data.api.dto.RrdPointDto
import app.proxmoxopen.data.api.dto.TaskDto
import app.proxmoxopen.data.api.dto.TaskStatusDto
import app.proxmoxopen.domain.model.Cluster
import app.proxmoxopen.domain.model.Guest
import app.proxmoxopen.domain.model.GuestStatus
import app.proxmoxopen.domain.model.GuestType
import app.proxmoxopen.domain.model.Node
import app.proxmoxopen.domain.model.NodeStatus
import app.proxmoxopen.domain.model.ProxmoxTask
import app.proxmoxopen.domain.model.RrdPoint
import app.proxmoxopen.domain.model.TaskState

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
    memUsed = mem ?: 0,
    memTotal = maxmem ?: 0,
    diskUsed = disk ?: 0,
    diskTotal = maxdisk ?: 0,
    uptimeSeconds = uptime ?: 0,
    loadAverage = emptyList(),
)

fun NodeStatusDto.toDomain(nodeName: String): Node = Node(
    name = nodeName,
    status = NodeStatus.ONLINE,
    cpuUsage = cpu ?: 0.0,
    cpuCount = cpuinfo?.cpus ?: 0,
    memUsed = memory?.used ?: 0,
    memTotal = memory?.total ?: 0,
    diskUsed = rootfs?.used ?: 0,
    diskTotal = rootfs?.total ?: 0,
    uptimeSeconds = uptime ?: 0,
    loadAverage = loadavg?.mapNotNull { it.toDoubleOrNull() } ?: emptyList(),
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
