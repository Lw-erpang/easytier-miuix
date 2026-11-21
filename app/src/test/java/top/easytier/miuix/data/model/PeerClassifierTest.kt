package top.easytier.miuix.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 状态页对端分类规则的单元测试 */
class PeerClassifierTest {

    private fun pair(
        route: Route,
        remoteUrl: String? = null,
    ): PeerRoutePair {
        val conn = if (remoteUrl == null) null else PeerConnInfo(tunnel = TunnelInfo(remoteAddr = Url(remoteUrl)))
        return PeerRoutePair(route = route, peer = PeerInfo(conns = listOfNotNull(conn)))
    }

    @Test
    fun `relayed peer counts as public node`() {
        val relay = pair(Route(peerId = 1, ipv4Addr = "10.144.144.2", cost = 2))
        assertTrue(PeerClassifier.isPublicNode(relay))
    }

    @Test
    fun `direct lan peer counts as device`() {
        val lan = pair(
            Route(peerId = 2, ipv4Addr = "10.144.144.3", cost = 1),
            "tcp://192.168.1.20:11010",
        )
        assertFalse(PeerClassifier.isPublicNode(lan))
    }

    @Test
    fun `direct peer with public ip or domain counts as public`() {
        val publicIp = pair(Route(peerId = 3, cost = 1), "tcp://1.2.3.4:11010")
        val domain = pair(Route(peerId = 4, cost = 1), "tcp://public.easytier.top:11010")
        assertTrue(PeerClassifier.isPublicNode(publicIp))
        assertTrue(PeerClassifier.isPublicNode(domain))
    }

    @Test
    fun `hostname fallback is used when tunnel info is missing`() {
        val byHostname = pair(Route(peerId = 5, cost = 1, hostname = "public.easytier.top"))
        assertTrue(PeerClassifier.isPublicNode(byHostname))
    }

    @Test
    fun `split keeps devices first and order stable`() {
        val a = pair(Route(peerId = 1, cost = 1), "tcp://10.0.0.2:11010")
        val b = pair(Route(peerId = 2, cost = 3))
        val c = pair(Route(peerId = 3, cost = 1), "tcp://172.20.0.9:11010")
        val (devices, publics) = PeerClassifier.split(listOf(a, b, c))
        assertEquals(listOf(1L, 3L), devices.map { it.route.peerId })
        assertEquals(listOf(2L), publics.map { it.route.peerId })
    }

    @Test
    fun `host parsing handles scheme port and ipv6`() {
        assertEquals("1.2.3.4", PeerClassifier.hostOfUrl("tcp://1.2.3.4:11010"))
        assertEquals("public.easytier.top", PeerClassifier.hostOfUrl("udp://public.easytier.top:11010"))
        assertEquals("::1", PeerClassifier.hostOfUrl("[::1]:11010"))
        assertEquals("", PeerClassifier.hostOfUrl(""))
    }

    @Test
    fun `private ranges are recognised`() {
        listOf("10.1.2.3", "172.16.0.1", "172.31.255.254", "192.168.0.1", "127.0.0.1", "169.254.1.1")
            .forEach { assertTrue(it, PeerClassifier.isPrivateHost(it)) }
        listOf("8.8.8.8", "172.32.0.1", "100.64.0.1", "public.easytier.top")
            .forEach { assertFalse(it, PeerClassifier.isPrivateHost(it)) }
    }

    // ---------- 同虚拟网段：保证设备分组不随链路切换跳动 ----------

    private fun local(ip: String, prefix: Int) =
        Ipv4Inet(address = ipv4(ip), networkLength = prefix)

    private fun ipv4(raw: String): Ipv4Addr {
        val parts = raw.split('.').map { it.toInt() }
        return Ipv4Addr((parts[0] shl 24) or (parts[1] shl 16) or (parts[2] shl 8) or parts[3])
    }

    @Test
    fun `relayed peer inside my subnet is still a device`() {
        // LW4600 这类节点：时直连时中继，但虚拟 IP 与我在同一网段 → 固定归入网络设备
        val relayed = pair(Route(peerId = 7, ipv4Addr = "10.144.144.6", cost = 3))
        assertFalse(PeerClassifier.isPublicNode(relayed, local("10.144.144.9", 24)))
        val direct = pair(Route(peerId = 7, ipv4Addr = "10.144.144.6", cost = 1), "tcp://1.2.3.4:11010")
        assertFalse(PeerClassifier.isPublicNode(direct, local("10.144.144.9", 24)))
    }

    @Test
    fun `relayed peer outside my subnet stays public`() {
        val relayed = pair(Route(peerId = 8, ipv4Addr = "10.144.144.6", cost = 3))
        assertTrue(PeerClassifier.isPublicNode(relayed, local("10.126.126.2", 24)))
    }

    @Test
    fun `missing network length falls back to 24`() {
        val sameC = pair(Route(peerId = 9, ipv4Addr = "10.126.126.77", cost = 2))
        assertFalse(PeerClassifier.isPublicNode(sameC, local("10.126.126.2", 0)))
        val otherC = pair(Route(peerId = 10, ipv4Addr = "10.126.127.77", cost = 2))
        assertTrue(PeerClassifier.isPublicNode(otherC, local("10.126.126.2", 0)))
    }

    @Test
    fun `subnet match respects wider prefix`() {
        val peer = pair(Route(peerId = 11, ipv4Addr = "10.144.200.8", cost = 4))
        assertFalse(PeerClassifier.isPublicNode(peer, local("10.144.144.9", 16)))
        assertTrue(PeerClassifier.isPublicNode(peer, local("10.144.144.9", 24)))
    }

    @Test
    fun `split with local ip keeps same subnet peers in devices`() {
        val same = pair(Route(peerId = 1, ipv4Addr = "10.144.144.6", cost = 5))
        val relay = pair(Route(peerId = 2, ipv4Addr = "8.8.8.8", cost = 3))
        val (devices, publics) = PeerClassifier.split(listOf(same, relay), local("10.144.144.9", 24))
        assertEquals(listOf(1L), devices.map { it.route.peerId })
        assertEquals(listOf(2L), publics.map { it.route.peerId })
    }
}
