package com.example.model

enum class ItemType(
  val id: String,
  val nameEn: String,
  val nameFa: String,
  val descEn: String,
  val descFa: String,
  val icon: String
) {
  FLASHLIGHT(
    "flashlight",
    "Flashlight",
    "چراغ‌قوه",
    "Essential to see in the pitch darkness. Keep battery in mind.",
    "برای دیدن در تاریکی مطلق ضروری است. مراقب باتری باشید.",
    "🔦"
  ),
  CELLAR_KEY(
    "cellar_key",
    "Rusty Cellar Key",
    "کلید زنگ‌زده زیرزمین",
    "Opens the heavy wooden cellar door to the ground floor.",
    "درب سنگین چوبی زیرزمین را به طبقه بالا باز می‌کند.",
    "🗝️"
  ),
  STUDY_KEY(
    "study_key",
    "Study Key",
    "کلید اتاق مطالعه",
    "Unlocks the master study room.",
    "درب اتاق مطالعه را باز می‌کند.",
    "🔑"
  ),
  BOLT_CUTTERS(
    "bolt_cutters",
    "Bolt Cutters",
    "قیچی آهن‌بر",
    "Found inside the study safe. Cuts the heavy chains on the exit door.",
    "درون گاوصندوق پیدا شد. زنجیرهای ضخیم درب خروج را می‌برد.",
    "✂️"
  ),
  MASTER_KEY(
    "master_key",
    "Golden Master Key",
    "کلید طلایی عمارت",
    "Unlocks the final deadbolt on the grand front exit door.",
    "قفل اصلی درب ورودی عمارت را باز می‌کند.",
    "🗝️"
  ),
  FUSE(
    "fuse",
    "Electrical Fuse",
    "فیوز برق",
    "Needed to restore power to the electromagnetic exit door lock.",
    "برای راه‌اندازی ژنراتور و قطع قفل مغناطیسی درب خروج نیاز است.",
    "⚡"
  ),
  NOTE_CREATURE(
    "note_creature",
    "Bloody Warning",
    "هشدار خونین",
    "A note written in blood about the roaming entity.",
    "دست‌نوشته‌ای با خون درباره هیولای پرسه زن.",
    "📜"
  ),
  NOTE_SAFE(
    "note_safe",
    "Diary Page",
    "برگه دفترچه خاطرات",
    "Hints at the combination lock for the study safe.",
    "سرنخی درباره رمز گاوصندوق اتاق مطالعه.",
    "📜"
  ),
  NOTE_EXIT(
    "note_exit",
    "Architect's Plan",
    "نقشه خروج",
    "Describes how to bypass the 3 security layers of the main door.",
    "نحوه غیرفعال کردن ۳ قفل درب اصلی خروج را توضیح می‌دهد.",
    "📜"
  )
}

enum class MonsterState {
  PATROL,
  ALERT,
  CHASE,
  SEARCHING,
  JUMPSCARE
}

enum class InteractiveType {
  CELLAR_DOOR,
  STUDY_DOOR,
  GENERATOR_DOOR,
  EXIT_DOOR,
  WARDROBE,
  SAFE_BOX,
  GENERATOR_PANEL,
  ITEM_PICKUP,
  NOTE_PICKUP
}

data class InteractiveObject(
  val id: String,
  val type: InteractiveType,
  val x: Float,
  val y: Float,
  val requiredItem: ItemType? = null,
  val itemToGrant: ItemType? = null,
  var isUnlocked: Boolean = false,
  var isOpen: Boolean = false,
  var isCollected: Boolean = false,
  val labelEn: String,
  val labelFa: String
)

data class PlayerState(
  var x: Float = 2.5f,
  var y: Float = 2.5f,
  var angle: Float = 0f,
  var pitch: Float = 0f,
  var isCrouching: Boolean = false,
  var isHiddenInWardrobe: Boolean = false,
  var hiddenWardrobeId: String? = null,
  var flashlightOn: Boolean = true,
  var flashlightBattery: Float = 100f,
  var sanity: Float = 100f,
  var heartRate: Int = 75,
  var isRunning: Boolean = false
)

data class MonsterData(
  var x: Float = 12.5f,
  var y: Float = 12.5f,
  var angle: Float = 0f,
  var state: MonsterState = MonsterState.PATROL,
  var speed: Float = 1.8f,
  var targetX: Float = 12.5f,
  var targetY: Float = 12.5f,
  var distanceToPlayer: Float = 20f,
  var lastKnownPlayerX: Float = 0f,
  var lastKnownPlayerY: Float = 0f,
  var alertLevel: Float = 0f, // 0 to 1
  var searchTimer: Float = 0f
)

data class PuzzleState(
  var cellarUnlocked: Boolean = false,
  var studyUnlocked: Boolean = false,
  var generatorDoorUnlocked: Boolean = false,
  var safeUnlocked: Boolean = false,
  var generatorPowerActive: Boolean = false,
  var generatorSwitches: List<Boolean> = listOf(false, false, false, false),
  var exitChainsCut: Boolean = false,
  var exitMasterUnlocked: Boolean = false,
  var exitPoweredOff: Boolean = false,
  var escaped: Boolean = false
)

enum class GameScreenState {
  TITLE,
  STORY_INTRO,
  PLAYING,
  SAFE_PUZZLE,
  GENERATOR_PUZZLE,
  NOTE_VIEW,
  JUMPSCARE_DEATH,
  VICTORY
}

enum class Language {
  FA,
  EN
}
