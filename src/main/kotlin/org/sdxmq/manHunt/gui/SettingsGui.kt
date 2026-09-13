package org.sdxmq.manHunt.gui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.sdxmq.manHunt.manager.GameManager
import org.sdxmq.manHunt.manager.WinMode

object SettingsGui : Listener {

    private val title = Component.text("[ 맨헌트 설정 ]").color(NamedTextColor.DARK_GRAY).decorate(TextDecoration.BOLD)

    fun open(player: Player) {
        val inv = Bukkit.createInventory(null, 27, title)
        updateInventory(inv)
        player.openInventory(inv)
    }

    private fun updateInventory(inv: Inventory) {
        // 슬롯 10: 심장박동 반경 (레드스톤)
        val radiusItem = ItemStack(Material.REDSTONE)
        radiusItem.editMeta { meta ->
            meta.displayName(Component.text("심장박동 감지 반경").color(NamedTextColor.RED).decorate(TextDecoration.BOLD))
            meta.lore(listOf(
                Component.text("현재 감지 반경: ${GameManager.config.heartbeatRadius}블록").color(NamedTextColor.GRAY),
                Component.empty(),
                Component.text("[좌클릭] -5블록 | [Shift+좌클릭] -20블록").color(NamedTextColor.GREEN),
                Component.text("[우클릭] +5블록 | [Shift+우클릭] +20블록").color(NamedTextColor.RED),
                Component.text("(최소 5블록)").color(NamedTextColor.DARK_GRAY)
            ))
        }
        inv.setItem(10, radiusItem)

        // 슬롯 12: 헌터 출발 대기 시간 (시계)
        val delayItem = ItemStack(Material.CLOCK)
        delayItem.editMeta { meta ->
            meta.displayName(Component.text("헌터 출발 대기 시간").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
            meta.lore(listOf(
                Component.text("현재 출발 대기: ${GameManager.config.hunterDelay}초").color(NamedTextColor.GRAY),
                Component.empty(),
                Component.text("[좌클릭] -5초 | [Shift+좌클릭] -30초").color(NamedTextColor.GREEN),
                Component.text("[우클릭] +5초 | [Shift+우클릭] +30초").color(NamedTextColor.RED),
                Component.text("(최소 5초)").color(NamedTextColor.DARK_GRAY)
            ))
        }
        inv.setItem(12, delayItem)

        // 슬롯 14: 헌터 부활 대기 시간 (불사의 토템)
        val respawnItem = ItemStack(Material.TOTEM_OF_UNDYING)
        respawnItem.editMeta { meta ->
            meta.displayName(Component.text("헌터 부활 대기 시간").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
            meta.lore(listOf(
                Component.text("현재 부활 대기: ${GameManager.config.hunterRespawnDelay}초").color(NamedTextColor.GRAY),
                Component.empty(),
                Component.text("[좌클릭] -5초 | [Shift+좌클릭] -30초").color(NamedTextColor.GREEN),
                Component.text("[우클릭] +5초 | [Shift+우클릭] +30초").color(NamedTextColor.RED),
                Component.text("(최소 5초)").color(NamedTextColor.DARK_GRAY)
            ))
        }
        inv.setItem(14, respawnItem)

        // 슬롯 16: 러너 패배 판정 모드 (다이아몬드 검)
        val modeStr = if (GameManager.config.runnerWinMode == WinMode.ALL) {
            Component.text("[모든 러너 사망 시 헌터 승리]").color(NamedTextColor.GREEN)
        } else {
            Component.text("[러너 1명 사망 시 헌터 승리]").color(NamedTextColor.RED)
        }
        val modeItem = ItemStack(Material.DIAMOND_SWORD)
        modeItem.editMeta { meta ->
            meta.displayName(Component.text("러너 패배 판정 모드").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD))
            meta.lore(listOf(
                Component.text("현재 모드: ").color(NamedTextColor.GRAY).append(modeStr),
                Component.empty(),
                Component.text("[클릭] 모드 전환").color(NamedTextColor.YELLOW)
            ))
        }
        inv.setItem(16, modeItem)
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val view = event.view
        if (view.title() == title) {
            event.isCancelled = true
            val inv = event.clickedInventory ?: return
            if (inv != view.topInventory) return

            val config = GameManager.config

            when (event.slot) {
                10 -> { // 반경
                    when (event.click) {
                        ClickType.LEFT -> config.heartbeatRadius -= 5
                        ClickType.SHIFT_LEFT -> config.heartbeatRadius -= 20
                        ClickType.RIGHT -> config.heartbeatRadius += 5
                        ClickType.SHIFT_RIGHT -> config.heartbeatRadius += 20
                        else -> {}
                    }
                    if (config.heartbeatRadius < 5) config.heartbeatRadius = 5
                    updateInventory(inv)
                }
                12 -> { // 출발 대기
                    when (event.click) {
                        ClickType.LEFT -> config.hunterDelay -= 5
                        ClickType.SHIFT_LEFT -> config.hunterDelay -= 30
                        ClickType.RIGHT -> config.hunterDelay += 5
                        ClickType.SHIFT_RIGHT -> config.hunterDelay += 30
                        else -> {}
                    }
                    if (config.hunterDelay < 5) config.hunterDelay = 5
                    updateInventory(inv)
                }
                14 -> { // 부활 대기
                    when (event.click) {
                        ClickType.LEFT -> config.hunterRespawnDelay -= 5
                        ClickType.SHIFT_LEFT -> config.hunterRespawnDelay -= 30
                        ClickType.RIGHT -> config.hunterRespawnDelay += 5
                        ClickType.SHIFT_RIGHT -> config.hunterRespawnDelay += 30
                        else -> {}
                    }
                    if (config.hunterRespawnDelay < 5) config.hunterRespawnDelay = 5
                    updateInventory(inv)
                }
                16 -> { // 판정 모드
                    config.runnerWinMode = if (config.runnerWinMode == WinMode.ALL) WinMode.ONE else WinMode.ALL
                    updateInventory(inv)
                }
            }
        }
    }
}
