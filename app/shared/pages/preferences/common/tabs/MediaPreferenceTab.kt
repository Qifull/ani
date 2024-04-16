package me.him188.ani.app.ui.preference.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.him188.ani.app.data.media.MediaSourceManager
import me.him188.ani.app.data.models.MediaCacheSettings
import me.him188.ani.app.data.repositories.PreferencesRepository
import me.him188.ani.app.navigation.AniNavigator
import me.him188.ani.app.navigation.LocalNavigator
import me.him188.ani.app.tools.MonoTasker
import me.him188.ani.app.ui.foundation.AbstractViewModel
import me.him188.ani.app.ui.foundation.rememberViewModel
import me.him188.ani.app.ui.preference.PreferenceScope
import me.him188.ani.app.ui.preference.PreferenceTab
import me.him188.ani.app.ui.preference.SelectableItem
import me.him188.ani.app.ui.preference.SwitchItem
import me.him188.ani.app.ui.subject.episode.details.renderSubtitleLanguage
import me.him188.ani.app.ui.subject.episode.mediaFetch.MediaPreference
import me.him188.ani.app.ui.subject.episode.mediaFetch.renderMediaSource
import me.him188.ani.datasources.api.topic.FileSize.Companion.megaBytes
import me.him188.ani.datasources.api.topic.SubtitleLanguage
import moe.tlaster.precompose.flow.collectAsStateWithLifecycle
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.roundToInt

@Stable
class MediaPreferenceViewModel : AbstractViewModel(), KoinComponent {
    private val preferencesRepository: PreferencesRepository by inject()
    private val mediaSourceManager: MediaSourceManager by inject()

    val defaultMediaPreference by preferencesRepository.defaultMediaPreference.flow
        .map {
            it ?: MediaPreference.Empty
        }.produceState(MediaPreference.Empty)


    val allMediaSources by mediaSourceManager.sources.map { list ->
        list.map { it.mediaSourceId }
    }.produceState(emptyList())

    val allSubtitleLanguageIds = SubtitleLanguage.matchableEntries.map { it.id }

    val sortedLanguages by derivedStateOf {
        defaultMediaPreference.fallbackSubtitleLanguageIds.extendTo(allSubtitleLanguageIds)
    }

    val sortedMediaSources by derivedStateOf {
        defaultMediaPreference.fallbackMediaSourceIds.extendTo(allMediaSources)
    }

    /**
     * 将 [this] 扩展到 [all]，并保持顺序.
     */
    private fun List<String>?.extendTo(
        all: List<String>
    ): List<SelectableItem<String>> {
        val fallback = this ?: return all.map { SelectableItem(it, selected = true) }

        return fallback.map {
            SelectableItem(it, selected = true)
        } + (all - fallback.toSet()).map {
            SelectableItem(it, selected = false)
        }
    }


    private val defaultMediaPreferenceTasker = MonoTasker(this.backgroundScope)
    fun updateDefaultMediaPreference(copy: MediaPreference) {
        defaultMediaPreferenceTasker.launch {
            preferencesRepository.defaultMediaPreference.set(copy)
        }
    }


    val mediaCacheSettings: Flow<MediaCacheSettings> =
        preferencesRepository.mediaCacheSettings.flow.shareInBackground()

    private val mediaCacheSettingsTasker = MonoTasker(this.backgroundScope)
    fun updateMediaCacheSettings(copy: MediaCacheSettings) {
        mediaCacheSettingsTasker.launch {
            preferencesRepository.mediaCacheSettings.set(copy)
        }
    }
}

@Composable
fun MediaPreferenceTab(
    vm: MediaPreferenceViewModel = rememberViewModel { MediaPreferenceViewModel() },
    modifier: Modifier = Modifier,
) {
    val navigator by rememberUpdatedState(LocalNavigator.current)
    PreferenceTab(modifier) {
        AutoCacheGroup(vm, navigator)

        MediaDownloadGroup(vm)
    }
}

