package top.easytier.miuix.ui.screens.networks

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.easytier.miuix.data.model.ConfigImporter
import top.easytier.miuix.data.model.ImportError
import top.easytier.miuix.data.model.ImportOutcome
import top.easytier.miuix.data.model.NetworkConfig
import top.easytier.miuix.data.model.NetworkInstance
import top.easytier.miuix.data.model.normalizeNetworkConfig
import top.easytier.miuix.data.repository.NetworkRepository
import top.easytier.miuix.data.repository.RealNetworkRepository
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/** 导入结果：界面据此弹提示框（成功/失败文案由字符串资源决定） */
sealed interface ImportResult {
    data class Success(val count: Int, val names: List<String>) : ImportResult
    data class Failure(val error: ImportError, val detail: String = "") : ImportResult
}

data class ImportUiState(
    val inProgress: Boolean = false,
    val result: ImportResult? = null,
)

@HiltViewModel
class NetworkListViewModel @Inject constructor(
    private val repository: NetworkRepository,
    // 选中态只存在于本地偏好里，接口层没有暴露，这里直接注入具体实现
    private val stateRepository: RealNetworkRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val instances: StateFlow<List<NetworkInstance>> = repository.getAllNetworkInstances()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _configs = MutableStateFlow<List<NetworkConfig>>(emptyList())
    val configs: StateFlow<List<NetworkConfig>> = _configs.asStateFlow()

    private val _importState = MutableStateFlow(ImportUiState())
    val importState: StateFlow<ImportUiState> = _importState.asStateFlow()

    /** 当前选中的网络；持久化后下次进入软件直接沿用 */
    val selectedInstanceId: StateFlow<String?> = stateRepository.selectedInstanceId

    /** 正在运行的实例 Id（同一时刻最多一个） */
    val runningInstanceId: StateFlow<String?> = instances
        .map { list -> list.firstOrNull { it.running }?.instanceId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            _configs.value = repository.loadConfigs()
            ensureSelection(_configs.value)
        }
    }

    fun refreshConfigs(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            _configs.value = repository.loadConfigs()
            ensureSelection(_configs.value)
            onComplete?.invoke()
        }
    }

    /** 点击网络卡片：只切换选中，不直接启动 */
    fun selectNetwork(instanceId: String) {
        stateRepository.setSelectedInstanceId(instanceId)
    }

    /** 底部按钮：未运行则启动选中项，运行中则停止 */
    fun toggleSelectedNetwork() {
        val runningId = instances.value.firstOrNull { it.running }?.instanceId
        if (runningId != null) {
            viewModelScope.launch { repository.stopNetworkInstance(runningId) }
            return
        }
        val targetId = selectedInstanceId.value ?: _configs.value.firstOrNull()?.instanceId ?: return
        val config = _configs.value.firstOrNull { it.instanceId == targetId } ?: return
        viewModelScope.launch { repository.runNetworkInstance(config) }
    }

    fun deleteNetwork(instanceId: String) {
        _configs.value = _configs.value.filter { it.instanceId != instanceId }
        viewModelScope.launch {
            repository.saveConfigs(_configs.value)
            repository.deleteNetworkInstance(instanceId)
            if (selectedInstanceId.value == instanceId) {
                stateRepository.setSelectedInstanceId(_configs.value.firstOrNull()?.instanceId)
            }
        }
    }

    fun runNetwork(config: NetworkConfig) {
        viewModelScope.launch { repository.runNetworkInstance(config) }
    }

    fun stopNetwork(instanceId: String) {
        viewModelScope.launch { repository.stopNetworkInstance(instanceId) }
    }

    fun getConfigById(instanceId: String): NetworkConfig? {
        return _configs.value.find { it.instanceId == instanceId }
    }

    /**
     * 保证选中项有效：没有选中 / 选中项已被删除时，
     * 依次回落到「上次运行过的网络」→「第一个网络」。
     */
    private fun ensureSelection(configs: List<NetworkConfig>) {
        if (configs.isEmpty()) {
            stateRepository.setSelectedInstanceId(null)
            return
        }
        val current = selectedInstanceId.value
        if (current != null && configs.any { it.instanceId == current }) return
        val fallback = stateRepository.getLastRunConfig()?.instanceId
            ?.takeIf { id -> configs.any { it.instanceId == id } }
            ?: configs.first().instanceId
        stateRepository.setSelectedInstanceId(fallback)
    }

    // ---------- 配置导入 ----------

    /** 读取用户选中的文件并导入；结果写入 [importState] 由界面展示后消费。 */
    fun importConfigs(uri: Uri) {
        if (_importState.value.inProgress) return
        viewModelScope.launch {
            _importState.value = ImportUiState(inProgress = true)
            val read = withContext(Dispatchers.IO) { readText(uri) }
            if (read.error != null) {
                _importState.value = ImportUiState(result = ImportResult.Failure(read.error))
                return@launch
            }
            val outcome = withContext(Dispatchers.Default) { ConfigImporter.parse(read.text) }
            when (outcome) {
                is ImportOutcome.Failure ->
                    _importState.value = ImportUiState(
                        result = ImportResult.Failure(outcome.error, outcome.detail),
                    )
                is ImportOutcome.Success -> {
                    val saved = repository.loadConfigs()
                    val prepared = buildList {
                        val usedIds = saved.map { it.instanceId }.toMutableSet()
                        val usedNames = saved
                            .map { it.instanceName.ifEmpty { it.networkName } }
                            .toMutableSet()
                        outcome.configs.forEach { raw ->
                            var config = normalizeNetworkConfig(raw)
                            // 解析层已重新分配 ID，这里再兜一层，确保不会覆盖已保存配置
                            if (config.instanceId in usedIds) {
                                config = config.copy(instanceId = java.util.UUID.randomUUID().toString())
                            }
                            val name = uniqueName(
                                config.instanceName.ifEmpty { config.networkName },
                                usedNames,
                            )
                            usedIds.add(config.instanceId)
                            usedNames.add(name)
                            add(config.copy(instanceName = name))
                        }
                    }
                    withContext(Dispatchers.IO) {
                        repository.saveConfigs(saved + prepared)
                    }
                    _configs.value = repository.loadConfigs()
                    ensureSelection(_configs.value)
                    _importState.value = ImportUiState(
                        result = ImportResult.Success(
                            count = prepared.size,
                            names = prepared.map { it.instanceName },
                        ),
                    )
                }
            }
        }
    }

    /** 关闭导入结果提示框 */
    fun consumeImportResult() {
        _importState.value = ImportUiState()
    }

    private data class ReadResult(val text: String = "", val error: ImportError? = null)

    private fun readText(uri: Uri): ReadResult = try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArrayOutputStream()
            val chunk = ByteArray(8 * 1024)
            while (true) {
                val n = input.read(chunk)
                if (n <= 0) break
                buffer.write(chunk, 0, n)
                if (buffer.size() > ConfigImporter.MAX_FILE_BYTES) {
                    return ReadResult(error = ImportError.TooLarge)
                }
            }
            ReadResult(text = buffer.toString(Charsets.UTF_8.name()))
        } ?: ReadResult(error = ImportError.ReadFailed)
    } catch (e: Exception) {
        ReadResult(error = ImportError.ReadFailed)
    }

    /** 重名时追加序号，避免列表里出现两个完全一样的名字 */
    private fun uniqueName(base: String, used: Set<String>): String {
        if (base !in used) return base
        var index = 2
        while ("$base ($index)" in used) index++
        return "$base ($index)"
    }
}
