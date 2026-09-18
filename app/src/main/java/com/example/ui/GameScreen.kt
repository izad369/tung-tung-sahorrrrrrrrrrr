package com.example.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.engine.MapDefinition
import com.example.model.GameScreenState
import com.example.model.ItemType
import com.example.model.Language
import com.example.model.MonsterState
import com.example.ui.theme.BloodRed
import com.example.ui.theme.BoneWhite
import com.example.ui.theme.Crimson
import com.example.ui.theme.DarkBlood
import com.example.ui.theme.DecayGreen
import com.example.ui.theme.FlashlightAmber
import com.example.ui.theme.HorrorBlack
import com.example.ui.theme.HorrorCardSurface
import com.example.ui.theme.HorrorDarkSurface
import com.example.ui.theme.MutedGrey
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun MainHorrorGameView(viewModel: GameViewModel) {
  val screenState by viewModel.screenState.collectAsState()
  val language by viewModel.language.collectAsState()

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(HorrorBlack)
  ) {
    when (screenState) {
      GameScreenState.TITLE -> TitleScreen(viewModel, language)
      GameScreenState.STORY_INTRO -> StoryIntroScreen(viewModel, language)
      GameScreenState.PLAYING -> GameViewport(viewModel, language)
      GameScreenState.SAFE_PUZZLE -> {
        GameViewport(viewModel, language)
        SafePuzzleDialog(viewModel, language)
      }
      GameScreenState.GENERATOR_PUZZLE -> {
        GameViewport(viewModel, language)
        GeneratorPuzzleDialog(viewModel, language)
      }
      GameScreenState.NOTE_VIEW -> {
        GameViewport(viewModel, language)
        NoteViewDialog(viewModel, language)
      }
      GameScreenState.JUMPSCARE_DEATH -> GameOverJumpscareScreen(viewModel, language)
      GameScreenState.VICTORY -> VictoryEscapeScreen(viewModel, language)
    }
  }
}

@Composable
fun TitleScreen(viewModel: GameViewModel, language: Language) {
  val isVRMode by viewModel.isVRMode.collectAsState()
  val infiniteTransition = rememberInfiniteTransition(label = "title")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.5f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
    label = "pulse"
  )

  Box(modifier = Modifier.fillMaxSize()) {
    // Atmospheric Background banner
    Image(
      painter = painterResource(id = R.drawable.horror_banner),
      contentDescription = "Haunted Mansion",
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize()
    )

    // Dark red atmospheric overlay gradient
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(
              Color(0xBB0A0002),
              Color(0xDD0D0406),
              Color(0xFB140408)
            )
          )
        )
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Top Bar: Language and Audio
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = { viewModel.toggleLanguage() },
          modifier = Modifier
            .background(HorrorDarkSurface.copy(alpha = 0.8f), CircleShape)
            .testTag("lang_toggle")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
          ) {
            Icon(Icons.Default.Language, contentDescription = "Language", tint = BoneWhite)
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (language == Language.FA) "FA" else "EN", color = BoneWhite, fontSize = 13.sp)
          }
        }

        IconButton(
          onClick = { viewModel.audioEngine.isMuted = !viewModel.audioEngine.isMuted },
          modifier = Modifier
            .background(HorrorDarkSurface.copy(alpha = 0.8f), CircleShape)
            .testTag("mute_toggle")
        ) {
          Icon(
            if (viewModel.audioEngine.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
            contentDescription = "Mute",
            tint = BoneWhite
          )
        }
      }

      // Title & Lore
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = if (language == Language.FA) "فـرار از خـانـه وحـشـت ۳D" else "HORROR HOUSE 3D",
          fontSize = 32.sp,
          fontWeight = FontWeight.Black,
          color = BloodRed.copy(alpha = pulseAlpha),
          textAlign = TextAlign.Center,
          letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = if (language == Language.FA) "واقعیت مجازی (VR) • هیولا • حل معماها" else "Virtual Reality (VR) • Roaming Creature • Puzzles",
          fontSize = 14.sp,
          color = FlashlightAmber,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
          colors = CardDefaults.cardColors(containerColor = HorrorDarkSurface.copy(alpha = 0.85f)),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Crimson.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            val f1 = if (language == Language.FA) "👁️ هیولای ترسناک در خانه پرسه می‌زند؛ وقتی نزدیک شد در کمد پنهان شوید!" else "👁️ Terrifying monster roams the halls; hide in wardrobes when near!"
            val f2 = if (language == Language.FA) "🧩 معماهای گاوصندوق، فیوز ژنراتور و کلیدها را برای فرار حل کنید." else "🧩 Solve safe combinations, generator circuits & find keys to escape."
            val f3 = if (language == Language.FA) "🥽 حالت واقعیت مجازی (VR) با ژیروسکوپ سر و عینک‌های کاردبورد." else "🥽 Stereoscopic VR Mode with 360° head-tracking for VR headsets."

            Text(text = f1, color = BoneWhite, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
            Text(text = f2, color = BoneWhite, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
            Text(text = f3, color = BoneWhite, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
          }
        }
      }

      // Bottom Buttons
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // VR Mode Toggle
        Button(
          onClick = { viewModel.toggleVRMode() },
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isVRMode) DecayGreen else HorrorCardSurface
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .border(1.dp, if (isVRMode) DecayGreen else Crimson, RoundedCornerShape(12.dp))
            .testTag("vr_mode_toggle")
        ) {
          Icon(Icons.Default.Visibility, contentDescription = null, tint = BoneWhite)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (isVRMode) {
              if (language == Language.FA) "حالت VR فعال است (استریوسکوپیک)" else "VR Mode ACTIVE (Stereoscopic)"
            } else {
              if (language == Language.FA) "فعال‌سازی حالت واقعیت مجازی (VR)" else "Enable Virtual Reality (VR) Mode"
            },
            fontWeight = FontWeight.Bold,
            color = BoneWhite
          )
        }

        // Start Game
        Button(
          onClick = { viewModel.startGame() },
          colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("start_game_button")
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, tint = BoneWhite)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (language == Language.FA) "شروع بازی وحشت" else "START HORROR GAME",
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            color = BoneWhite,
            letterSpacing = 1.sp
          )
        }
      }
    }
  }
}