@Composable
private fun PreferenceScope.AutoCacheGroup(
    vm: MediaPreferenceViewModel,
    navigator: AniNavigator
) {
    Group(
        title = { Text("自动缓存") },
        description = { Text("自动缓存 \"在看\" 分类中未观看的剧集") },
    ) {
        val mediaCacheSettings by vm.mediaCacheSettings.collectAsStateWithLifecycle(MediaCacheSettings.Default)
        SwitchItem(
            title = { Text("启用自动缓存") },
            description = { Text("启用后下面的设置才有效") },
        ) {
            Switch(
                checked = mediaCacheSettings.enabled,
                onCheckedChange = {
                    vm.updateMediaCacheSettings(mediaCacheSettings.copy(enabled = it))
                }
            )
        }

        HorizontalDividerItem()

        var maxCount by remember(mediaCacheSettings) { mutableFloatStateOf(mediaCacheSettings.maxCountPerSubject.toFloat()) }
        SliderItem(
            title = { Text("最大自动缓存话数") },
            description = {
                Column {
                    Text("若手动缓存数量超过该设置值，将不会自动缓存")
                    Row {
                        Text(remember(maxCount) { autoCacheDescription(maxCount) })
                        if (maxCount == 10f) {
                            Text("可能会占用大量空间", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
        ) {
            Slider(
                value = maxCount,
                onValueChange = { maxCount = it },
                valueRange = 0f..10f,
                steps = 9,
            )
        }

        HorizontalDividerItem()

        var mostRecentOnly by remember(mediaCacheSettings) {
            mutableStateOf(mediaCacheSettings.mostRecentOnly)
        } // for preview
        SwitchItem(
            checked = mostRecentOnly,
            onCheckedChange = {
                mostRecentOnly = it
                vm.updateMediaCacheSettings(mediaCacheSettings.copy(mostRecentOnly = it))
            },
            title = { Text("仅缓存最近看过的番剧") },
//            description = {
//                if (!mostRecentOnly) {
//                    Text("当前设置: 总是缓存 \"在看\" 分类中的全部番剧")
//                }
//            },
        )

        AnimatedVisibility(mostRecentOnly) {
            SubGroup {
                var mostRecentCount by remember(mediaCacheSettings) { mutableFloatStateOf(mediaCacheSettings.mostRecentCount.toFloat()) }
                SliderItem(
                    title = { Text("缓存数量") },
                    description = {
                        Text("当前设置: 仅缓存最近看过的 ${mostRecentCount.roundToInt()} 部番剧")
                    },
                ) {
                    Slider(
                        value = mostRecentCount,
                        onValueChange = { mostRecentCount = it },
                        onValueChangeFinished = {
                            vm.updateMediaCacheSettings(mediaCacheSettings.copy(mostRecentCount = mostRecentCount.roundToInt()))
                        },
                        valueRange = 0f..30f,
                        steps = 30 - 1,
                    )
                }
            }
        }

        HorizontalDividerItem()

        TextItem(
            title = { Text("管理已缓存的剧集") },
            icon = { Icon(Icons.Rounded.ArrowOutward, null) },
            onClick = { navigator.navigateCaches() },
        )
    }
}

@Composable
private fun PreferenceScope.MediaDownloadGroup(vm: MediaPreferenceViewModel) {
    Group(
        title = {
            Text("资源选择偏好")
        },
        description = {
            Column {
                Text("设置默认的资源选择偏好。将同时影响在线播放和缓存")
                Text("每个番剧在播放时的选择将覆盖这里的设置")
            }
        },
    ) {
        val textAny = "任意"
        val textNone = "无"

        SorterItem(
            values = { vm.sortedMediaSources },
            onSort = { list ->
                vm.updateDefaultMediaPreference(
                    vm.defaultMediaPreference.copy(fallbackMediaSourceIds = list.filter { it.selected }.map { it.item })
                )
            },
            item = { Text(renderMediaSource(it)) },
            key = { it },
            exposed = { list ->
                Text(
                    remember(list) {
                        if (list.fastAll { it.selected }) {
                            textAny
                        } else if (list.fastAll { !it.selected }) {
                            textNone
                        } else
                            list.asSequence().filter { it.selected }
                                .joinToString { renderMediaSource(it.item) }
                    },
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            },
            title = { Text("数据源") },
            description = { Text("优先选择较为靠前的数据源") },
            icon = { Icon(Icons.Rounded.DisplaySettings, null) },
        )

        HorizontalDividerItem()

        SorterItem(
            values = { vm.sortedLanguages },
            onSort = { list ->
                vm.updateDefaultMediaPreference(
                    vm.defaultMediaPreference.copy(fallbackSubtitleLanguageIds = list.filter { it.selected }
                        .map { it.item })
                )
            },
            exposed = { list ->
                Text(
                    remember(list) {
                        if (list.fastAll { it.selected }) {
                            textAny
                        } else if (list.fastAll { !it.selected }) {
                            textNone
                        } else
                            list.asSequence().filter { it.selected }
                                .joinToString { renderSubtitleLanguage(it.item) }
                    },
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            },
            item = { Text(renderSubtitleLanguage(it)) },
            description = { Text("优先选择较为靠前的字幕语言") },
            key = { it },
            icon = { Icon(Icons.Rounded.Language, null) },
            title = { Text("字幕语言") },
        )

        HorizontalDividerItem()

        TextItem(
//                selected = { mediaPreference.resolution },
//                values = { vm.resolutions },
//                itemText = {
//                    if (it == null) {
//                        Text("无偏好")
//                    } else {
//                        Text(it)
//                    }
//                },
//                onSelect = {
//                    vm.updateDefaultMediaPreference(mediaPreference.copy(resolution = it))
//                },
            action = {
                Text(
                    "尽可能高",
                    Modifier.padding(end = 12.dp),
                )
            },
            title = { Text("分辨率") },
            description = { Text("暂不支持修改") },
            icon = { Icon(Icons.Rounded.Hd, null) },
        )

        HorizontalDividerItem()

        var allianceRegexes by remember(vm.defaultMediaPreference) {
            mutableStateOf(vm.defaultMediaPreference.alliancePatterns?.joinToString() ?: "")
        }
        TextFieldItem(
            value = allianceRegexes,
            title = { Text("字幕组") },
            placeholder = { Text(textAny) },
            description = {
                Text("支持使用正则表达式，使用逗号分隔。越靠前的表达式的优先级越高\n\n示例: 桜都, 喵萌, 北宇治\n将优先采用桜都字幕组资源，否则采用喵萌，以此类推")
            },
            icon = { Icon(Icons.Rounded.Subtitles, null) },
            onValueChange = { allianceRegexes = it.replace("，", ",") },
            onValueChangeCompleted = {
                vm.updateDefaultMediaPreference(
                    vm.defaultMediaPreference.copy(
                        alliancePatterns = allianceRegexes.split(",", "，").map { it.trim() })
                )
            },
        )
    }
}

fun autoCacheDescription(sliderValue: Float) = when (sliderValue) {
    0f -> "当前设置: 不自动缓存"
    10f -> "当前设置: 自动缓存全部未观看剧集, "
    else -> "当前设置: 自动缓存观看进度之后的 ${sliderValue.toInt()} 话, " +
            "预计占用空间 ${600.megaBytes * sliderValue}/番剧"
}