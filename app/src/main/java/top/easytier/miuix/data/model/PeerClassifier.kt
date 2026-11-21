package top.easytier.miuix.data.model

/**
 * 状态页的对端分类：区分「网络设备」与「公共节点」。
 *
 * 上游并没有直接给出「这是公共中继」的字段，只能用可观测信息推断。判定顺序很关键：
 *
 * 1. **同虚拟网段优先**：对端的虚拟 IP 和本机虚拟 IP 在同一网段，说明它和我属于
 *    同一张 EasyTier 网络，无论走直连还是中继都必须是「网络设备」。
 *    这条规则只看虚拟 IP（稳定不变），因此不会像「直连/中继」那样随链路切换来回跳。
 * 2. 走中继（[Route.isRelay]）→ 公共节点；
 * 3. 直连时看连接的远端地址：域名或公网 IP 视为公共节点，
 *    私网段（10/8、172.16/12、192.168/16、127/8、169.254/16）视为局域网设备。
 *
 * 全部为纯函数，方便单元测试。
 */
object PeerClassifier {

    /** 拆成（网络设备, 公共节点），保持原有顺序 */
    fun split(
        pairs: List<PeerRoutePair>,
        localIpv4: Ipv4Inet? = null,
    ): Pair<List<PeerRoutePair>, List<PeerRoutePair>> {
        val devices = ArrayList<PeerRoutePair>(pairs.size)
        val publics = ArrayList<PeerRoutePair>()
        pairs.forEach { pair ->
            if (isPublicNode(pair, localIpv4)) publics.add(pair) else devices.add(pair)
        }
        return devices to publics
    }

    fun isPublicNode(pair: PeerRoutePair, localIpv4: Ipv4Inet? = null): Boolean {
        // ① 同一虚拟网段 → 同一张网络里的设备；链路在直连/中继之间切换也不改分类
        if (sharesVirtualSubnet(pair.route.ipv4Addr, localIpv4)) return false

        if (pair.route.isRelay) return true
        val remoteUrl = pair.peer?.primaryConn?.tunnel?.remoteAddr?.url.orEmpty()
        val host = hostOfUrl(remoteUrl).ifBlank { pair.route.hostname }
        if (host.isBlank()) return false
        return !isPrivateHost(host)
    }

    /**
     * 对端虚拟 IP 与本机虚拟 IP 是否属于同一网段。
     *
     * 本机网段长度缺失（0）时按 /24 处理 —— EasyTier 默认给网络分配 /24。
     */
    fun sharesVirtualSubnet(peerIp: String?, localIpv4: Ipv4Inet?): Boolean {
        if (peerIp.isNullOrBlank() || localIpv4 == null) return false
        val local = parseIpv4(localIpv4.address.toString()) ?: return false
        val peer = parseIpv4(peerIp.substringBefore('/')) ?: return false
        if (local == 0 || peer == 0) return false
        val prefix = localIpv4.networkLength.takeIf { it in 1..32 } ?: 24
        val mask = if (prefix == 32) -1 else (-1 shl (32 - prefix))
        return (local and mask) == (peer and mask)
    }

    /** `10.144.144.2` → 与 [Ipv4Addr] 相同的高位在前的 Int 编码 */
    private fun parseIpv4(raw: String): Int? {
        val parts = raw.trim().split('.')
        if (parts.size != 4) return null
        var value = 0
        parts.forEach { part ->
            val octet = part.toIntOrNull() ?: return null
            if (octet !in 0..255) return null
            value = (value shl 8) or octet
        }
        return value
    }

    /** 从 `tcp://1.2.3.4:11010`、`[::1]:11010` 这类地址里取出主机部分 */
    fun hostOfUrl(rawUrl: String): String {
        if (rawUrl.isBlank()) return ""
        var rest = rawUrl.substringAfter("://", rawUrl).substringBefore('/')
        if (rest.startsWith("[")) return rest.substringAfter('[').substringBefore(']')
        rest = rest.substringBeforeLast(':', rest)
        return rest
    }

    fun isPrivateHost(host: String): Boolean {
        val h = host.trim().lowercase()
        if (h.isEmpty() || h == "localhost") return true
        val parts = h.split('.')
        if (parts.size != 4 || parts.any { it.toIntOrNull() == null }) {
            // 不含点的短名按本机/局域网处理，带点的域名按公网处理
            return !h.contains('.')
        }
        val a = parts[0].toInt()
        val b = parts[1].toInt()
        return a == 10 ||
            (a == 192 && b == 168) ||
            (a == 172 && b in 16..31) ||
            a == 127 ||
            (a == 169 && b == 254)
    }
}
