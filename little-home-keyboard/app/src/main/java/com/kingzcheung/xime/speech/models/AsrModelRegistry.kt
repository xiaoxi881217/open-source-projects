package com.kingzcheung.xime.speech.models

import com.kingzcheung.xime.model.ModelInfo
import com.kingzcheung.xime.model.ModelFile
import com.kingzcheung.xime.model.ModelVersion
import com.kingzcheung.xime.model.ModelCategory

/**
 * 本地 ASR 模型适配注册表。
 *
 * 内置 profile 是 App 侧的权威文件映射来源：索引条目（扩展商店）只提供
 * 下载与展示信息，按 id 在此查到 profile 后取文件角色映射；索引未收录的
 * 内置 profile 仍可被选中使用（本地测试、索引上架前的过渡期）。
 */
object AsrModelRegistry {

    /** 全部内置模型适配，新模型在此追加。首项为默认模型。 */
    val profiles: List<AsrModelProfile> = listOf(
        ZipformerZhInt8Profile,
        XAsr480msZhEnPunctInt8Profile,
    ) + AdditionalStreamingAsrProfiles

    val default: AsrModelProfile = profiles.first()

    fun downloadModels(): List<ModelInfo> = profiles.map { profile ->
        ModelInfo(profile.id, profile.name, profile.description, ModelCategory.ASR,
            versions = listOf(ModelVersion(version = if (profile.engine == AsrEngine.LEGACY_ZIPFORMER) "" else "20261112",
                files = profile.downloadFiles.ifEmpty { profile.requiredFiles.map { ModelFile(it, "", bytes = profile.fileSizes[it] ?: 0L) } },
                archiveUrl = profile.downloadUrl.takeIf { profile.downloadFiles.isEmpty() },
                size = profile.size, sha256 = profile.archiveSha256)), size = profile.size)
    }

    /** 按 id 查找内置适配。 */
    fun findById(id: String): AsrModelProfile? = profiles.find { it.id == id }

    /**
     * 按 id 取适配；未知 id（如索引新增、App 未随之更新）回退默认布局，
     * 保证按通用命名发布的 zipformer2 模型仍可加载。
     */
    fun profileOrDefault(id: String): AsrModelProfile =
        findById(id) ?: default.copy(id = id)
}