@Composable
fun StoryIntroScreen(viewModel: GameViewModel, language: Language) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(HorrorBlack)
      .padding(28.dp),
    contentAlignment = Alignment.Center
  ) {
    Card(
      colors = CardDefaults.cardColors(containerColor = HorrorDarkSurface),
      shape = RoundedCornerShape(20.dp),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.5.dp, BloodRed, RoundedCornerShape(20.dp))
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = if (language == Language.FA) "کابوس آغاز می‌شود..." else "The Nightmare Begins...",
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = BloodRed,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = if (language == Language.FA) {
            "در تاریکی سرد یک زیرزمین متروکه به هوش آمده‌ای. درهای خانه قفل است و سایه‌ای خوفناک در راهروها در کمین توست.\n\n" +
            "• صدای پای سنگین او را بشنو و در کمدها مخفی شو.\n" +
            "• معماها را حل کن: کلید زیرزمین، رمز گاوصندوق مطالعه، و فیوز ژنراتور.\n" +
            "• با قطع زنجیرها و باز کردن قفل‌ها، از خانه فرار کن!"
          } else {
            "You awaken trapped inside a decrepit Victorian mansion. Roaming footsteps and guttural breathing echo in the dark corridors.\n\n" +
            "• Listen to its footsteps and HIDE inside wardrobes when it hunts you.\n" +
            "• Solve the puzzles: Cellar Key, Study Safe Code, and Generator Circuit.\n" +
            "• Cut the exit chains, disable the magnetic lock, and ESCAPE!"
          },
          fontSize = 14.sp,
          lineHeight = 22.sp,
          color = BoneWhite
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
          onClick = { viewModel.proceedToGameplay() },
          colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("enter_house_button")
        ) {
          Text(
            text = if (language == Language.FA) "ورود به خانه (بقا)" else "ENTER THE HOUSE",
            fontWeight = FontWeight.Bold,
            color = BoneWhite
          )
        }
      }
    }
  }
}

