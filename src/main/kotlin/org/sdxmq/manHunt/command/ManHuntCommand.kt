package org.sdxmq.manHunt.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.sdxmq.manHunt.gui.SettingsGui
import org.sdxmq.manHunt.manager.GameManager

class ManHuntCommand : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(Component.text("플레이어만 사용할 수 있습니다.").color(NamedTextColor.RED))
            return true
        }

        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(Component.text("[맨헌트] 권한이 없습니다.").color(NamedTextColor.RED))
            return true
        }

        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        when (args[0]) {
            "설정" -> {
                SettingsGui.open(sender)
            }
            "헌터" -> {
                if (args.size < 2) {
                    sender.sendMessage(Component.text("[맨헌트] 사용법: /맨헌트 헌터 <플레이어>").color(NamedTextColor.RED))
                    return true
                }
                val target = Bukkit.getPlayerExact(args[1])
                if (target == null) {
                    sender.sendMessage(Component.text("[맨헌트] 플레이어를 찾을 수 없습니다.").color(NamedTextColor.RED))
                    return true
                }
                if (GameManager.hunters.contains(target.uniqueId)) {
                    GameManager.hunters.remove(target.uniqueId)
                    sender.sendMessage(Component.text("[맨헌트] ${target.name} 님을 헌터 목록에서 제거했습니다.").color(NamedTextColor.WHITE))
                } else {
                    GameManager.addHunter(target.uniqueId)
                    sender.sendMessage(Component.text("[맨헌트] ${target.name} 님을 헌터로 등록했습니다.").color(NamedTextColor.GREEN))
                }
            }
            "러너" -> {
                if (args.size < 2) {
                    sender.sendMessage(Component.text("[맨헌트] 사용법: /맨헌트 러너 <플레이어>").color(NamedTextColor.RED))
                    return true
                }
                val target = Bukkit.getPlayerExact(args[1])
                if (target == null) {
                    sender.sendMessage(Component.text("[맨헌트] 플레이어를 찾을 수 없습니다.").color(NamedTextColor.RED))
                    return true
                }
                if (GameManager.runners.contains(target.uniqueId)) {
                    GameManager.runners.remove(target.uniqueId)
                    sender.sendMessage(Component.text("[맨헌트] ${target.name} 님을 러너 목록에서 제거했습니다.").color(NamedTextColor.WHITE))
                } else {
                    GameManager.addRunner(target.uniqueId)
                    sender.sendMessage(Component.text("[맨헌트] ${target.name} 님을 러너로 등록했습니다.").color(NamedTextColor.AQUA))
                }
            }
            "시작" -> {
                if (GameManager.isRunning) {
                    sender.sendMessage(Component.text("[맨헌트] 이미 게임이 진행 중입니다.").color(NamedTextColor.RED))
                    return true
                }
                if (GameManager.runners.isEmpty()) {
                    sender.sendMessage(Component.text("[맨헌트] 러너가 1명 이상 등록되어야 합니다. (/맨헌트 러너 <플레이어>)").color(NamedTextColor.RED))
                    return true
                }
                val success = GameManager.startGame()
                if (!success) {
                    sender.sendMessage(Component.text("[맨헌트] 헌터로 참여할 플레이어가 없습니다. (최소 2인 필요)").color(NamedTextColor.RED))
                }
            }
            "종료" -> {
                if (!GameManager.isRunning) {
                    sender.sendMessage(Component.text("[맨헌트] 진행 중인 게임이 없습니다.").color(NamedTextColor.RED))
                    return true
                }
                GameManager.stopGame(force = true)
            }
            else -> sendHelp(sender)
        }
        return true
    }

    private fun sendHelp(sender: Player) {
        sender.sendMessage(Component.text("[맨헌트 명령어]").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
        sender.sendMessage(Component.text("/맨헌트 시작").color(NamedTextColor.YELLOW).append(Component.text(" - 게임을 시작합니다.").color(NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("/맨헌트 종료").color(NamedTextColor.YELLOW).append(Component.text(" - 게임을 강제 종료합니다.").color(NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("/맨헌트 헌터 <플레이어>").color(NamedTextColor.YELLOW).append(Component.text(" - 헌터 등록/해제").color(NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("/맨헌트 러너 <플레이어>").color(NamedTextColor.YELLOW).append(Component.text(" - 러너 등록/해제").color(NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("/맨헌트 설정").color(NamedTextColor.YELLOW).append(Component.text(" - 설정 GUI 메뉴를 엽니다.").color(NamedTextColor.GRAY)))
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): MutableList<String> {
        val completions = mutableListOf<String>()
        if (args.size == 1) {
            val options = listOf("시작", "종료", "헌터", "러너", "설정")
            completions.addAll(options.filter { it.startsWith(args[0]) })
        } else if (args.size == 2 && (args[0] == "헌터" || args[0] == "러너")) {
            completions.addAll(Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) })
        }
        return completions
    }
}
