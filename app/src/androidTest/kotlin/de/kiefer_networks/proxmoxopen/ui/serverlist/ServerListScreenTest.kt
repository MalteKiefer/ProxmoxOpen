package de.kiefer_networks.proxmoxopen.ui.serverlist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.kiefer_networks.proxmoxopen.core.ui.theme.ProxMoxOpenTheme
import de.kiefer_networks.proxmoxopen.domain.model.Realm
import de.kiefer_networks.proxmoxopen.domain.model.Server
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for [ServerListScreen] rendering. These exercise the composable
 * directly, bypassing Hilt and the ViewModel, so they can run without a
 * container setup on any emulator that already has the APK installed.
 */
@RunWith(AndroidJUnit4::class)
class ServerListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun empty_state_shows_add_server_cta() {
        composeTestRule.setContent {
            ProxMoxOpenTheme {
                StatelessServerList(
                    servers = emptyList(),
                    onAdd = {},
                    onOpen = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Servers").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("No servers yet. Tap + to add your first Proxmox server.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Add server").assertIsDisplayed()
    }

    @Test
    fun populated_list_shows_server_names_and_opens_on_click() {
        val opened = MutableStateFlow<Long?>(null)
        composeTestRule.setContent {
            ProxMoxOpenTheme {
                StatelessServerList(
                    servers = listOf(
                        sampleServer(1, "alpha"),
                        sampleServer(2, "beta"),
                    ),
                    onAdd = {},
                    onOpen = { opened.value = it.id },
                )
            }
        }
        composeTestRule.onNodeWithText("alpha").assertIsDisplayed()
        composeTestRule.onNodeWithText("beta").assertIsDisplayed()
        composeTestRule.onNodeWithText("alpha").performClick()
        assert(opened.value == 1L)
    }

    private fun sampleServer(id: Long, name: String) = Server(
        id = id,
        name = name,
        host = "$name.lan",
        port = 8006,
        realm = Realm.PVE_TOKEN,
        username = "root",
        tokenId = "mobile",
        fingerprintSha256 = "aa".repeat(32),
        createdAt = 0,
        lastConnectedAt = null,
    )
}