@Composable
fun GameViewport(viewModel: GameViewModel, language: Language) {
  val isVRMode by viewModel.isVRMode.collectAsState()
  val inventory by viewModel.inventory.collectAsState()
  val currentPrompt by viewModel.currentPrompt.collectAsState()
  val gazeProgress by viewModel.gazeProgress.collectAsState()
  val interactiveObjects by viewModel.interactiveObjects.collectAsState()

  // Frame tick state to trigger Canvas redraw
  var frameTick by remember { mutableStateOf(0L) }
  LaunchedEffect(Unit) {
    while (true) {
      frameTick++
      delay(16)
    }
  }

  // Render 3D Raycaster frame into bitmap
  val renderedBitmap = remember(frameTick) {
    viewModel.raycaster.render(
      viewModel.player,
      viewModel.monster,
      interactiveObjects,
      isVRMode
    )
  }

  Box(modifier = Modifier.fillMaxSize()) {
    // 1. The 3D Render Canvas
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(isVRMode) {
          if (!isVRMode) {
            detectDragGestures { change, dragAmount ->
              change.consume()
              // Right side drag = look camera
              if (change.position.x > size.width * 0.45f) {
                viewModel.lookYawDelta += dragAmount.x * 0.008f
                viewModel.lookPitchDelta += dragAmount.y * 0.008f
              }
            }
          }
        }
    ) {
      val bmp = renderedBitmap.asImageBitmap()
      drawImage(
        image = bmp,
        dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
      )
    }

    // 2. Wardrobe Hiding Shutter View Overlay
    if (viewModel.player.isHiddenInWardrobe) {
      WardrobeSlatsOverlay(language) {
        viewModel.interact()
      }
    }

    // 3. Heartbeat & Terror Red Vignette
    TerrorVignette(
      heartRate = viewModel.player.heartRate,
      isChasing = viewModel.monster.state == MonsterState.CHASE,
      distance = viewModel.monster.distanceToPlayer
    )

    // 4. VR Mode Overlays vs Normal HUD
    if (isVRMode) {
      VRModeHud(viewModel, gazeProgress, language)
    } else {
      NormalModeHud(viewModel, inventory, currentPrompt, language)
    }
  }
}

@Composable
fun WardrobeSlatsOverlay(language: Language, onExit: () -> Unit) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xCC000000))
  ) {
    // Wooden slats slats effect
    Canvas(modifier = Modifier.fillMaxSize()) {
      val slatHeight = 40.dp.toPx()
      val slitHeight = 12.dp.toPx()
      var y = 0f
      while (y < size.height) {
        drawRect(
          color = Color(0xF0180F0A),
          topLeft = Offset(0f, y),
          size = Size(size.width, slatHeight)
        )
        y += slatHeight + slitHeight
      }
    }

    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = if (language == Language.FA) "پنهان در کمد • ساکت بمانید..." else "HIDDEN IN WARDROBE • Stay Quiet...",
        color = FlashlightAmber,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(10.dp))
      Button(
        onClick = onExit,
        colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.testTag("exit_wardrobe_button")
      ) {
        Text(if (language == Language.FA) "خروج از کمد" else "Exit Wardrobe", color = BoneWhite)
      }
    }
  }
}

@Composable
fun TerrorVignette(heartRate: Int, isChasing: Boolean, distance: Float) {
  val proximity = (1.0f - distance / 14f).coerceIn(0f, 1f)
  val pulseTransition = rememberInfiniteTransition(label = "heartbeat_vignette")
  val pulseDuration = (60_000 / heartRate.coerceIn(60, 180))

  val pulseAlpha by pulseTransition.animateFloat(
    initialValue = 0.1f,
    targetValue = (0.25f + proximity * 0.5f).coerceIn(0.1f, 0.85f),
    animationSpec = infiniteRepeatable(tween(pulseDuration / 2), RepeatMode.Reverse),
    label = "vignette_pulse"
  )

  if (proximity > 0.2f || isChasing) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.radialGradient(
            listOf(
              Color.Transparent,
              BloodRed.copy(alpha = pulseAlpha * 0.4f),
              DarkBlood.copy(alpha = pulseAlpha)
            )
          )
        )
    )
  }
}

