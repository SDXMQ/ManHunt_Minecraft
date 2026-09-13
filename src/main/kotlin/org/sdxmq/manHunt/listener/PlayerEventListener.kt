package org.sdxmq.manHunt.listener

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.EnderDragon
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.sdxmq.manHunt.manager.GameManager
import java.time.Duration

class PlayerEventListener : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        val uuid = player.uniqueId

        if (GameManager.isRunning) {
            // 게임 도중 난입 시 러너도 헌터도 아니면 헌터로 강제 배정
            if (!GameManager.runners.contains(uuid) && !GameManager.hunters.contains(uuid)) {
                GameManager.addHunter(uuid)
                player.gameMode = GameMode.SURVIVAL
                player.sendMessage(Component.text("[맨헌트] 게임이 진행 중이므로 헌터로 자동 배정되었습니다!").color(NamedTextColor.RED).decorate(TextDecoration.BOLD))
            }

            // 동결 상태인지 확인 (포션 효과 부여는 삭제됨, PlayerMoveEvent 로 제어)
            if (GameManager.deadRunners.contains(uuid)) {
                // 죽은 러너로 기록되어 있으면 영구 관전 모드 적용
                player.gameMode = GameMode.SPECTATOR
            }
        }
    }

    // 사망 시 위치를 기억하기 위해 Map 사용
    private val deathLocations = mutableMapOf<java.util.UUID, org.bukkit.Location>()

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        val player = event.player
        if (!GameManager.isRunning) return

        if (GameManager.runners.contains(player.uniqueId) || GameManager.hunters.contains(player.uniqueId)) {
            deathLocations[player.uniqueId] = player.location
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        val uuid = player.uniqueId
        if (!GameManager.isRunning) return

        val loc = deathLocations.remove(uuid) ?: return

        if (GameManager.hunters.contains(uuid)) {
            // 헌터 리스폰: 사망 위치로 보내고 부활 대기시간 부여 (동결)
            event.respawnLocation = loc
            val waitMillis = GameManager.config.hunterRespawnDelay * 1000L
            GameManager.frozenPlayers[uuid] = System.currentTimeMillis() + waitMillis
            
            Bukkit.broadcast(Component.text("[맨헌트] 헌터 ${player.name} 님이 사망했습니다! (${GameManager.config.hunterRespawnDelay}초 동안 동결)").color(NamedTextColor.RED))
            
        } else if (GameManager.runners.contains(uuid)) {
            // 러너 리스폰: 사망자 목록에 추가 및 사망 방송
            GameManager.deadRunners.add(uuid)
            Bukkit.broadcast(Component.text("[맨헌트] 러너 ${player.name} 님이 사망했습니다!").color(NamedTextColor.RED))
            
            // 승패 판정 먼저 확인
            val isGameOver = GameManager.checkHunterWin()
            
            if (!isGameOver) {
                // 게임이 아직 끝나지 않은 경우 (다른 살아있는 러너가 있는 경우) 관전 모드로 전환
                Bukkit.getScheduler().runTask(org.bukkit.plugin.java.JavaPlugin.getPlugin(org.sdxmq.manHunt.ManHunt::class.java), Runnable {
                    if (player.isOnline && GameManager.isRunning) {
                        player.gameMode = GameMode.SPECTATOR
                    }
                })
                
                val title = Title.title(
                    Component.text("탈락").color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                    Component.text("관전 모드로 전환되었습니다.").color(NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofMillis(500))
                )
                player.showTitle(title)
            }
        }
    }

    // 동결 상태 시 XYZ 이동 방지 (시야는 허용)
    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        if (!GameManager.isRunning) return
        val player = event.player
        if (GameManager.frozenPlayers.containsKey(player.uniqueId)) {
            val from = event.from
            val to = event.to
            if (from.x != to.x || from.y != to.y || from.z != to.z) {
                // 시야(Pitch/Yaw)는 변경 가능하도록 from 위치에 시야만 to로 덮어씌움
                event.setTo(from.apply {
                    pitch = to.pitch
                    yaw = to.yaw
                })
            }
        }
    }

    // 동결 상태 플레이어 행동 제한
    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        if (!GameManager.isRunning) return
        val entity = event.entity
        if (entity is Player && GameManager.frozenPlayers.containsKey(entity.uniqueId)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onDamageByEntity(event: EntityDamageByEntityEvent) {
        if (!GameManager.isRunning) return
        val damager = event.damager
        if (damager is Player && GameManager.frozenPlayers.containsKey(damager.uniqueId)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if (!GameManager.isRunning) return
        if (GameManager.frozenPlayers.containsKey(event.player.uniqueId)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (!GameManager.isRunning) return
        if (GameManager.frozenPlayers.containsKey(event.player.uniqueId)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onDrop(event: PlayerDropItemEvent) {
        if (!GameManager.isRunning) return
        if (GameManager.frozenPlayers.containsKey(event.player.uniqueId)) {
            event.isCancelled = true
        }
    }

    // 나침반 수동 갱신 및 나침반 제작(변환) 알림
    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (!GameManager.isRunning) return
        val player = event.player
        val item = event.item ?: return

        if (GameManager.hunters.contains(player.uniqueId) && item.type == Material.COMPASS) {
            if (event.action.isRightClick) {
                val trackerName = "러너 추적 나침반"
                val meta = item.itemMeta
                val currentName = meta?.displayName()?.let { 
                    net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(it)
                }

                if (currentName != trackerName) {
                    // 1. 일반 나침반 우클릭 시
                    // 인벤토리에 이미 추적 나침반이 있는지 확인
                    val hasTracker = player.inventory.contents.any {
                        it?.type == Material.COMPASS &&
                        it.itemMeta?.displayName()?.let { name -> net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(name) == trackerName } == true
                    }

                    if (hasTracker) {
                        // 이미 있다면 캔슬하고 아무것도 안 함
                        event.isCancelled = true
                        player.sendMessage(Component.text("[맨헌트] 이미 러너 추적 나침반을 소지하고 있습니다.").color(NamedTextColor.RED))
                        return
                    } else {
                        // 없다면 현재 나침반을 추적 나침반으로 변경
                        meta?.displayName(Component.text(trackerName).color(NamedTextColor.GREEN))
                        item.itemMeta = meta
                        player.sendMessage(Component.text("[맨헌트] 일반 나침반이 러너 추적 나침반으로 변환되었습니다.").color(NamedTextColor.GREEN))
                        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f)
                        event.isCancelled = true // 굳이 블록 상호작용 안 일어나게 캔슬
                    }
                }

                // 2. 이후 (또는 원래) 추적 나침반일 경우 타겟 탐색
                var targetName: String? = null
                var minDistance = Double.MAX_VALUE
                
                val aliveRunners = Bukkit.getOnlinePlayers().filter { 
                    GameManager.runners.contains(it.uniqueId) && !GameManager.deadRunners.contains(it.uniqueId) 
                }
                
                for (runner in aliveRunners) {
                    if (runner.world == player.world) {
                        val dist = player.location.distance(runner.location)
                        if (dist < minDistance) {
                            minDistance = dist
                            targetName = runner.name
                        }
                    }
                }
                
                if (targetName != null) {
                    player.sendActionBar(Component.text("[나침반] 러너 $targetName 실시간 추적 중").color(NamedTextColor.GREEN))
                    player.playSound(player.location, Sound.ITEM_LODESTONE_COMPASS_LOCK, 1.2f, 1.0f)
                } else {
                    player.sendActionBar(Component.text("[나침반] 같은 월드에 추적 가능한 살아있는 러너가 없습니다.").color(NamedTextColor.RED))
                }
            }
        }
    }

    // 엔더 드래곤 처치 시 러너 승리
    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        if (!GameManager.isRunning) return
        if (event.entity is EnderDragon) {
            GameManager.checkRunnerWin()
        }
    }
}
