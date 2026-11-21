package top.easytier.miuix.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 配置文件导入解析器。
 *
 * 支持两种来源：
 * 1. 本应用持久化格式（`NetworkConfig.toJSON()`，单个对象或对象数组）；
 * 2. EasyTier 官方 TOML 配置（easytier-gui / 命令行导出的 `config.toml`），
 *    只取本客户端模型里已有的字段，其余键忽略。
 *
 * 解析阶段不做任何持久化，调用方拿到 [NetworkConfig] 后自行落盘。
 */
enum class ImportFormat { Json, Toml }

/** 面向界面的失败原因；界面据此挑字符串，不用把中文错误文案写死在数据层。 */
enum class ImportError {
    Empty,
    Unsupported,
    JsonSyntax,
    TomlSyntax,
    NoConfig,
    ReadFailed,
    TooLarge,
}

sealed interface ImportOutcome {
    data class Success(val format: ImportFormat, val configs: List<NetworkConfig>) : ImportOutcome
    data class Failure(val error: ImportError, val detail: String = "") : ImportOutcome
}

object ConfigImporter {

    /** 配置文件通常只有几 KB，1 MB 足够且能挡住误选的大文件。 */
    const val MAX_FILE_BYTES = 1 shl 20

    fun parse(rawText: String): ImportOutcome {
        val text = rawText.removePrefix("\uFEFF").trim()
        return try {
            when {
                text.isEmpty() -> ImportOutcome.Failure(ImportError.Empty)
                text.startsWith("{") -> parseJson(text)
                // 注意：TOML 的 [table] / [[table]] 也以 '[' 开头，不能只看首字符
                text.startsWith("[") && !looksLikeToml(text) -> parseJson(text)
                looksLikeToml(text) -> parseToml(text)
                else -> ImportOutcome.Failure(ImportError.Unsupported)
            }
        } catch (e: Exception) {
            // 兜底：任何未预期异常都转成可展示的失败，不让界面崩溃
            ImportOutcome.Failure(ImportError.Unsupported, e.message.orEmpty())
        }
    }

    // ---------- JSON ----------