@Composable
fun NormalModeHud(
  viewModel: GameViewModel,
  inventory: List<ItemType>,
  currentPrompt: String?,
  language: Language
) {
  var showMap by remember { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxSize()) {
    // Top Bar: Heart Rate, Flashlight, Map, VR Toggle
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Heart rate monitor
      Card(
        colors = CardDefaults.cardColors(containerColor = HorrorDarkSurface.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(1.dp, Crimson.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = "❤️", fontSize = 16.sp)
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "${viewModel.player.heartRate} BPM",
            color = if (viewModel.player.heartRate > 120) BloodRed else BoneWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Map Button
        IconButton(
          onClick = { showMap = !showMap },
          modifier = Modifier
            .background(HorrorDarkSurface.copy(alpha = 0.8f), CircleShape)
            .testTag("toggle_map_button")
        ) {
          Icon(Icons.Default.Map, contentDescription = "Map", tint = BoneWhite)
        }

        // VR Toggle Button
        IconButton(
          onClick = { viewModel.toggleVRMode() },
          modifier = Modifier
            .background(HorrorDarkSurface.copy(alpha = 0.8f), CircleShape)
            .testTag("hud_vr_toggle")
        ) {
          Icon(Icons.Default.Visibility, contentDescription = "VR Mode", tint = DecayGreen)
        }

        // Flashlight Toggle
        IconButton(
          onClick = { viewModel.toggleFlashlight() },
          modifier = Modifier
            .background(
              if (viewModel.player.flashlightOn) FlashlightAmber.copy(alpha = 0.85f) else HorrorDarkSurface.copy(alpha = 0.8f),
              CircleShape
            )
            .testTag("flashlight_toggle")
        ) {
          Icon(
            if (viewModel.player.flashlightOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
            contentDescription = "Flashlight",
            tint = if (viewModel.player.flashlightOn) HorrorBlack else BoneWhite
          )
        }
      }
    }

    // Interactive Action Prompt Banner
    if (currentPrompt != null) {
      Box(
        modifier = Modifier
          .align(Alignment.Center)
          .offset(y = 60.dp)
      ) {
        Button(
          onClick = { viewModel.interact() },
          colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier
            .border(1.5.dp, BoneWhite, RoundedCornerShape(20.dp))
            .testTag("action_interact_button")
        ) {
          Icon(Icons.Default.TouchApp, contentDescription = null, tint = BoneWhite)
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = currentPrompt, fontWeight = FontWeight.Bold, color = BoneWhite)
        }
      }
    }

    // Bottom Controls: Virtual Joystick & Action Buttons & Inventory
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
        .padding(16.dp)
    ) {
      // Inventory Quickbar
      if (inventory.isNotEmpty()) {
        Card(
          colors = CardDefaults.cardColors(containerColor = HorrorDarkSurface.copy(alpha = 0.85f)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .border(1.dp, Crimson.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        ) {
          Row(
            modifier = Modifier
              .padding(8.dp)
              .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (language == Language.FA) "کوله:" else "Items:",
              fontSize = 12.sp,
              color = FlashlightAmber,
              fontWeight = FontWeight.Bold
            )
            inventory.forEach { item ->
              Box(
                modifier = Modifier
                  .background(HorrorCardSurface, RoundedCornerShape(8.dp))
                  .border(1.dp, Crimson, RoundedCornerShape(8.dp))
                  .clickable {
                    if (item.id.startsWith("note_")) {
                      viewModel.interact()
                    }
                  }
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(text = item.icon, fontSize = 16.sp)
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (language == Language.FA) item.nameFa else item.nameEn,
                    fontSize = 11.sp,
                    color = BoneWhite
                  )
                }
              }
            }
          }
        }
      }

      // Movement Pad (Left) and Crouch Button (Right)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
      ) {
        // Virtual Analog Joystick
        VirtualJoystick(
          onMove = { fwd, strf ->
            viewModel.moveForward = fwd
            viewModel.moveStrafe = strf
          }
        )

        // Crouch / Sneak Toggle
        Button(
          onClick = { viewModel.toggleCrouch() },
          colors = ButtonDefaults.buttonColors(
            containerColor = if (viewModel.player.isCrouching) DecayGreen else HorrorDarkSurface
          ),
          shape = CircleShape,
          modifier = Modifier
            .size(60.dp)
            .border(1.dp, if (viewModel.player.isCrouching) DecayGreen else Crimson, CircleShape)
            .testTag("crouch_toggle")
        ) {
          Icon(
            Icons.Default.DirectionsRun,
            contentDescription = "Crouch",
            tint = BoneWhite
          )
        }
      }
    }

    // Mini-map Modal
    if (showMap) {
      MiniMapDialog(viewModel, onDismiss = { showMap = false }, language = language)
    }
  }
}

