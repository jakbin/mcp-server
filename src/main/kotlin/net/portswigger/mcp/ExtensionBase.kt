package net.portswigger.mcp

import burp.api.montoya.BurpExtension
import burp.api.montoya.MontoyaApi
import net.portswigger.mcp.config.ConfigUi
import net.portswigger.mcp.config.McpConfig
import net.portswigger.mcp.providers.ClaudeDesktopProvider
import net.portswigger.mcp.providers.ManualProxyInstallerProvider
import net.portswigger.mcp.providers.ProxyJarManager
import net.portswigger.mcp.server.KtorServerManager

class ExtensionBase : BurpExtension {

    override fun initialize(api: MontoyaApi) {
        api.extension().setName("Burp MCP Server")

        val config = McpConfig(api.persistence().extensionData())
        val serverManager = KtorServerManager(api)

        val proxyJarManager = ProxyJarManager(api.logging())

        val configUi = ConfigUi(
            config = config,
            providers = listOf(
                ClaudeDesktopProvider(api.logging(), proxyJarManager),
                ManualProxyInstallerProvider(api.logging(), proxyJarManager),
            )
        )

        configUi.onEnabledToggled { enabled ->
            val currentConfig = configUi.getConfig()

            config.enabled = enabled
            config.host = currentConfig.host
            config.port = currentConfig.port

            if (enabled) {
                serverManager.start(config) { state ->
                    configUi.updateServerState(state)
                }
            } else {
                serverManager.stop { state ->
                    configUi.updateServerState(state)
                }
            }
        }

        api.userInterface().registerSuiteTab("MCP", configUi.component)

        api.extension().registerUnloadingHandler {
            serverManager.shutdown()
        }

        if (config.enabled) {
            serverManager.start(config) { state ->
                configUi.updateServerState(state)
            }
        }
    }
}