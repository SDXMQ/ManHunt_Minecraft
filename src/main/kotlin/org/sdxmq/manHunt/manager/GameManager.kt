package org.sdxmq.manHunt.manager

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.time.Duration
import java.util.UUID

enum class WinMode {
    ALL, ONE
}

data class ConfigData(
    var heartbeatRadius: Int = 50,
    var hunterDelay: Int = 60,
    var hunterRespawnDelay: Int = 90,
    var runnerWinMode: WinMode = WinMode.ALL
)

object GameManager {
    var isRunning: Boolean = false
    val config = ConfigData()

    val runners = mutableSetOf<UUID>()
    val hunters = mutableSetOf<UUID>()
    val deadRunners = mutableSetOf<UUID>()

    // UUID -> 동결 해제 시각 (System.currentTimeMillis() 기준)
    // 값이 Long.MAX_VALUE 이면 영구 동결 (죽은 러너 등)
    val frozenPlayers = mutableMapOf<UUID, Long>()

    // 시작 시각 (헌터 출발 대기 타이머용)
    var gameStartTime: Long = 0L

    fun addRunner(uuid: UUID): Boolean {
        hunters.remove(uuid)
        return runners.add(uuid)
    }

    fun addHunter(uuid: UUID): Boolean {
        runners.remove(uuid)
        return hunters.add(uuid)
    }

    fun startGame(): Boolean {
        if (isRunning) return false
        if (runners.isEmpty()) return false

        // 접속자 중 러너가 아닌 모든 사람을 헌터로 자동 등록
        hunters.clear()
        Bukkit.getOnlinePlayers().forEach { player ->
            if (!runners.contains(player.uniqueId)) {
                hunters.add(player.uniqueId)
            }
        }

        if (hunters.isEmpty()) return false

        isRunning = true
        deadRunners.clear()
        frozenPlayers.clear()
        gameStartTime = System.currentTimeMillis()

        broadcast(
            Component.text("==============================================").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
        )
        broadcast(
            Component.text("[맨헌트] 게임이 시작되었습니다!").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
        )
        broadcast(
            Component.text("러너는 헌터를 피해 엔더 드래곤을 처치해야 승리합니다!").color(NamedTextColor.AQUA)
        )
        broadcast(
            Component.text("헌터는 ${config.hunterDelay}초 후에 출발합니다!").color(NamedTextColor.RED)
        )
        broadcast(
            Component.text("==============================================").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
        )

        // 초기화
        Bukkit.getOnlinePlayers().forEach { player ->
            val uuid = player.uniqueId
            if (runners.contains(uuid)) {
                player.gameMode = GameMode.SURVIVAL
                val title = Title.title(
                    Component.text("러너!").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD),
                    Component.text("헌터를 피해 드래곤을 처치하세요!").color(NamedTextColor.WHITE),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
                )
                player.showTitle(title)
            } else if (hunters.contains(uuid)) {
                player.gameMode = GameMode.SURVIVAL

                // 헌터 동결 (hunterDelay 만큼)
                frozenPlayers[uuid] = gameStartTime + (config.hunterDelay * 1000L)

                val title = Title.title(
                    Component.text("헌터!").color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                    Component.text("${config.hunterDelay}초 후 출발합니다!").color(NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
                )
                player.showTitle(title)
            }
        }
        return true
    }

    fun stopGame(force: Boolean = false) {
        if (!isRunning) return
        isRunning = false
        frozenPlayers.clear()
        deadRunners.clear()

        if (force) {
            broadcast(Component.text("[맨헌트] 관리자에 의해 게임이 강제 종료되었습니다.").color(NamedTextColor.RED).decorate(TextDecoration.BOLD))
        }

        Bukkit.getOnlinePlayers().forEach { player ->
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS)
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST)
            if (player.gameMode == GameMode.SPECTATOR) {
                player.gameMode = GameMode.SURVIVAL
            }
        }
    }

    fun checkHunterWin(): Boolean {
        if (!isRunning) return false
        var hunterWin = false
        if (config.runnerWinMode == WinMode.ONE) {
            hunterWin = true
        } else {
            val allDead = runners.all { deadRunners.contains(it) }
            if (allDead) hunterWin = true
        }

        if (hunterWin) {
            stopGame(false)
            broadcast(Component.text("==============================================").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
            broadcast(Component.text("[맨헌트] 헌터 팀의 승리입니다!").color(NamedTextColor.RED).decorate(TextDecoration.BOLD))
            broadcast(Component.text("러너가 모두 잡혔습니다.").color(NamedTextColor.GRAY))
            broadcast(Component.text("==============================================").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))

            Bukkit.getOnlinePlayers().forEach { player ->
                val title = Title.title(
                    Component.text("헌터 팀 승리!").color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                    Component.text("러너를 모두 사냥했습니다!").color(NamedTextColor.WHITE),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofMillis(500))
                )
                player.showTitle(title)
                player.playSound(player.location, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f)
            }
            return true
        }
        return false
    }

    fun checkRunnerWin() {
        if (!isRunning) return
        stopGame(false)
        broadcast(Component.text("==============================================").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
        broadcast(Component.text("[맨헌트] 러너 팀의 승리입니다!").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD))
        broadcast(Component.text("엔더 드래곤이 처치되었습니다!").color(NamedTextColor.GRAY))
        broadcast(Component.text("==============================================").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))

        Bukkit.getOnlinePlayers().forEach { player ->
            val title = Title.title(
                Component.text("러너 팀 승리!").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD),
                Component.text("엔더 드래곤 처치 성공!").color(NamedTextColor.WHITE),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofMillis(500))
            )
            player.showTitle(title)
            player.playSound(player.location, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f)
        }
    }

    private fun broadcast(message: Component) {
        Bukkit.getServer().broadcast(message)
    }
}