@Composable
fun VirtualJoystick(onMove: (forward: Float, strafe: Float) -> Unit) {
  var knobOffset by remember { mutableStateOf(Offset.Zero) }
  val radius = 55.dp

  Box(
    modifier = Modifier
      .size(110.dp)
      .background(HorrorDarkSurface.copy(alpha = 0.75f), CircleShape)
      .border(1.5.dp, Crimson.copy(alpha = 0.7f), CircleShape)
      .pointerInput(Unit) {
        detectDragGestures(
          onDragEnd = {
            knobOffset = Offset.Zero
            onMove(0f, 0f)
          },
          onDragCancel = {
            knobOffset = Offset.Zero
            onMove(0f, 0f)
          }
        ) { change, dragAmount ->
          change.consume()
          val newOffset = knobOffset + dragAmount
          val dist = sqrt(newOffset.x * newOffset.x + newOffset.y * newOffset.y)
          val maxDist = radius.toPx()
          knobOffset = if (dist > maxDist) {
            Offset(newOffset.x / dist * maxDist, newOffset.y / dist * maxDist)
          } else {
            newOffset
          }
          // Y negative = forward, X positive = strafe right
          val forward = -knobOffset.y / maxDist
          val strafe = knobOffset.x / maxDist
          onMove(forward, strafe)
        }
      },
    contentAlignment = Alignment.Center
  ) {
    Box(
      modifier = Modifier
        .offset { IntOffset(knobOffset.x.toInt(), knobOffset.y.toInt()) }
        .size(44.dp)
        .background(BloodRed, CircleShape)
        .border(1.dp, BoneWhite, CircleShape)
    )
  }
}

@Composable
fun VRModeHud(viewModel: GameViewModel, gazeProgress: Float, language: Language) {
  Box(modifier = Modifier.fillMaxSize()) {
    // Center divider info in VR
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Left Eye HUD
      VRPeripheralInfo(viewModel, gazeProgress, language)
      // Right Eye HUD
      VRPeripheralInfo(viewModel, gazeProgress, language)
    }

    // Exit VR Button at top right
    Button(
      onClick = { viewModel.toggleVRMode() },
      colors = ButtonDefaults.buttonColors(containerColor = HorrorDarkSurface.copy(alpha = 0.8f)),
      shape = RoundedCornerShape(8.dp),
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(top = 8.dp)
        .testTag("exit_vr_mode_button")
    ) {
      Text(if (language == Language.FA) "خروج از VR" else "Exit VR", fontSize = 11.sp, color = BoneWhite)
    }
  }
}

@Composable
fun VRPeripheralInfo(viewModel: GameViewModel, gazeProgress: Float, language: Language) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    if (gazeProgress > 0f) {
      Card(
        colors = CardDefaults.cardColors(containerColor = BloodRed.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text(
          text = if (language == Language.FA) "نگاه مداوم: ${(gazeProgress * 100).toInt()}%" else "Gazing: ${(gazeProgress * 100).toInt()}%",
          fontSize = 10.sp,
          color = BoneWhite,
          modifier = Modifier.padding(4.dp)
        )
      }
    }
    Text(
      text = "❤️ ${viewModel.player.heartRate} BPM",
      fontSize = 11.sp,
      color = if (viewModel.player.heartRate > 110) BloodRed else BoneWhite,
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
fun MiniMapDialog(viewModel: GameViewModel, onDismiss: () -> Unit, language: Language) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
      colors = CardDefaults.cardColors(containerColor = HorrorDarkSurface),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, Crimson, RoundedCornerShape(16.dp))
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (language == Language.FA) "نقشه عمارت" else "Mansion Floor Plan",
            fontWeight = FontWeight.Bold,
            color = FlashlightAmber
          )
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = BoneWhite)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Draw 20x20 Grid Map
        Canvas(
          modifier = Modifier
            .size(240.dp)
            .background(Color.Black, RoundedCornerShape(8.dp))
            .border(1.dp, MutedGrey, RoundedCornerShape(8.dp))
        ) {
          val cellSize = size.width / MapDefinition.MAP_WIDTH
          for (y in 0 until MapDefinition.MAP_HEIGHT) {
            for (x in 0 until MapDefinition.MAP_WIDTH) {
              val tile = MapDefinition.getTile(x, y)
              if (tile > 0) {
                val color = when (tile) {
                  10, 11, 12, 13 -> DecayGreen // Doors
                  else -> Color(0xFF4A3E40) // Walls
                }
                drawRect(
                  color = color,
                  topLeft = Offset(x * cellSize, y * cellSize),
                  size = Size(cellSize, cellSize)
                )
              }
            }
          }

          // Draw Player Position & Direction
          val px = viewModel.player.x * cellSize
          val py = viewModel.player.y * cellSize
          drawCircle(color = FlashlightAmber, radius = cellSize * 0.7f, center = Offset(px, py))
          drawLine(
            color = FlashlightAmber,
            start = Offset(px, py),
            end = Offset(
              px + cos(viewModel.player.angle) * cellSize * 2.2f,
              py + sin(viewModel.player.angle) * cellSize * 2.2f
            ),
            strokeWidth = 3f
          )

          // Draw Monster (if nearby or detected)
          val mx = viewModel.monster.x * cellSize
          val my = viewModel.monster.y * cellSize
          drawCircle(color = BloodRed, radius = cellSize * 0.65f, center = Offset(mx, my))
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = if (language == Language.FA) "نقطه زرد: شما • نقطه قرمز: هیولا" else "Yellow: You • Red: Monster",
          fontSize = 11.sp,
          color = BoneWhite
        )
      }
    }
  }
}

