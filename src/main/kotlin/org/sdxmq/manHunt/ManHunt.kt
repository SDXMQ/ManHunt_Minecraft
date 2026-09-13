package org.sdxmq.manHunt

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.sdxmq.manHunt.command.ManHuntCommand
import org.sdxmq.manHunt.gui.SettingsGui
import org.sdxmq.manHunt.listener.PlayerEventListener
import org.sdxmq.manHunt.manager.GameManager
import org.sdxmq.manHunt.task.GameTickTask

class ManHunt : JavaPlugin() {

    override fun onEnable() {
        // 커맨드 등록
        val executor = ManHuntCommand()
        getCommand("맨헌트")?.let {
            it.setExecutor(executor)
            it.tabCompleter = executor
        }
        getCommand("manhunt")?.let {
            it.setExecutor(executor)
            it.tabCompleter = executor
        }

        // 리스너 등록
        val pm = Bukkit.getPluginManager()
        pm.registerEvents(PlayerEventListener(), this)
        pm.registerEvents(SettingsGui, this)

        // 1초마다 실행되는 글로벌 태스크 등록 (20 틱 = 1초)
        Bukkit.getScheduler().runTaskTimer(this, GameTickTask(), 20L, 20L)
        
        logger.info("[ManHunt] 맨헌트 플러그인이 활성화되었습니다.")
    }

    override fun onDisable() {
        // 게임 중이었으면 강제 종료 및 효과 초기화
        GameManager.stopGame(force = true)
        logger.info("[ManHunt] 맨헌트 플러그인이 비활성화되었습니다.")
    }
}
