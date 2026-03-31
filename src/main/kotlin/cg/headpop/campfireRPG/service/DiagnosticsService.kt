package cg.headpop.campfireRPG.service

import cg.headpop.campfireRPG.CampfireRPG
import kotlin.math.max

data class DiagnosticsSnapshot(
    val ticksProcessed: Long,
    val campfiresActivated: Long,
    val playersBuffed: Long,
    val averageTickMs: Double,
    val maxTickMs: Double,
)

class DiagnosticsService(
    private val plugin: CampfireRPG,
) {

    private var debugEnabled = false
    private var ticksProcessed = 0L
    private var campfiresActivated = 0L
    private var playersBuffed = 0L
    private var totalTickNanos = 0L
    private var maxTickNanos = 0L
    private var ticksSinceLog = 0L

    fun reload() {
        debugEnabled = plugin.settingsLoader.settings.debug.enabledByDefault
        ticksSinceLog = 0L
    }

    fun isDebugEnabled(): Boolean = debugEnabled

    fun toggleDebug(): Boolean {
        debugEnabled = !debugEnabled
        ticksSinceLog = 0L
        return debugEnabled
    }

    fun recordTick(durationNanos: Long, activatedCampfires: Int, buffedPlayers: Int) {
        ticksProcessed++
        campfiresActivated += activatedCampfires
        playersBuffed += buffedPlayers
        totalTickNanos += durationNanos
        maxTickNanos = max(maxTickNanos, durationNanos)
        ticksSinceLog += plugin.settingsLoader.settings.scan.intervalTicks

        if (debugEnabled && ticksSinceLog >= plugin.settingsLoader.settings.debug.logIntervalTicks) {
            ticksSinceLog = 0L
            val snapshot = snapshot()
            plugin.logger.info(
                "CampfireRPG debug | ticks=${snapshot.ticksProcessed} active=${snapshot.campfiresActivated} " +
                    "buffed=${snapshot.playersBuffed} avg=${"%.2f".format(snapshot.averageTickMs)}ms " +
                    "max=${"%.2f".format(snapshot.maxTickMs)}ms tracked=${plugin.campfireRegistry.size()}"
            )
        }
    }

    fun snapshot(): DiagnosticsSnapshot {
        val average = if (ticksProcessed == 0L) 0.0 else totalTickNanos.toDouble() / ticksProcessed / 1_000_000.0
        return DiagnosticsSnapshot(
            ticksProcessed = ticksProcessed,
            campfiresActivated = campfiresActivated,
            playersBuffed = playersBuffed,
            averageTickMs = average,
            maxTickMs = maxTickNanos.toDouble() / 1_000_000.0,
        )
    }
}
