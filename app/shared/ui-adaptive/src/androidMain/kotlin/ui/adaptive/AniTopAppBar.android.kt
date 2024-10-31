/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.adaptive

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import me.him188.ani.app.domain.session.TestUserInfo
import me.him188.ani.app.domain.session.createTestAuthState
import me.him188.ani.app.ui.foundation.ProvideFoundationCompositionLocalsForPreview
import me.him188.ani.app.ui.foundation.layout.Zero
import me.him188.ani.app.ui.foundation.session.SelfAvatar
import me.him188.ani.app.ui.foundation.widgets.TopAppBarGoBackButton
import me.him188.ani.utils.platform.annotations.TestOnly


@OptIn(TestOnly::class)
@Composable
@PreviewScreenSizes
private fun PreviewAniTopAppBar() = ProvideFoundationCompositionLocalsForPreview {
    val scope = rememberCoroutineScope()
    AniTopAppBar(
        title = { Text("MyTitle") },
        windowInsets = WindowInsets.Zero,
        navigationIcon = { TopAppBarGoBackButton() },
        actions = {
            IconButton({}) {
                Icon(Icons.Rounded.Settings, null)
            }
        },
        avatar = {
            SelfAvatar(createTestAuthState(scope), TestUserInfo)
        },
        searchIconButton = {
            IconButton({}) {
                Icon(Icons.Rounded.Search, null)
            }
        },
        searchBar = {
            IconButton({}) {
                Icon(Icons.Rounded.Search, null)
            }
        },
    )
}