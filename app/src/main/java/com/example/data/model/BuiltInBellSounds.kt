package com.example.data.model

data class BellSoundOption(
    val id: String,
    val name: String,
    val description: String,
    val isCustom: Boolean = false,
    val localFilePath: String? = null,
    val durationLabel: String? = null,
)

object BuiltInBellSounds {
    const val BUILTIN_BRIGHT_START = "builtin_bright_start"
    const val BUILTIN_WARM_END = "builtin_warm_end"
    const val BUILTIN_CLASSIC_4 = "builtin_classic_4"
    const val BUILTIN_WESTMINSTER = "builtin_westminster"
    const val SYSTEM_ALARM = "system_alarm"

    val presets = listOf(
        BellSoundOption(
            id = BUILTIN_BRIGHT_START,
            name = "경쾌한 시작 차임벨",
            description = "맑고 밝게 올라가는 4음 차임벨 (시작종 추천)",
            durationLabel = "3초",
        ),
        BellSoundOption(
            id = BUILTIN_WARM_END,
            name = "차분한 종료 차임벨",
            description = "부드럽게 마무리되는 하강 화음 (종료종 추천)",
            durationLabel = "3초",
        ),
        BellSoundOption(
            id = BUILTIN_CLASSIC_4,
            name = "클래식 학원 딩동댕",
            description = "익숙하고 선명한 도-미-솔-도 4음 종소리",
            durationLabel = "3.5초",
        ),
        BellSoundOption(
            id = BUILTIN_WESTMINSTER,
            name = "웨스트민스터 종소리",
            description = "전통 학교·강당용 8음 차임벨",
            durationLabel = "5.5초",
        ),
        BellSoundOption(
            id = SYSTEM_ALARM,
            name = "기기 기본 알람음",
            description = "안드로이드 기기에 설정된 기본 알람 벨소리",
            durationLabel = "기본",
        ),
    )

    fun findPresetName(soundId: String, customSounds: List<CustomBellSoundEntity>): String {
        presets.firstOrNull { it.id == soundId }?.let { return it.name }
        customSounds.firstOrNull { it.id == soundId }?.let { return it.displayName }
        return "경쾌한 시작 차임벨"
    }
}
