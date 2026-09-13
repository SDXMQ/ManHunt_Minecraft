package org.sdxmq.manHunt.task

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType
import org.sdxmq.manHunt.manager.GameManager
import java.time.Duration
import org.bukkit.inventory.ItemStack

class GameTickTask : Runnable {
    override fun run() {
        if (!GameManager.isRunning) return

        val now = System.currentTimeMillis()

        // 1. 동결 체크 및 해제
        val iterator = GameManager.frozenPlayers.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val uuid = entry.key
            val freezeUntil = entry.value

            if (now >= freezeUntil) {
                // 동결 해제 시각이 지남
                iterator.remove()
                val player = Bukkit.getPlayer(uuid)
                if (player != null && player.isOnline) {
                    player.removePotionEffect(PotionEffectType.SLOWNESS)
                    player.removePotionEffect(PotionEffectType.JUMP_BOOST)
                    
                    if (GameManager.hunters.contains(uuid)) {
                        // 헌터 출발/부활
                        val title = Title.title(
                            Component.text("출발!").color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                            Component.text("러너를 추적하세요!").color(NamedTextColor.YELLOW),
                            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
                        )
                        player.showTitle(title)
                        player.playSound(player.location, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f)
                    }
                }
            } else {
                // 아직 동결 중일 때, 출발 대기 시간 카운트다운 방송 (헌터 전원 대상일 때만)
                // 개별 부활 타이머는 액션바나 타이틀로 보여줄 수 있지만, 전체 카운트다운은 gameStartTime 기준
                val remainSeconds = ((freezeUntil - now) / 1000).toInt()
                val player = Bukkit.getPlayer(uuid)
                if (player != null && player.isOnline) {
                    player.sendActionBar(Component.text("${remainSeconds}초 후 이동 가능").color(NamedTextColor.RED))
                }
            }
        }

        // 전체 카운트다운 (게임 시작 직후 헌터 대기용)
        val startRemain = (GameManager.gameStartTime + (GameManager.config.hunterDelay * 1000L) - now) / 1000
        if (startRemain in 1..5) {
            Bukkit.broadcast(Component.text("[맨헌트] 헌터 출발까지 ${startRemain}초!").color(NamedTextColor.RED))
            Bukkit.getOnlinePlayers().forEach { p ->
                if (GameManager.hunters.contains(p.uniqueId)) {
                    p.playSound(p.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f)
                }
            }
        } else if (startRemain == 10L || startRemain == 30L || startRemain == 60L) {
            Bukkit.broadcast(Component.text("[맨헌트] 헌터 출발까지 ${startRemain}초 남았습니다!").color(NamedTextColor.YELLOW))
        }

        // 2. 나침반 타겟 갱신 및 나침반 중복 제거
        val onlineHunters = Bukkit.getOnlinePlayers().filter { GameManager.hunters.contains(it.uniqueId) }
        val aliveRunners = Bukkit.getOnlinePlayers().filter { 
            GameManager.runners.contains(it.uniqueId) && !GameManager.deadRunners.contains(it.uniqueId) 
        }

        for (hunter in onlineHunters) {
            // 나침반 개수 정리 (1개만 유지)
            val compassCount = hunter.inventory.contents.count { it?.type == Material.COMPASS }
            if (compassCount > 1) {
                var removed = 0
                val targetRemove = compassCount - 1
                for (i in 0 until hunter.inventory.size) {
                    val item = hunter.inventory.getItem(i)
                    if (item?.type == Material.COMPASS) {
                        hunter.inventory.setItem(i, null)
                        removed++
                        if (removed >= targetRemove) break
                    }
                }
            } else if (compassCount == 0 && !GameManager.frozenPlayers.containsKey(hunter.uniqueId)) {
                // 게임 중인데 나침반이 없다면 1개 지급
                val compass = ItemStack(Material.COMPASS)
                compass.editMeta { meta ->
                    meta.displayName(Component.text("러너 추적 나침반").color(NamedTextColor.GREEN))
                }
                hunter.inventory.addItem(compass)
            }

            // 동결 중이 아니면 타겟 갱신
            if (!GameManager.frozenPlayers.containsKey(hunter.uniqueId)) {
                var targetRunner: Player? = null
                var minDistance = Double.MAX_VALUE

                for (runner in aliveRunners) {
                    if (runner.world == hunter.world) {
                        val dist = hunter.location.distance(runner.location)
                        if (dist < minDistance) {
                            minDistance = dist
                            targetRunner = runner
                        }
                    }
                }

                if (targetRunner != null) {
                    hunter.compassTarget = targetRunner.location
                }
            }
        }

        // 3. 심장박동 사운드 및 알림
        val radius = GameManager.config.heartbeatRadius.toDouble()
        for (runner in aliveRunners) {
            var nearHunter = false
            for (hunter in onlineHunters) {
                if (!GameManager.frozenPlayers.containsKey(hunter.uniqueId) && hunter.gameMode == GameMode.SURVIVAL) {
                    if (hunter.world == runner.world && hunter.location.distance(runner.location) <= radius) {
                        nearHunter = true
                        break
                    }
                }
            }

            if (nearHunter) {
                runner.playSound(runner.location, Sound.ENTITY_WARDEN_HEARTBEAT, 1.5f, 1.0f)
                runner.playSound(runner.location, Sound.BLOCK_NOTE_BLOCK_BASS, 1.5f, 0.6f)
                runner.sendActionBar(Component.text("[ ♥ 심장박동 감지됨! 가까이에 헌터가 있습니다! ]").color(NamedTextColor.RED).decorate(TextDecoration.BOLD))
            }
        }
    }
}