@Composable
fun SafePuzzleDialog(viewModel: GameViewModel, language: Language) {
  var digits by remember { mutableStateOf(listOf(0, 0, 0, 0)) }
  val isError by viewModel.safeError.collectAsState()

  Dialog(onDismissRequest = { viewModel.closeDialog() }) {
    Card(
      colors = CardDefaults.cardColors(containerColor = HorrorDarkSurface),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.5.dp, BloodRed, RoundedCornerShape(16.dp))
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = if (language == Language.FA) "گاوصندوق عتیقه اتاق مطالعه" else "Antique Study Safe",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = FlashlightAmber
        )
        Text(
          text = if (language == Language.FA) "رمز ۴ رقمی را وارد کنید (سرنخ: دفترچه خاطرات)" else "Enter 4-digit code (Hint: Diary note)",
          fontSize = 12.sp,
          color = BoneWhite
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 4 Digital Dials
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          digits.forEachIndexed { index, digit ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Button(
                onClick = {
                  val newD = digits.toMutableList()
                  newD[index] = (digit + 1) % 10
                  digits = newD
                },
                colors = ButtonDefaults.buttonColors(containerColor = HorrorCardSurface),
                modifier = Modifier.size(44.dp)
              ) {
                Text("▲", color = BoneWhite, fontSize = 12.sp)
              }

              Card(
                colors = CardDefaults.cardColors(
                  containerColor = if (isError) DarkBlood else Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                  .size(52.dp)
                  .padding(vertical = 4.dp)
                  .border(1.dp, FlashlightAmber, RoundedCornerShape(8.dp))
              ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  Text(
                    text = digit.toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isError) BloodRed else FlashlightAmber
                  )
                }
              }

              Button(
                onClick = {
                  val newD = digits.toMutableList()
                  newD[index] = if (digit == 0) 9 else digit - 1
                  digits = newD
                },
                colors = ButtonDefaults.buttonColors(containerColor = HorrorCardSurface),
                modifier = Modifier.size(44.dp)
              ) {
                Text("▼", color = BoneWhite, fontSize = 12.sp)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = { viewModel.closeDialog() },
            colors = ButtonDefaults.buttonColors(containerColor = HorrorCardSurface),
            modifier = Modifier.weight(1f)
          ) {
            Text(if (language == Language.FA) "انصراف" else "Cancel", color = BoneWhite)
          }

          Button(
            onClick = {
              val code = digits.joinToString("")
              viewModel.submitSafeCode(code)
            },
            colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
            modifier = Modifier
              .weight(1f)
              .testTag("submit_safe_code")
          ) {
            Text(if (language == Language.FA) "گشودن قفل" else "Unlock", color = BoneWhite, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun GeneratorPuzzleDialog(viewModel: GameViewModel, language: Language) {
  val switches by viewModel.generatorSwitches.collectAsState()

  Dialog(onDismissRequest = { viewModel.closeDialog() }) {
    Card(
      colors = CardDefaults.cardColors(containerColor = HorrorDarkSurface),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.5.dp, FlashlightAmber, RoundedCornerShape(16.dp))
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = if (language == Language.FA) "تابلو مدار ژنراتور برق" else "Generator Circuit Panel",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = FlashlightAmber
        )
        Text(
          text = if (language == Language.FA) "همه کلیدهای مدار را وصل کنید تا قفل مغناطیسی درب خروج باز شود." else "Activate all circuit switches to disable exit door magnetic lock.",
          fontSize = 12.sp,
          color = BoneWhite,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 4 Electrical Switches (Red, Green, Blue, Yellow)
        val switchNamesFa = listOf("هسته مرکزی (قرمز)", "زمین مدار (سبز)", "فیلتر نوسان (آبی)", "منبع تغذیه (زرد)")
        val switchNamesEn = listOf("Core Circuit (Red)", "Ground Link (Green)", "Filter Link (Blue)", "Main Power (Yellow)")
        val switchColors = listOf(BloodRed, DecayGreen, Color(0xFF1976D2), FlashlightAmber)

        switches.forEachIndexed { index, isOn ->
          Card(
            colors = CardDefaults.cardColors(
              containerColor = if (isOn) switchColors[index].copy(alpha = 0.25f) else HorrorCardSurface
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .border(1.dp, if (isOn) switchColors[index] else MutedGrey, RoundedCornerShape(10.dp))
              .clickable { viewModel.toggleGeneratorSwitch(index) }
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (language == Language.FA) switchNamesFa[index] else switchNamesEn[index],
                color = BoneWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
              )
              Card(
                colors = CardDefaults.cardColors(
                  containerColor = if (isOn) switchColors[index] else Color.DarkGray
                ),
                shape = CircleShape,
                modifier = Modifier.size(24.dp)
              ) {}
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = { viewModel.closeDialog() },
          colors = ButtonDefaults.buttonColors(containerColor = HorrorCardSurface),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(if (language == Language.FA) "بستن پنل" else "Close Panel", color = BoneWhite)
        }
      }
    }
  }
}

@Composable
fun NoteViewDialog(viewModel: GameViewModel, language: Language) {
  val note by viewModel.activeNote.collectAsState()

  Dialog(onDismissRequest = { viewModel.closeDialog() }) {
    Card(
      colors = CardDefaults.cardColors(containerColor = Color(0xFFE8DCC4)),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .border(2.dp, DarkBlood, RoundedCornerShape(12.dp))
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = if (language == Language.FA) note?.nameFa ?: "یادداشت" else note?.nameEn ?: "Note",
          fontSize = 18.sp,
          fontWeight = FontWeight.Black,
          color = DarkBlood
        )

        Spacer(modifier = Modifier.height(12.dp))

        val noteText = when (note) {
          ItemType.NOTE_CREATURE -> {
            if (language == Language.FA) {
              "«او از نور می‌گریزد اما گوش‌های تیزی دارد!\n" +
              "اگر بدوی، صدای گام‌هایت را می‌شنود و به سویت می‌تازد.\n" +
              "هنگامی که صدای خرخر و نفس‌هایش را نزدیک دیدی، در کمدها پنهان شو و نفس نکش تا دور شود...»"
            } else {
              "\"It despises the light, but possesses razor-sharp hearing!\n" +
              "If you run, it will hear your footsteps and hunt you down.\n" +
              "When you hear its heavy guttural breathing nearby, HIDE inside the wardrobes and remain still until it passes...\""
            }
          }
          ItemType.NOTE_SAFE -> {
            if (language == Language.FA) {
              "«دفترچه خاطرات صاحب پیشین عمارت:\n" +
              "سالی که عمارت در آتش سوخت و هیولا از چاه تاریک آزاد شد...\n" +
              "رمز گاوصندوق مطالعه همان سال نفرین است: ۱ ۹ ۸ ۴ (1984).\n" +
              "قیچی آهن‌بر درون گاوصندوق، تنها راه بریدن زنجیرهای خروج است.»"
            } else {
              "\"Diary of the mansion's previous master:\n" +
              "The year the manor burned and the entity was unleashed...\n" +
              "The study safe code is that cursed year: 1 9 8 4.\n" +
              "The Bolt Cutters inside are the only way to sever the heavy exit chains.\""
            }
          }
          ItemType.NOTE_EXIT -> {
            if (language == Language.FA) {
              "«نقشه فرار از عمارت:\n" +
              "درب بزرگ خروج با ۳ سد امنیتی بسته است:\n" +
              "۱. زنجیرهای فولادی (نیاز به قیچی آهن‌بر از گاوصندوق)\n" +
              "۲. قفل طلایی مرگبار (نیاز به کلید طلایی از اتاق خواب اصلی)\n" +
              "۳. قفل الکترومغناطیسی (نیاز به اتصال فیوز و راه‌اندازی ژنراتور)»"
            } else {
              "\"Architect Escape Instructions:\n" +
              "The grand exit doors are sealed by 3 independent locks:\n" +
              "1. Heavy Iron Chains (Cut with Bolt Cutters from safe)\n" +
              "2. Master Golden Deadbolt (Unlocked with Master Key in bedroom)\n" +
              "3. Electromagnetic Lock (Power generator with the circuit fuse)\""
            }
          }
          else -> ""
        }

        Text(
          text = noteText,
          fontSize = 14.sp,
          lineHeight = 22.sp,
          color = Color(0xFF2C1E14),
          fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = { viewModel.closeDialog() },
          colors = ButtonDefaults.buttonColors(containerColor = DarkBlood),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(if (language == Language.FA) "بستن یادداشت" else "Close Note", color = BoneWhite)
        }
      }
    }
  }
}

@Composable
fun GameOverJumpscareScreen(viewModel: GameViewModel, language: Language) {
  val infiniteTransition = rememberInfiniteTransition(label = "game_over_shake")
  val flashAlpha by infiniteTransition.animateFloat(
    initialValue = 0.6f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(tween(200), RepeatMode.Reverse),
    label = "shake"
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black),
    contentAlignment = Alignment.Center
  ) {
    // Monster Face jumpscare
    Image(
      painter = painterResource(id = R.drawable.horror_monster),
      contentDescription = "Monster Jumpscare",
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .fillMaxSize()
        .background(DarkBlood.copy(alpha = flashAlpha * 0.4f))
    )

    // Blood wash
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.radialGradient(
            listOf(Color.Transparent, BloodRed.copy(alpha = 0.6f), Color.Black)
          )
        )
    )

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(24.dp)
    ) {
      Text(
        text = if (language == Language.FA) "کـشـتـه شـدی!" else "YOU WERE CAUGHT!",
        fontSize = 36.sp,
        fontWeight = FontWeight.Black,
        color = BloodRed,
        letterSpacing = 2.sp,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = if (language == Language.FA) "هیولا تو را پیدا کرد! در دور بعد در کمدها پنهان شو و بی صدا حرکت کن." else "The creature found you! Hide inside wardrobes and move quietly.",
        color = BoneWhite,
        textAlign = TextAlign.Center,
        fontSize = 14.sp
      )

      Spacer(modifier = Modifier.height(28.dp))

      Button(
        onClick = { viewModel.retryGame() },
        colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth(0.7f)
          .height(52.dp)
          .testTag("retry_game_button")
      ) {
        Icon(Icons.Default.Refresh, contentDescription = null, tint = BoneWhite)
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (language == Language.FA) "تلاش دوباره" else "TRY AGAIN", fontWeight = FontWeight.Bold, color = BoneWhite)
      }
    }
  }
}

