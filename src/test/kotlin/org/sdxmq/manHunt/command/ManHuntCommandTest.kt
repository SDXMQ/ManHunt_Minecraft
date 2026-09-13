package org.sdxmq.manHunt.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sdxmq.manHunt.manager.GameManager
import java.lang.reflect.Proxy
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ManHuntCommandTest {

    private lateinit var command: ManHuntCommand
    private val sentMessages = mutableListOf<String>()
    private val onlinePlayers = mutableListOf<Player>()
    private val playerMap = mutableMapOf<String, Player>()

    private fun createMockPlayer(
        name: String,
        uuid: UUID = UUID.randomUUID(),
        isAdmin: Boolean = true
    ): Player {
        val handler = java.lang.reflect.InvocationHandler { _, method, args ->
            when (method.name) {
                "getName" -> name
                "getUniqueId" -> uuid
                "hasPermission" -> {
                    val perm = args?.get(0) as? String
                    if (perm == "manhunt.admin") isAdmin else false
                }
                "sendMessage" -> {
                    val comp = args?.get(0)
                    if (comp is Component) {
                        sentMessages.add(PlainTextComponentSerializer.plainText().serialize(comp))
                    } else if (comp is String) {
                        sentMessages.add(comp)
                    }
                    null
                }
                "canSee" -> true
                "equals" -> args?.get(0)?.let { it is Player && it.name == name } ?: false
                "hashCode" -> name.hashCode()
                "toString" -> "MockPlayer($name)"
                else -> when (method.returnType) {
                    Boolean::class.javaPrimitiveType -> false
                    Int::class.javaPrimitiveType -> 0
                    Long::class.javaPrimitiveType -> 0L
                    else -> null
                }
            }
        }
        val mock = Proxy.newProxyInstance(
            Player::class.java.classLoader,
            arrayOf(Player::class.java),
            handler
        ) as Player
        onlinePlayers.add(mock)
        playerMap[name] = mock
        return mock
    }

    private fun createMockSender(isAdmin: Boolean = true): CommandSender {
        val handler = java.lang.reflect.InvocationHandler { _, method, args ->
            when (method.name) {
                "getName" -> "Console"
                "hasPermission" -> {
                    val perm = args?.get(0) as? String
                    if (perm == "manhunt.admin") isAdmin else false
                }
                "sendMessage" -> {
                    val comp = args?.get(0)
                    if (comp is Component) {
                        sentMessages.add(PlainTextComponentSerializer.plainText().serialize(comp))
                    } else if (comp is String) {
                        sentMessages.add(comp)
                    }
                    null
                }
                else -> when (method.returnType) {
                    Boolean::class.javaPrimitiveType -> false
                    Int::class.javaPrimitiveType -> 0
                    Long::class.javaPrimitiveType -> 0L
                    else -> null
                }
            }
        }
        return Proxy.newProxyInstance(
            CommandSender::class.java.classLoader,
            arrayOf(CommandSender::class.java),
            handler
        ) as CommandSender
    }

    private class DummyCommand(name: String) : Command(name) {
        override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean = true
    }

    private fun createDummyCommand(name: String): Command = DummyCommand(name)

    @BeforeEach
    fun setUp() {
        sentMessages.clear()
        onlinePlayers.clear()
        playerMap.clear()
        GameManager.isRunning = false
        GameManager.runners.clear()
        GameManager.hunters.clear()
        GameManager.deadRunners.clear()
        GameManager.frozenPlayers.clear()

        val logger = java.util.logging.Logger.getAnonymousLogger()
        val serverHandler = java.lang.reflect.InvocationHandler { _, method, args ->
            when (method.name) {
                "getOnlinePlayers" -> onlinePlayers
                "getPlayerExact" -> playerMap[args?.get(0) as? String]
                "getLogger" -> logger
                "getName" -> "MockServer"
                else -> when (method.returnType) {
                    Boolean::class.javaPrimitiveType -> false
                    Int::class.javaPrimitiveType -> 0
                    Long::class.javaPrimitiveType -> 0L
                    else -> null
                }
            }
        }
        val mockServer = Proxy.newProxyInstance(
            Server::class.java.classLoader,
            arrayOf(Server::class.java),
            serverHandler
        ) as Server

        val serverField = Bukkit::class.java.getDeclaredField("server")
        serverField.isAccessible = true
        serverField.set(null, mockServer)

        command = ManHuntCommand()
    }

    @AfterEach
    fun tearDown() {
        try {
            val serverField = Bukkit::class.java.getDeclaredField("server")
            serverField.isAccessible = true
            serverField.set(null, null)
        } catch (_: Exception) {}
    }

    @Test
    fun `tab completion on manhunt root offers English subcommands`() {
        val admin = createMockPlayer("Admin")
        val dummyCmd = createDummyCommand("manhunt")

        val result = command.onTabComplete(admin, dummyCmd, "manhunt", arrayOf(""))
        assertEquals(listOf("start", "stop", "runner", "option"), result)
    }

    @Test
    fun `tab completion on manhunt filters by prefix`() {
        val admin = createMockPlayer("Admin")
        val dummyCmd = createDummyCommand("manhunt")

        assertEquals(listOf("start", "stop"), command.onTabComplete(admin, dummyCmd, "manhunt", arrayOf("st")))
        assertEquals(listOf("start"), command.onTabComplete(admin, dummyCmd, "manhunt", arrayOf("sta")))
        assertEquals(listOf("stop"), command.onTabComplete(admin, dummyCmd, "manhunt", arrayOf("sto")))
        assertEquals(listOf("runner"), command.onTabComplete(admin, dummyCmd, "manhunt", arrayOf("ru")))
        assertEquals(listOf("option"), command.onTabComplete(admin, dummyCmd, "manhunt", arrayOf("op")))
    }

    @Test
    fun `tab completion on korean command root offers Korean subcommands`() {
        val admin = createMockPlayer("Admin")
        val dummyCmd = createDummyCommand("맨헌트")

        val result = command.onTabComplete(admin, dummyCmd, "맨헌트", arrayOf(""))
        assertEquals(listOf("시작", "종료", "러너", "설정"), result)
        assertEquals(listOf("시작"), command.onTabComplete(admin, dummyCmd, "맨헌트", arrayOf("시")))
    }

    @Test
    fun `tab completion supports cross-language fallback`() {
        val admin = createMockPlayer("Admin")
        val dummyCmd = createDummyCommand("manhunt")

        // English command with Korean prefix falls back
        assertEquals(listOf("시작"), command.onTabComplete(admin, dummyCmd, "manhunt", arrayOf("시")))
        assertEquals(listOf("러너"), command.onTabComplete(admin, dummyCmd, "manhunt", arrayOf("러")))

        // Korean command with English prefix falls back
        assertEquals(listOf("start", "stop"), command.onTabComplete(admin, dummyCmd, "맨헌트", arrayOf("st")))
        assertEquals(listOf("runner"), command.onTabComplete(admin, dummyCmd, "맨헌트", arrayOf("ru")))
    }

    @Test
    fun `tab completion handles namespaced commands`() {
        val admin = createMockPlayer("Admin")
        val dummyCmd = createDummyCommand("manhunt")

        assertEquals(listOf("start", "stop", "runner", "option"), command.onTabComplete(admin, dummyCmd, "manhunt:manhunt", arrayOf("")))
        assertEquals(listOf("시작", "종료", "러너", "설정"), command.onTabComplete(admin, dummyCmd, "manhunt:맨헌트", arrayOf("")))
    }

    @Test
    fun `tab completion for runner completes online players`() {
        val admin = createMockPlayer("Admin")
        createMockPlayer("Alice")
        createMockPlayer("Bob")
        val dummyCmd = createDummyCommand("manhunt")

        val completions = command.onTabComplete(admin, dummyCmd, "manhunt", arrayOf("runner", ""))
        assertEquals(listOf("Admin", "Alice", "Bob"), completions)

        val filteredCompletions = command.onTabComplete(admin, dummyCmd, "manhunt", arrayOf("runner", "Al"))
        assertEquals(listOf("Alice"), filteredCompletions)

        // Case insensitivity for runner
        val caseCompletions = command.onTabComplete(admin, dummyCmd, "manhunt", arrayOf("RUNNER", "B"))
        assertEquals(listOf("Bob"), caseCompletions)

        // Korean runner
        val koreanCompletions = command.onTabComplete(admin, dummyCmd, "맨헌트", arrayOf("러너", "Al"))
        assertEquals(listOf("Alice"), koreanCompletions)
    }

    @Test
    fun `tab completion for hunter does not complete players`() {
        val admin = createMockPlayer("Admin")
        createMockPlayer("Alice")
        val dummyCmd = createDummyCommand("manhunt")

        // hunter was removed from command system in commit cb7acb06
        val completions = command.onTabComplete(admin, dummyCmd, "manhunt", arrayOf("hunter", ""))
        assertTrue(completions.isEmpty())

        val koreanCompletions = command.onTabComplete(admin, dummyCmd, "맨헌트", arrayOf("헌터", ""))
        assertTrue(koreanCompletions.isEmpty())
    }

    @Test
    fun `tab completion for subcommands without extra arguments returns empty list`() {
        val admin = createMockPlayer("Admin")
        val dummyCmd = createDummyCommand("manhunt")

        assertTrue(command.onTabComplete(admin, dummyCmd, "manhunt", arrayOf("start", "")).isEmpty())
        assertTrue(command.onTabComplete(admin, dummyCmd, "manhunt", arrayOf("stop", "")).isEmpty())
        assertTrue(command.onTabComplete(admin, dummyCmd, "manhunt", arrayOf("option", "")).isEmpty())
        assertTrue(command.onTabComplete(admin, dummyCmd, "manhunt", arrayOf("runner", "Alice", "")).isEmpty())
    }

    @Test
    fun `tab completion is blocked without manhunt admin permission`() {
        val normalUser = createMockPlayer("User", isAdmin = false)
        val dummyCmd = createDummyCommand("manhunt")

        val result = command.onTabComplete(normalUser, dummyCmd, "manhunt", arrayOf(""))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `onCommand blocks non-players and non-admins`() {
        val dummyCmd = createDummyCommand("manhunt")

        val consoleSender = createMockSender(isAdmin = true)
        command.onCommand(consoleSender, dummyCmd, "manhunt", emptyArray())
        assertTrue(sentMessages.any { it.contains("플레이어만") })

        sentMessages.clear()
        val nonAdminPlayer = createMockPlayer("Normal", isAdmin = false)
        command.onCommand(nonAdminPlayer, dummyCmd, "manhunt", emptyArray())
        assertTrue(sentMessages.any { it.contains("권한이 없습니다") })
    }

    @Test
    fun `onCommand manhunt with empty args shows English help without hunter`() {
        val admin = createMockPlayer("Admin")
        val dummyCmd = createDummyCommand("manhunt")

        command.onCommand(admin, dummyCmd, "manhunt", emptyArray())

        assertTrue(sentMessages.any { it.contains("/manhunt start") })
        assertTrue(sentMessages.any { it.contains("/manhunt stop") })
        assertTrue(sentMessages.any { it.contains("/manhunt runner <플레이어>") })
        assertTrue(sentMessages.any { it.contains("/manhunt option") })
        assertFalse(sentMessages.any { it.contains("hunter") })
    }

    @Test
    fun `onCommand manhunt runner toggles player`() {
        val admin = createMockPlayer("Admin")
        val target = createMockPlayer("TargetPlayer")
        val dummyCmd = createDummyCommand("manhunt")

        // Missing arg
        command.onCommand(admin, dummyCmd, "manhunt", arrayOf("runner"))
        assertTrue(sentMessages.any { it.contains("사용법: /manhunt runner <플레이어>") })

        // Add runner
        sentMessages.clear()
        command.onCommand(admin, dummyCmd, "manhunt", arrayOf("runner", "TargetPlayer"))
        assertTrue(GameManager.runners.contains(target.uniqueId))
        assertTrue(sentMessages.any { it.contains("TargetPlayer 님을 러너로 등록했습니다") })

        // Toggle / remove runner
        sentMessages.clear()
        command.onCommand(admin, dummyCmd, "manhunt", arrayOf("runner", "TargetPlayer"))
        assertFalse(GameManager.runners.contains(target.uniqueId))
        assertTrue(sentMessages.any { it.contains("TargetPlayer 님을 러너 목록에서 제거했습니다") })
    }

    @Test
    fun `onCommand manhunt start without runners prompts runner command`() {
        val admin = createMockPlayer("Admin")
        val dummyCmd = createDummyCommand("manhunt")

        command.onCommand(admin, dummyCmd, "manhunt", arrayOf("start"))
        assertTrue(sentMessages.any { it.contains("/manhunt runner <플레이어>") })

        sentMessages.clear()
        command.onCommand(admin, dummyCmd, "맨헌트", arrayOf("시작"))
        assertTrue(sentMessages.any { it.contains("/맨헌트 러너 <플레이어>") })

        sentMessages.clear()
        command.onCommand(admin, dummyCmd, "맨헌트", arrayOf("start"))
        assertTrue(sentMessages.any { it.contains("/맨헌트 runner <플레이어>") })
    }

    @Test
    fun `onCommand stop when not running reports no active game`() {
        val admin = createMockPlayer("Admin")
        val dummyCmd = createDummyCommand("manhunt")

        command.onCommand(admin, dummyCmd, "manhunt", arrayOf("stop"))
        assertTrue(sentMessages.any { it.contains("진행 중인 게임이 없습니다") })
    }
}
