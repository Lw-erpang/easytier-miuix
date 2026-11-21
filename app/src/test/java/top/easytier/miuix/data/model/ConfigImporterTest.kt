package top.easytier.miuix.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 配置导入解析器的单元测试：覆盖 JSON、TOML 与各类失败分支。 */
class ConfigImporterTest {

    // ---------- JSON ----------

    @Test
    fun `json single object keeps core fields`() {
        val json = """
            {
              "instanceName": "办公室",
              "networkName": "office",
              "networkSecret": "s3cret",
              "dhcp": false,
              "virtualIpv4": "10.1.2.3/24",
              "peerUrls": ["tcp://1.2.3.4:11010"],
              "proxyCidrs": ["192.168.0.0/24"],
              "enableSocks5": true,
              "socks5Port": 1081
            }
        """.trimIndent()

        val outcome = ConfigImporter.parse(json)
        assertTrue(outcome is ImportOutcome.Success)
        val config = (outcome as ImportOutcome.Success).configs.single()
        assertEquals(ImportFormat.Json, outcome.format)
        assertEquals("办公室", config.instanceName)
        assertEquals("office", config.networkName)
        assertEquals("s3cret", config.networkSecret)
        assertEquals("10.1.2.3/24", config.virtualIpv4)
        assertEquals(listOf("tcp://1.2.3.4:11010"), config.peerUrls)
        assertEquals(listOf("192.168.0.0/24"), config.proxyCidrs)
        assertTrue(config.enableSocks5)
        assertEquals(1081, config.socks5Port)
    }

    @Test
    fun `json array imports every config and regenerates ids`() {
        val json = """
            [
              {"instanceId": "fixed-id", "networkName": "a"},
              {"instanceId": "fixed-id", "networkName": "b"}
            ]
        """.trimIndent()

        val outcome = ConfigImporter.parse(json) as ImportOutcome.Success
        assertEquals(2, outcome.configs.size)
        assertEquals(listOf("a", "b"), outcome.configs.map { it.networkName })
        assertEquals(2, outcome.configs.map { it.instanceId }.toSet().size)
        assertTrue(outcome.configs.none { it.instanceId == "fixed-id" })
    }

    // ---------- TOML ----------

    @Test
    fun `toml maps identity peers flags and listeners`() {
        val toml = """
            instance_name = "家里"
            hostname = "phone"
            dhcp = false
            ipv4 = "10.144.144.7/24"
            listeners = [
                "tcp://0.0.0.0:11010",   # 注释
                "udp://0.0.0.0:11010",
            ]
            routes = ["10.0.0.0/8"]

            [network_identity]
            network_name = "home"
            network_secret = "pw"

            [[peer]]
            uri = "tcp://public.easytier.top:11010"

            [[peer]]
            uri = "udp://peer.example.com:11010"

            [flags]
            no_tun = true
            enable_exit_node = true
            enable_ipv6 = false
            multi_thread = false
            mtu = 1300
            accept_dns = true
            encryption_algorithm = "xor"

            [[proxy_network]]
            cidr = "192.168.1.0/24"

            [[port_forward]]
            bind_addr = "10.144.144.7:8080"
            dst_addr = "192.168.1.10:80"
            proto = "tcp"

            [vpn_portal_config]
            wireguard_listen = "0.0.0.0:22022"

            [[vpn_portal_config.clients]]
            name = "laptop"
            virtual_ip = "10.14.14.2/24"
        """.trimIndent()

        val outcome = ConfigImporter.parse(toml) as ImportOutcome.Success
        val config = outcome.configs.single()
        assertEquals(ImportFormat.Toml, outcome.format)
        assertEquals("家里", config.instanceName)
        assertEquals("home", config.networkName)
        assertEquals("pw", config.networkSecret)
        assertEquals("phone", config.hostname)
        assertTrue(!config.dhcp)
        assertEquals("10.144.144.7/24", config.virtualIpv4)
        assertEquals(24, config.networkLength)
        assertEquals(listOf("tcp://0.0.0.0:11010", "udp://0.0.0.0:11010"), config.listenerUrls)
        assertEquals(
            listOf("tcp://public.easytier.top:11010", "udp://peer.example.com:11010"),
            config.peerUrls,
        )
        assertEquals(listOf("192.168.1.0/24"), config.proxyCidrs)
        assertEquals(listOf("10.0.0.0/8"), config.routes)
        assertTrue(config.enableManualRoutes)
        assertTrue(config.enableExitNode)
        assertTrue(config.disableIpv6)
        assertTrue(!config.multiThread)
        assertEquals(1300, config.mtu)
        assertTrue(config.enableMagicDns)
        assertEquals(EncryptionAlgorithm.Xor, config.encryptionAlgorithm)
        assertTrue(config.enableVpnPortal)
        assertEquals(22022, config.vpnPortalListenPort)
        assertEquals("laptop", config.vpnPortalClients.single().name)
        assertEquals("10.14.14.2/24", config.vpnPortalClients.single().virtualIp)
        val forward = config.portForwards.single()
        assertEquals("10.144.144.7", forward.bindIp)
        assertEquals(8080, forward.bindPort)
        assertEquals("192.168.1.10", forward.dstIp)
        assertEquals(80, forward.dstPort)
    }

    @Test
    fun `toml with only identity still imports`() {
        val toml = """
            [network_identity]
            network_name = "mini"
        """.trimIndent()

        val outcome = ConfigImporter.parse(toml)
        require(outcome is ImportOutcome.Success) { "unexpected outcome: $outcome" }
        val config = outcome.configs.single()
        assertEquals("mini", config.networkName)
        assertTrue(config.dhcp)
    }

    // ---------- 失败分支 ----------

    @Test
    fun `empty text fails with empty error`() {
        val outcome = ConfigImporter.parse("   \n  ")
        assertEquals(ImportError.Empty, (outcome as ImportOutcome.Failure).error)
    }

    @Test
    fun `unknown format fails with unsupported error`() {
        val outcome = ConfigImporter.parse("hello world\nthis is not a config")
        assertEquals(ImportError.Unsupported, (outcome as ImportOutcome.Failure).error)
    }

    @Test
    fun `broken json fails with json syntax error`() {
        val outcome = ConfigImporter.parse("{\"networkName\":")
        assertEquals(ImportError.JsonSyntax, (outcome as ImportOutcome.Failure).error)
    }

    @Test
    fun `unrelated json is rejected instead of importing a default config`() {
        val json = """
            {
              "type": "shuying-backup",
              "version": 1,
              "appVersion": "2.2.5",
              "algorithms": [{"id": "a", "name": "b"}]
            }
        """.trimIndent()

        val outcome = ConfigImporter.parse(json)
        assertEquals(ImportError.NoConfig, (outcome as ImportOutcome.Failure).error)
    }

    @Test
    fun `quotes in imported names are stripped so toml stays valid`() {
        val json = """{"networkName": "a\"b", "instanceName": "x\"y"}"""
        val config = (ConfigImporter.parse(json) as ImportOutcome.Success).configs.single()
        assertEquals("ab", config.networkName)
        assertEquals("xy", config.instanceName)
    }
}