@Composable
fun VictoryEscapeScreen(viewModel: GameViewModel, language: Language) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          listOf(Color(0xFF0D1B2A), Color(0xFF1B263B), Color(0xFF000814))
        )
      )
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Card(
      colors = CardDefaults.cardColors(containerColor = HorrorDarkSurface),
      shape = RoundedCornerShape(20.dp),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.5.dp, DecayGreen, RoundedCornerShape(20.dp))
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "🌕",
          fontSize = 40.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = if (language == Language.FA) "شـمـا فـرار کـردیـد!" else "YOU ESCAPED THE HOUSE!",
          fontSize = 24.sp,
          fontWeight = FontWeight.Black,
          color = DecayGreen,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = if (language == Language.FA) {
            "با بریدن زنجیرهای درب، باز کردن قفل طلایی و روشن کردن ژنراتور، موفق شدید قبل از اینکه هیولا به شما برسد به هوای آزاد مهتابی فرار کنید!\n\nتبریک! معماهای عمارت وحشت با موفقیت حل شدند."
          } else {
            "By cutting the iron chains, unlocking the golden deadbolt, and restoring the generator power, you stepped out into the misty moonlight before the creature could reach you!\n\nCongratulations! You survived the Horror House 3D."
          },
          fontSize = 14.sp,
          lineHeight = 22.sp,
          color = BoneWhite,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
          onClick = { viewModel.retryGame() },
          colors = ButtonDefaults.buttonColors(containerColor = DecayGreen),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("play_again_button")
        ) {
          Text(
            text = if (language == Language.FA) "بازی مجدد" else "PLAY AGAIN",
            fontWeight = FontWeight.Bold,
            color = Color.Black
          )
        }
      }
    }
  }
}