    private fun parseJson(text: String): ImportOutcome {
        val objects: List<JSONObject> = try {
            if (text.startsWith("[")) {
                val arr = JSONArray(text)
                (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i) }
            } else {
                listOf(JSONObject(text))
            }
        } catch (e: Exception) {
            return ImportOutcome.Failure(ImportError.JsonSyntax, e.message.orEmpty())
        }
        // 普通 JSON（别的 App 的备份、随便一个 .json）结构合法但没有一个已知字段，
        // 不能当成「全默认配置」导进来，否则会凭空多出一个空网络。
        val configs = objects
            .filter { obj -> obj.keys().asSequence().any { it in recognizedJsonKeys } }
            .map { obj -> finalize(obj.toNetworkConfig()) }
        if (configs.isEmpty()) return ImportOutcome.Failure(ImportError.NoConfig)
        return ImportOutcome.Success(ImportFormat.Json, configs)
    }

    // ---------- TOML ----------

    /** TOML 表头（`[flags]`、`[[peer]]`），用整行匹配避免把 JSON 数组误判成 TOML。 */
    private val tomlHeader = Regex("""^\[\[?[\w.\-]+\]\]?$""")

    /** `NetworkConfig.toJSON()` 写出的键；用于识别「这确实是本应用的配置」。 */
    private val recognizedJsonKeys = setOf(
        "instanceId", "instanceName", "networkName", "networkSecret", "credentialFile",
        "dhcp", "virtualIpv4", "networkLength", "hostname", "devName", "mtu",
        "instanceRecvBpsLimit", "listenerUrls", "mappedListeners", "peerUrls", "proxyCidrs",
        "exitNodes", "routes", "relayNetworkWhitelist", "enableManualRoutes",
        "enableRelayNetworkWhitelist", "latencyFirst", "useSmoltcp", "disableIpv6",
        "ipv6PublicAddrAuto", "enableKcpProxy", "disableKcpInput", "enableQuicProxy",
        "disableQuicInput", "disableP2p", "p2pOnly", "lazyP2p", "bindDevice",
        "enableExitNode", "relayAllPeerRpc", "needP2p", "multiThread",
        "proxyForwardBySystem", "enableMagicDns", "enablePrivateMode", "disableEncryption",
        "disableTcpHolePunching", "disableUdpHolePunching", "disableUpnp",
        "enableUdpBroadcastRelay", "disableSymHolePunching", "advancedSettings",
        "encryptionAlgorithm", "dataCompressAlgo", "enableVpnPortal", "vpnPortalListenPort",
        "vpnPortalClients", "enableSocks5", "socks5Port", "portForwards",
    )

    private fun looksLikeToml(text: String): Boolean = text.lineSequence().any { raw ->
        val line = raw.trim()
        tomlHeader.matches(line) ||
            line.startsWith("network_name") || line.startsWith("instance_name") ||
            line.startsWith("listeners") || line.startsWith("dhcp") || line.startsWith("ipv4")
    }

    /** TOML 的一条 `key = value`；[instance] 用于区分同名的 `[[table]]` 第几次出现。 */
    private data class TomlEntry(val table: String, val instance: Int, val key: String, val value: String)

    private fun parseToml(text: String): ImportOutcome {
        val entries = mutableListOf<TomlEntry>()
        var table = ""
        var instance = 0
        val pending = StringBuilder()
        var depth = 0

        fun flushPending() {
            if (pending.isEmpty()) return
            addEntry(pending.toString(), table, instance, entries)
            pending.setLength(0)
            depth = 0
        }

        for (raw in text.split('\n')) {
            val line = stripTomlComment(raw).trim()
            if (line.isEmpty()) continue
            if (depth > 0) {
                // 跨行的数组（listeners = [ ... ]）先拼成一行再解析
                pending.append(' ').append(line)
                depth = (depth + bracketDelta(line)).coerceAtLeast(0)
                if (depth == 0) flushPending()
                continue
            }
            when {
                line.startsWith("[[") && line.contains("]]") -> {
                    table = line.substringAfter("[[").substringBefore("]]").trim()
                    instance += 1
                }
                line.startsWith("[") && line.contains("]") -> {
                    table = line.substringAfter("[").substringBefore("]").trim()
                    instance = 0
                }
                indexOfAssign(line) != null -> {
                    val delta = bracketDelta(line)
                    if (delta > 0) {
                        pending.append(line)
                        depth = delta
                    } else {
                        addEntry(line, table, instance, entries)
                    }
                }
                // 其余行（注释块、非法行）直接忽略
            }
        }
        flushPending()

        if (entries.isEmpty()) return ImportOutcome.Failure(ImportError.TomlSyntax)
        return try {
            ImportOutcome.Success(ImportFormat.Toml, listOf(buildFromToml(entries)))
        } catch (e: Exception) {
            ImportOutcome.Failure(ImportError.TomlSyntax, e.message.orEmpty())
        }
    }

    private fun buildFromToml(entries: List<TomlEntry>): NetworkConfig {
        var instanceName = ""
        var hostname = ""
        var dhcpFlag: Boolean? = null
        var ipv4 = ""
        var listeners: List<String>? = null
        var mappedListeners: List<String> = emptyList()
        var exitNodes: List<String> = emptyList()
        var routes: List<String> = emptyList()
        var socks5: String? = null
        var networkName = ""
        var networkSecret = ""
        var vpnListen: String? = null
        val peerUrls = mutableListOf<String>()
        val proxyCidrs = mutableListOf<String>()
        val flags = mutableMapOf<String, String>()
        val vpnClients = mutableMapOf<Int, MutableMap<String, String>>()
        val forwards = mutableMapOf<Int, MutableMap<String, String>>()

        for (entry in entries) {
            when (entry.table) {
                "" -> when (entry.key) {
                    "instance_name" -> instanceName = stringValue(entry.value)
                    "hostname" -> hostname = stringValue(entry.value)
                    "dhcp" -> dhcpFlag = boolValue(entry.value) ?: dhcpFlag
                    "ipv4" -> ipv4 = stringValue(entry.value)
                    "listeners" -> listeners = arrayValue(entry.value)
                    "mapped_listeners" -> mappedListeners = arrayValue(entry.value)
                    "exit_nodes" -> exitNodes = arrayValue(entry.value)
                    "routes" -> routes = arrayValue(entry.value)
                    "socks5_proxy" -> socks5 = stringValue(entry.value)
                    else -> Unit
                }
                "network_identity" -> when (entry.key) {
                    "network_name" -> networkName = stringValue(entry.value)
                    "network_secret" -> networkSecret = stringValue(entry.value)
                    else -> Unit
                }
                "peer" -> if (entry.key == "uri") {
                    stringValue(entry.value).takeIf { it.isNotBlank() }?.let { peerUrls.add(it) }
                }
                "proxy_network" -> if (entry.key == "cidr") {
                    stringValue(entry.value).takeIf { it.isNotBlank() }?.let { proxyCidrs.add(it) }
                }
                "flags" -> flags[entry.key] = entry.value
                "vpn_portal_config" -> if (entry.key == "wireguard_listen") {
                    vpnListen = stringValue(entry.value)
                }
                "vpn_portal_config.clients" ->
                    vpnClients.getOrPut(entry.instance) { mutableMapOf() }[entry.key] = stringValue(entry.value)
                "port_forward" ->
                    forwards.getOrPut(entry.instance) { mutableMapOf() }[entry.key] = stringValue(entry.value)
                else -> Unit
            }
        }

        val name = clean(networkName).ifBlank { "easytier" }
        val staticIpv4 = clean(ipv4)
        // TOML 里 dhcp 与 ipv4 二选一：显式 dhcp 优先，否则有静态地址就用静态
        val dhcp = dhcpFlag ?: staticIpv4.isBlank()

        var config = NetworkConfig(
            instanceId = UUID.randomUUID().toString(),
            instanceName = clean(instanceName),
            networkName = name,
            networkSecret = clean(networkSecret),
            hostname = clean(hostname).takeIf { it.isNotBlank() },
            dhcp = dhcp,
            virtualIpv4 = staticIpv4.ifBlank { "10.144.144.1/24" },
            networkLength = staticIpv4.substringAfter('/', "24").trim().toIntOrNull() ?: 24,
            listenerUrls = listeners?.takeIf { it.isNotEmpty() } ?: NetworkConfig().listenerUrls,
            mappedListeners = mappedListeners.map(::clean).filter { it.isNotBlank() },
            peerUrls = peerUrls.map(::clean).filter { it.isNotBlank() },
            proxyCidrs = proxyCidrs.map(::clean).filter { it.isNotBlank() },
            exitNodes = exitNodes.map(::clean).filter { it.isNotBlank() },
            routes = routes.map(::clean).filter { it.isNotBlank() },
            enableManualRoutes = routes.any { it.isNotBlank() },
            enableSocks5 = socks5 != null,
            socks5Port = portOf(socks5) ?: NetworkConfig().socks5Port,
            enableVpnPortal = vpnListen != null,
            vpnPortalListenPort = portOf(vpnListen) ?: NetworkConfig().vpnPortalListenPort,
            vpnPortalClients = vpnClients.values.map { item ->
                VpnPortalClient(
                    name = clean(item["name"].orEmpty()),
                    virtualIp = clean(item["virtual_ip"].orEmpty()),
                )
            }.filter { it.name.isNotBlank() && it.virtualIp.isNotBlank() },
            portForwards = forwards.values.map { item ->
                val bindAddr = item["bind_addr"].orEmpty()
                val dstAddr = item["dst_addr"].orEmpty()
                PortForwardConfig(
                    bindIp = bindAddr.substringBeforeLast(':', ""),
                    bindPort = portOf(bindAddr) ?: 65535,
                    dstIp = dstAddr.substringBeforeLast(':', ""),
                    dstPort = portOf(dstAddr) ?: 65535,
                    proto = item["proto"].orEmpty().ifBlank { "tcp" },
                )
            }.filter { it.bindIp.isNotBlank() && it.dstIp.isNotBlank() },
        )
        config = applyFlags(config, flags)
        return finalize(config)
    }

    /** 把 `[flags]` 映射回模型；注意 `enable_* = false` 这类反向键。 */
    private fun applyFlags(config: NetworkConfig, flags: Map<String, String>): NetworkConfig {
        fun bool(key: String): Boolean? = flags[key]?.let { boolValue(it) }
        fun text(key: String): String? = flags[key]?.let { clean(stringValue(it)) }

        var result = config
        bool("bind_device")?.let { result = result.copy(bindDevice = it) }
        bool("enable_ipv6")?.let { result = result.copy(disableIpv6 = !it) }
        bool("latency_first")?.let { result = result.copy(latencyFirst = it) }
        bool("disable_p2p")?.let { result = result.copy(disableP2p = it) }
        bool("p2p_only")?.let { result = result.copy(p2pOnly = it) }
        bool("lazy_p2p")?.let { result = result.copy(lazyP2p = it) }
        bool("need_p2p")?.let { result = result.copy(needP2p = it) }
        bool("enable_exit_node")?.let { result = result.copy(enableExitNode = it) }
        bool("multi_thread")?.let { result = result.copy(multiThread = it) }
        bool("enable_kcp_proxy")?.let { result = result.copy(enableKcpProxy = it) }
        bool("disable_kcp_input")?.let { result = result.copy(disableKcpInput = it) }
        bool("enable_quic_proxy")?.let { result = result.copy(enableQuicProxy = it) }
        bool("disable_quic_input")?.let { result = result.copy(disableQuicInput = it) }
        bool("enable_encryption")?.let { result = result.copy(disableEncryption = !it) }
        bool("disable_tcp_hole_punching")?.let { result = result.copy(disableTcpHolePunching = it) }
        bool("disable_udp_hole_punching")?.let { result = result.copy(disableUdpHolePunching = it) }
        bool("disable_sym_hole_punching")?.let { result = result.copy(disableSymHolePunching = it) }
        bool("disable_upnp")?.let { result = result.copy(disableUpnp = it) }
        bool("relay_all_peer_rpc")?.let { result = result.copy(relayAllPeerRpc = it) }
        bool("enable_udp_broadcast_relay")?.let { result = result.copy(enableUdpBroadcastRelay = it) }
        bool("proxy_forward_by_system")?.let { result = result.copy(proxyForwardBySystem = it) }
        bool("use_smoltcp")?.let { result = result.copy(useSmoltcp = it) }
        bool("accept_dns")?.let { result = result.copy(enableMagicDns = it) }
        bool("private_mode")?.let { result = result.copy(enablePrivateMode = it) }
        bool("ipv6_public_addr_auto")?.let { result = result.copy(ipv6PublicAddrAuto = it) }

        text("relay_network_whitelist")?.let {
            result = result.copy(enableRelayNetworkWhitelist = true, relayNetworkWhitelist = it)
        }
        text("dev_name")?.takeIf { it.isNotBlank() }?.let { result = result.copy(devName = it) }
        flags["mtu"]?.let { raw -> raw.trim().toIntOrNull()?.let { result = result.copy(mtu = it) } }
        flags["instance_recv_bps_limit"]?.let { raw ->
            stringValue(raw).toLongOrNull()?.let { result = result.copy(instanceRecvBpsLimit = it) }
        }
        text("encryption_algorithm")?.takeIf { it.isNotBlank() }?.let {
            result = result.copy(encryptionAlgorithm = EncryptionAlgorithm.fromValue(it))
        }
        text("data_compress_algo")?.takeIf { it.isNotBlank() }?.let {
            result = result.copy(dataCompressAlgo = CompressionAlgo.fromValue(it))
        }
        return result
    }

    // ---------- 值解析 ----------

    private fun indexOfAssign(line: String): Int? {
        var inString = false
        var escaped = false
        for (i in line.indices) {
            val c = line[i]
            when {
                escaped -> escaped = false
                inString && c == '\\' -> escaped = true
                c == '"' -> inString = !inString
                !inString && c == '=' -> return i
            }
        }
        return null
    }

    private fun stripTomlComment(line: String): String {
        var inString = false
        var escaped = false
        for (i in line.indices) {
            val c = line[i]
            when {
                escaped -> escaped = false
                inString && c == '\\' -> escaped = true
                c == '"' -> inString = !inString
                !inString && c == '#' -> return line.substring(0, i)
            }
        }
        return line
    }

    /** 未闭合的方括号差值，用于把跨行的数组拼成一行。 */
    private fun bracketDelta(line: String): Int {
        var inString = false
        var escaped = false
        var delta = 0
        for (c in line) {
            when {
                escaped -> escaped = false
                inString && c == '\\' -> escaped = true
                c == '"' -> inString = !inString
                !inString && c == '[' -> delta++
                !inString && c == ']' -> delta--
            }
        }
        return delta
    }

    private fun addEntry(line: String, table: String, instance: Int, out: MutableList<TomlEntry>) {
        val idx = indexOfAssign(line) ?: return
        val key = line.substring(0, idx).trim()
        if (key.isEmpty()) return
        out.add(TomlEntry(table, instance, key, line.substring(idx + 1).trim()))
    }

    private fun stringValue(raw: String): String {
        val value = raw.trim()
        if (value.length >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            val body = value.substring(1, value.length - 1)
            return body
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
        }
        return value
    }

    private fun boolValue(raw: String): Boolean? = when (raw.trim().lowercase()) {
        "true" -> true
        "false" -> false
        else -> null
    }

    private fun arrayValue(raw: String): List<String> {
        val value = raw.trim()
        if (!value.startsWith("[") || !value.endsWith("]")) return emptyList()
        val body = value.substring(1, value.length - 1)
        val items = mutableListOf<String>()
        val current = StringBuilder()
        var inString = false
        var escaped = false
        for (c in body) {
            when {
                escaped -> {
                    current.append(c)
                    escaped = false
                }
                inString && c == '\\' -> {
                    current.append(c)
                    escaped = true
                }
                c == '"' -> {
                    inString = !inString
                    current.append(c)
                }
                !inString && c == ',' -> {
                    items.add(current.toString().trim())
                    current.setLength(0)
                }
                else -> current.append(c)
            }
        }
        if (current.isNotBlank()) items.add(current.toString().trim())
        return items.map { stringValue(it) }.filter { it.isNotBlank() }
    }

    private fun portOf(url: String?): Int? {
        if (url.isNullOrBlank()) return null
        return url.substringAfterLast(':', "").trim().toIntOrNull()
    }

    /**
     * 导入去引号/换行：这些字符会原样拼进 TOML，未转义的双引号会让整份配置解析失败。
     */
    private fun clean(value: String): String =
        value.replace("\"", "").replace("\\", "").replace("\r", " ").replace("\n", " ").trim()

    /** 导入统一分配新的实例 ID，避免与手机上已有配置撞车。 */
    private fun finalize(config: NetworkConfig): NetworkConfig {
        val name = config.networkName.let(::clean).ifBlank { "easytier" }
        val instanceName = clean(config.instanceName)
        return config.copy(
            instanceId = UUID.randomUUID().toString(),
            instanceName = if (instanceName.isBlank()) name else instanceName,
            networkName = name,
            networkSecret = clean(config.networkSecret),
            peerUrls = config.peerUrls.map(::clean).filter { it.isNotBlank() },
            listenerUrls = config.listenerUrls.map(::clean).filter { it.isNotBlank() },
        )
    }
}
