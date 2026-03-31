package cg.headpop.campfireRPG.integration

data class ClanContext(
    val source: String,
    val id: String,
    val tag: String,
    val size: Int,
    val leader: Boolean,
    val role: String?,
)
