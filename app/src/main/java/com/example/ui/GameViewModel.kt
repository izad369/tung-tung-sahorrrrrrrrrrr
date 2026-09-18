package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.HorrorAudioEngine
import com.example.engine.HorrorRaycaster
import com.example.engine.MapDefinition
import com.example.engine.MonsterController
import com.example.model.GameScreenState
import com.example.model.InteractiveObject
import com.example.model.InteractiveType
import com.example.model.ItemType
import com.example.model.Language
import com.example.model.MonsterState
import com.example.model.PlayerState
import com.example.model.PuzzleState
import com.example.sensor.VRHeadTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class GameViewModel(application: Application) : AndroidViewModel(application) {
  val raycaster = HorrorRaycaster(application)
  val audioEngine = HorrorAudioEngine()
  val headTracker = VRHeadTracker(application)
  private val monsterController = MonsterController()

  private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
    vibratorManager?.defaultVibrator
  } else {
    @Suppress("DEPRECATION")
    application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
  }

  // Reactive UI States
  val player = PlayerState()
  val puzzleState = PuzzleState()

  private val _screenState = MutableStateFlow(GameScreenState.TITLE)
  val screenState: StateFlow<GameScreenState> = _screenState.asStateFlow()

  private val _language = MutableStateFlow(Language.FA)
  val language: StateFlow<Language> = _language.asStateFlow()

  private val _isVRMode = MutableStateFlow(false)
  val isVRMode: StateFlow<Boolean> = _isVRMode.asStateFlow()

  private val _inventory = MutableStateFlow<List<ItemType>>(emptyList())
  val inventory: StateFlow<List<ItemType>> = _inventory.asStateFlow()

  private val _currentPrompt = MutableStateFlow<String?>(null)
  val currentPrompt: StateFlow<String?> = _currentPrompt.asStateFlow()

  private val _targetObject = MutableStateFlow<InteractiveObject?>(null)
  val targetObject: StateFlow<InteractiveObject?> = _targetObject.asStateFlow()

  private val _activeNote = MutableStateFlow<ItemType?>(null)
  val activeNote: StateFlow<ItemType?> = _activeNote.asStateFlow()

  private val _interactiveObjects = MutableStateFlow<List<InteractiveObject>>(emptyList())
  val interactiveObjects: StateFlow<List<InteractiveObject>> = _interactiveObjects.asStateFlow()

  val monster = monsterController.monster

  // VR Gaze Progress
  private val _gazeProgress = MutableStateFlow(0f)
  val gazeProgress: StateFlow<Float> = _gazeProgress.asStateFlow()

  // Safe Puzzle Input State
  val safeInput = MutableStateFlow("0000")
  val safeError = MutableStateFlow(false)

  // Generator Puzzle Switch States (4 switches)
  val generatorSwitches = MutableStateFlow(listOf(false, false, false, false))

  // Game Loop Job
  private var gameLoopJob: Job? = null
  private var frameCount = 0L

  // Controls input
  var moveForward = 0f
  var moveStrafe = 0f
  var lookYawDelta = 0f
  var lookPitchDelta = 0f

  init {
    resetGame()
  }

  fun toggleLanguage() {
    _language.value = if (_language.value == Language.FA) Language.EN else Language.FA
  }

  fun toggleVRMode() {
    val newMode = !_isVRMode.value
    _isVRMode.value = newMode
    if (newMode) {
      headTracker.start()
      headTracker.calibrate(player.angle)
    } else {
      headTracker.stop()
    }
  }

  fun startGame() {
    _screenState.value = GameScreenState.STORY_INTRO
  }

  fun proceedToGameplay() {
    _screenState.value = GameScreenState.PLAYING
    audioEngine.start()
    startGameLoop()
  }

  fun resetGame() {
    player.x = 2.5f
    player.y = 2.5f
    player.angle = 0f
    player.pitch = 0f
    player.isCrouching = false
    player.isHiddenInWardrobe = false
    player.hiddenWardrobeId = null
    player.flashlightOn = true
    player.flashlightBattery = 100f
    player.sanity = 100f
    player.heartRate = 75

    monsterController.reset()
    _interactiveObjects.value = MapDefinition.getInitialObjects()
    _inventory.value = emptyList()

    puzzleState.cellarUnlocked = false
    puzzleState.studyUnlocked = false
    puzzleState.generatorDoorUnlocked = false
    puzzleState.safeUnlocked = false
    puzzleState.generatorPowerActive = false
    puzzleState.generatorSwitches = listOf(false, false, false, false)
    puzzleState.exitChainsCut = false
    puzzleState.exitMasterUnlocked = false
    puzzleState.exitPoweredOff = false
    puzzleState.escaped = false

    safeInput.value = "0000"
    generatorSwitches.value = listOf(false, false, false, false)
    _currentPrompt.value = null
    _targetObject.value = null
  }

  fun retryGame() {
    resetGame()
    _screenState.value = GameScreenState.PLAYING
    audioEngine.start()
    startGameLoop()
  }

  private fun startGameLoop() {
    gameLoopJob?.cancel()
    gameLoopJob = viewModelScope.launch {
      var lastTime = System.nanoTime()
      while (isActive && _screenState.value == GameScreenState.PLAYING) {
        val now = System.nanoTime()
        val dt = ((now - lastTime) / 1_000_000_000.0f).coerceIn(0.001f, 0.05f)
        lastTime = now

        update(dt)
        delay(16) // ~60 fps
      }
    }
  }

  private fun update(dt: Float) {
    frameCount++

    // 1. Process Head Tracking in VR Mode
    if (_isVRMode.value && headTracker.isTracking) {
      player.angle = headTracker.yaw
      player.pitch = headTracker.pitch
    } else {
      // Touch/Joystick look
      player.angle += lookYawDelta * dt * 2.2f
      player.pitch = (player.pitch + lookPitchDelta * dt * 1.5f).coerceIn(-0.6f, 0.6f)
      lookYawDelta = 0f
      lookPitchDelta = 0f
    }

    // 2. Player Movement (if not hidden inside wardrobe)
    if (!player.isHiddenInWardrobe) {
      val moveSpeed = if (player.isCrouching) 1.5f else 2.6f
      val forwardDist = moveForward * moveSpeed * dt
      val strafeDist = moveStrafe * moveSpeed * dt

      val dirX = cos(player.angle)
      val dirY = sin(player.angle)
      val strafeX = -dirY
      val strafeY = dirX

      val nextX = player.x + dirX * forwardDist + strafeX * strafeDist
      val nextY = player.y + dirY * forwardDist + strafeY * strafeDist

      // Collision check with map walls
      if (!MapDefinition.isSolid(nextX, player.y)) {
        player.x = nextX
      }
      if (!MapDefinition.isSolid(player.x, nextY)) {
        player.y = nextY
      }

      // Footstep sound when moving
      if ((forwardDist != 0f || strafeDist != 0f) && frameCount % (if (player.isCrouching) 30 else 18) == 0L) {
        audioEngine.playFootstep()
      }
    }

    // 3. Flashlight Battery Drain
    if (player.flashlightOn && player.flashlightBattery > 0f) {
      player.flashlightBattery = (player.flashlightBattery - dt * 0.4f).coerceAtLeast(0f)
    }

    // 4. Update Monster AI
    monsterController.update(
      dt,
      player,
      onJumpscare = {
        triggerJumpscare()
      },
      onChaseStart = {
        audioEngine.playMonsterRoar()
        vibrate(400)
      },
      onAlertSound = {
        vibrate(100)
      }
    )

    // 5. Update Heartbeat Audio and Fear effects based on monster proximity
    val dist = monster.distanceToPlayer
    val proximity = (1.0f - (dist / 15.0f)).coerceIn(0f, 1f)
    audioEngine.monsterProximity = proximity
    audioEngine.isMonsterChasing = monster.state == MonsterState.CHASE

    val targetHeartRate = when {
      monster.state == MonsterState.CHASE -> 160
      proximity > 0.6f -> 135
      proximity > 0.3f -> 100
      player.isHiddenInWardrobe -> 85
      else -> 72
    }
    player.heartRate = targetHeartRate
    audioEngine.heartbeatBpm = targetHeartRate.toFloat()

    // 6. Check closest interactive object
    checkInteractables(dt)
  }

  private fun checkInteractables(dt: Float) {
    if (player.isHiddenInWardrobe) {
      _currentPrompt.value = if (_language.value == Language.FA) "خروج از کمد" else "Exit Wardrobe"
      _targetObject.value = null
      return
    }

    var closestObj: InteractiveObject? = null
    var closestDist = 2.0f // interaction distance threshold

    for (obj in _interactiveObjects.value) {
      if (obj.isCollected) continue
      val dx = obj.x - player.x
      val dy = obj.y - player.y
      val dist = sqrt(dx * dx + dy * dy)
      if (dist < closestDist) {
        closestDist = dist
        closestObj = obj
      }
    }

    _targetObject.value = closestObj
    if (closestObj != null) {
      val label = if (_language.value == Language.FA) closestObj.labelFa else closestObj.labelEn
      _currentPrompt.value = label

      // In VR mode: gaze progress auto-interaction!
      if (_isVRMode.value) {
        _gazeProgress.value = (_gazeProgress.value + dt / 1.4f).coerceIn(0f, 1f)
        if (_gazeProgress.value >= 1.0f) {
          interact()
          _gazeProgress.value = 0f
        }
      }
    } else {
      _currentPrompt.value = null
      _gazeProgress.value = 0f
    }
  }

  fun interact() {
    audioEngine.playClick()

    // If inside wardrobe, step out!
    if (player.isHiddenInWardrobe) {
      player.isHiddenInWardrobe = false
      player.hiddenWardrobeId = null
      audioEngine.playDoorUnlock()
      return
    }

    val obj = _targetObject.value ?: return

    when (obj.type) {
      InteractiveType.ITEM_PICKUP -> {
        obj.itemToGrant?.let { item ->
          _inventory.value = _inventory.value + item
          obj.isCollected = true
          audioEngine.playClick()
          vibrate(80)
        }
      }

      InteractiveType.NOTE_PICKUP -> {
        obj.itemToGrant?.let { note ->
          if (!_inventory.value.contains(note)) {
            _inventory.value = _inventory.value + note
          }
          _activeNote.value = note
          _screenState.value = GameScreenState.NOTE_VIEW
        }
      }

      InteractiveType.WARDROBE -> {
        // Hide inside!
        player.isHiddenInWardrobe = true
        player.hiddenWardrobeId = obj.id
        audioEngine.playDoorUnlock()
        vibrate(150)
      }

      InteractiveType.CELLAR_DOOR -> {
        if (_inventory.value.contains(ItemType.CELLAR_KEY)) {
          obj.isUnlocked = true
          MapDefinition.setTile(4, 3, 0) // Open door passage
          audioEngine.playDoorUnlock()
          vibrate(200)
          obj.isCollected = true
        } else {
          showQuickMessage(
            if (_language.value == Language.FA) "درب قفل است! کلید زنگ‌زده زیرزمین را پیدا کنید."
            else "Door is locked! Find the Rusty Cellar Key."
          )
        }
      }

      InteractiveType.STUDY_DOOR -> {
        if (_inventory.value.contains(ItemType.STUDY_KEY)) {
          obj.isUnlocked = true
          MapDefinition.setTile(14, 5, 0) // Open door passage
          audioEngine.playDoorUnlock()
          vibrate(200)
          obj.isCollected = true
        } else {
          showQuickMessage(
            if (_language.value == Language.FA) "درب مطالعه قفل است! کلید روی میز راهرو را بیابید."
            else "Study is locked! Find the key in the corridor."
          )
        }
      }

      InteractiveType.GENERATOR_DOOR -> {
        obj.isUnlocked = true
        MapDefinition.setTile(3, 12, 0)
        audioEngine.playDoorUnlock()
        obj.isCollected = true
      }

      InteractiveType.SAFE_BOX -> {
        if (puzzleState.safeUnlocked) {
          showQuickMessage(
            if (_language.value == Language.FA) "گاوصندوق قبلاً باز شده است."
            else "Safe is already cracked."
          )
        } else {
          _screenState.value = GameScreenState.SAFE_PUZZLE
        }
      }

      InteractiveType.GENERATOR_PANEL -> {
        if (!_inventory.value.contains(ItemType.FUSE)) {
          showQuickMessage(
            if (_language.value == Language.FA) "ژنراتور نیاز به یک فیوز برق دارد!"
            else "Generator is missing a Fuse!"
          )
        } else {
          _screenState.value = GameScreenState.GENERATOR_PUZZLE
        }
      }

      InteractiveType.EXIT_DOOR -> {
        handleExitDoorInteraction()
      }
    }
  }

  private fun handleExitDoorInteraction() {
    // 1. Cut chains with bolt cutters
    if (!puzzleState.exitChainsCut) {
      if (_inventory.value.contains(ItemType.BOLT_CUTTERS)) {
        puzzleState.exitChainsCut = true
        audioEngine.playDoorUnlock()
        vibrate(300)
        showQuickMessage(
          if (_language.value == Language.FA) "زنجیرهای سنگین بریده شدند!"
          else "Heavy chains cut with Bolt Cutters!"
        )
      } else {
        showQuickMessage(
          if (_language.value == Language.FA) "درب با زنجیرهای ضخیم بسته شده! نیاز به قیچی آهن‌بر دارید (درون گاوصندوق مطالعه)."
          else "Door chained shut! Need Bolt Cutters from study safe."
        )
      }
      return
    }

    // 2. Master Key
    if (!puzzleState.exitMasterUnlocked) {
      if (_inventory.value.contains(ItemType.MASTER_KEY)) {
        puzzleState.exitMasterUnlocked = true
        audioEngine.playDoorUnlock()
        vibrate(300)
        showQuickMessage(
          if (_language.value == Language.FA) "قفل اصلی با کلید طلایی باز شد!"
          else "Main lock opened with Golden Master Key!"
        )
      } else {
        showQuickMessage(
          if (_language.value == Language.FA) "قفل طلایی عمارت بسته است! کلید طلایی را در اتاق خواب اصلی بیابید."
          else "Main lock requires Golden Master Key from master bedroom."
        )
      }
      return
    }

    // 3. Generator power check
    if (!puzzleState.generatorPowerActive) {
      showQuickMessage(
        if (_language.value == Language.FA) "قفل الکترومغناطیسی فعال است! برق ژنراتور را فعال کنید."
        else "Electromagnetic lock is active! Power the generator."
      )
      return
    }

    // WIN CONDITION: Escaped!
    puzzleState.escaped = true
    _screenState.value = GameScreenState.VICTORY
    audioEngine.playDoorUnlock()
  }

  fun submitSafeCode(code: String) {
    if (code == "1984") {
      puzzleState.safeUnlocked = true
      _inventory.value = _inventory.value + ItemType.BOLT_CUTTERS
      audioEngine.playDoorUnlock()
      vibrate(400)
      _screenState.value = GameScreenState.PLAYING
      showQuickMessage(
        if (_language.value == Language.FA) "گاوصندوق باز شد! قیچی آهن‌بر دریافت شد."
        else "Safe unlocked! Acquired Bolt Cutters."
      )
    } else {
      safeError.value = true
      audioEngine.playClick()
      vibrate(100)
      viewModelScope.launch {
        delay(1200)
        safeError.value = false
      }
    }
  }

  fun toggleGeneratorSwitch(index: Int) {
    audioEngine.playClick()
    val current = generatorSwitches.value.toMutableList()
    current[index] = !current[index]
    generatorSwitches.value = current

    // Sequence check: Ground (Switch 1 / Green) -> Power (Switch 3 / Yellow) -> Filter (Switch 2 / Blue) -> Core (Switch 0 / Red)
    // All 4 switches turned on successfully completes circuit!
    if (current.all { it }) {
      puzzleState.generatorPowerActive = true
      audioEngine.playMonsterRoar() // generator kicks to life
      vibrate(500)
      _screenState.value = GameScreenState.PLAYING
      showQuickMessage(
        if (_language.value == Language.FA) "ژنراتور روشن شد! قفل مغناطیسی درب خروج از کار افتاد."
        else "Generator active! Electronic lock disabled."
      )
    }
  }

  fun toggleFlashlight() {
    player.flashlightOn = !player.flashlightOn
    audioEngine.playClick()
    vibrate(40)
  }

  fun toggleCrouch() {
    player.isCrouching = !player.isCrouching
    vibrate(30)
  }

  fun closeDialog() {
    _screenState.value = GameScreenState.PLAYING
  }

  private fun triggerJumpscare() {
    _screenState.value = GameScreenState.JUMPSCARE_DEATH
    audioEngine.playJumpscare()
    vibrate(1200)
  }

  private fun vibrate(durationMs: Long) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(durationMs)
      }
    } catch (e: Exception) {
      // ignore
    }
  }

  private fun showQuickMessage(msg: String) {
    _currentPrompt.value = msg
  }

  override fun onCleared() {
    super.onCleared()
    gameLoopJob?.cancel()
    audioEngine.stop()
    headTracker.stop()
  }
}
