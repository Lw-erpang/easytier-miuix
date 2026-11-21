package top.easytier.miuix.ui.screens.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.easytier.miuix.data.model.NetworkInstance
import top.easytier.miuix.data.model.PeerRoutePair
import top.easytier.miuix.data.repository.NetworkRepository
import javax.inject.Inject

/** 主机名重命名失败的两种原因；文案由界面按资源映射 */
enum class RenameError { Empty, Missing }

/** restarted = true 表示已通过重启实例让新主机名生效 */
data class RenameResult(val restarted: Boolean = false, val error: RenameError? = null)

@HiltViewModel
class StatusViewModel @Inject constructor(
    private val repository: NetworkRepository,
) : ViewModel() {

    private val _currentInstance = MutableStateFlow<NetworkInstance?>(null)
    val currentInstance: StateFlow<NetworkInstance?> = _currentInstance.asStateFlow()

    private val _txRate = MutableStateFlow("0 B/s")
    val txRate: StateFlow<String> = _txRate.asStateFlow()

    private val _rxRate = MutableStateFlow("0 B/s")
    val rxRate: StateFlow<String> = _rxRate.asStateFlow()

    private val _txHistory = MutableStateFlow<List<Long>>(emptyList())
    val txHistory: StateFlow<List<Long>> = _txHistory.asStateFlow()

    private val _rxHistory = MutableStateFlow<List<Long>>(emptyList())
    val rxHistory: StateFlow<List<Long>> = _rxHistory.asStateFlow()

    private var prevTxSum = 0L
    private var prevRxSum = 0L
    private var hasBaseline = false
    private var lastSampleAt = 0L
    private var lastRunningId: String? = null

    init {
        viewModelScope.launch {
            repository.getAllNetworkInstances().collect { instances ->
                val running = instances.firstOrNull { it.running }
                _currentInstance.value = running

                // Reset counters and history when instance changes or stops
                val runningId = running?.instanceId
                if (runningId != lastRunningId) {
                    prevTxSum = 0L
                    prevRxSum = 0L
                    hasBaseline = false
                    lastSampleAt = 0L
                    _txHistory.value = emptyList()
                    _rxHistory.value = emptyList()
                    lastRunningId = runningId
                }

                val detail = running?.detail ?: run {
                    _txRate.value = "0 B/s"
                    _rxRate.value = "0 B/s"
                    hasBaseline = false
                    lastSampleAt = 0L
                    return@collect
                }
                val curTxSum = detail.peers.flatMap { it.conns }.sumOf { it.stats?.txBytes ?: 0 }
                val curRxSum = detail.peers.flatMap { it.conns }.sumOf { it.stats?.rxBytes ?: 0 }
                val now = android.os.SystemClock.elapsedRealtime()

                when {
                    // 首个样本或连接重建导致累计值回退：只记基准，不产生脏样本
                    !hasBaseline || curTxSum < prevTxSum || curRxSum < prevRxSum -> {
                        hasBaseline = true
                        prevTxSum = curTxSum
                        prevRxSum = curRxSum
                        lastSampleAt = now
                    }
                    // 轮询间无新数据：保持上次速率，不往图表塞零点
                    curTxSum == prevTxSum && curRxSum == prevRxSum -> Unit
                    else -> {
                        val elapsedMs = now - lastSampleAt
                        if (elapsedMs >= 500) {
                            val tx = (curTxSum - prevTxSum) * 1000 / elapsedMs
                            val rx = (curRxSum - prevRxSum) * 1000 / elapsedMs
                            _txRate.value = humanFileSize(tx) + "/s"
                            _rxRate.value = humanFileSize(rx) + "/s"
                            _txHistory.value = (_txHistory.value + tx).takeLast(60)
                            _rxHistory.value = (_rxHistory.value + rx).takeLast(60)
                            prevTxSum = curTxSum
                            prevRxSum = curRxSum
                            lastSampleAt = now
                        }
                    }
                }
            }
        }
    }

    fun getPeerRoutePairs(): List<PeerRoutePair> {
        return _currentInstance.value?.detail?.peerRoutePairs ?: emptyList()
    }

    // ---------- 本机主机名重命名 ----------

    private val _renameResult = MutableStateFlow<RenameResult?>(null)
    val renameResult: StateFlow<RenameResult?> = _renameResult.asStateFlow()

    /**
     * 修改本机主机名。
     *
     * 主机名要传给核心才能生效，所以先写回配置；网络正在运行时需要重启实例，
     * 新名字才会出现在其他节点看到的对端列表里。
     */
    fun renameHostname(rawName: String) {
        val name = rawName.trim()
            .replace("\"", "")
            .replace("\\", "")
            .take(63)
        if (name.isBlank()) {
            _renameResult.value = RenameResult(error = RenameError.Empty)
            return
        }
        val instanceId = _currentInstance.value?.instanceId
        if (instanceId == null) {
            _renameResult.value = RenameResult(error = RenameError.Missing)
            return
        }
        viewModelScope.launch {
            val configs = repository.loadConfigs().toMutableList()
            val index = configs.indexOfFirst { it.instanceId == instanceId }
            if (index < 0) {
                _renameResult.value = RenameResult(error = RenameError.Missing)
                return@launch
            }
            val updated = configs[index].copy(hostname = name)
            configs[index] = updated
            repository.saveConfigs(configs)

            val wasRunning = _currentInstance.value?.running == true
            if (wasRunning) {
                repository.stopNetworkInstance(instanceId)
                repository.runNetworkInstance(updated)
            }
            _renameResult.value = RenameResult(restarted = wasRunning)
        }
    }

    fun consumeRenameResult() {
        _renameResult.value = null
    }

    private fun humanFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        val gb = mb / 1024.0
        return "%.1f GB".format(gb)
    }
}
