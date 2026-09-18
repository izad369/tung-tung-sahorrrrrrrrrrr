package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.example.model.InteractiveObject
import com.example.model.InteractiveType
import com.example.model.MonsterData
import com.example.model.MonsterState
import com.example.model.PlayerState
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class HorrorRaycaster(context: Context? = null) {
  val renderWidth = 320
  val renderHeight = 220

  val frameBuffer = IntArray(renderWidth * renderHeight)
  val zBuffer = FloatArray(renderWidth)
  val bitmap: Bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)

  private var wallTexture: Bitmap? = null
  private var monsterTexture: Bitmap? = null
  private val texSize = 64
  private val proceduralTextures = Array(8) { IntArray(texSize * texSize) }

  init {
    loadAssets(context)
    generateProceduralTextures()
  }

  private fun loadAssets(context: Context?) {
    if (context == null) return
    try {
      val wallResId = context.resources.getIdentifier("horror_wall", "drawable", context.packageName)
      if (wallResId != 0) {
        val rawWall = BitmapFactory.decodeResource(context.resources, wallResId)
        wallTexture = Bitmap.createScaledBitmap(rawWall, texSize, texSize, true)
      }
      val monsterResId = context.resources.getIdentifier("horror_monster", "drawable", context.packageName)
      if (monsterResId != 0) {
        val rawMonster = BitmapFactory.decodeResource(context.resources, monsterResId)
        monsterTexture = Bitmap.createScaledBitmap(rawMonster, texSize, texSize, true)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun generateProceduralTextures() {
    // 1: Stone Brick
    for (y in 0 until texSize) {
      for (x in 0 until texSize) {
        val isMortar = (y % 16 == 0) || ((x + (y / 16) * 16) % 32 == 0)
        val noise = ((x * 13 + y * 29) % 20)
        proceduralTextures[1][y * texSize + x] = if (isMortar) {
          Color.rgb(30, 26, 28)
        } else {
          val shade = (50 + noise).coerceIn(0, 255)
          Color.rgb(shade, shade - 10, shade - 8)
        }
      }
    }

    // 2: Victorian Wallpaper with blood scratches
    for (y in 0 until texSize) {
      for (x in 0 until texSize) {
        val pat = (sin(x * 0.4) * cos(y * 0.4) * 20).toInt()
        val scratch = if (abs(x - y) < 2 && y in 10..40) 80 else 0
        val r = (65 + pat + scratch).coerceIn(0, 255)
        val g = (25 + pat / 2).coerceIn(0, 255)
        val b = (28 + pat / 2).coerceIn(0, 255)
        proceduralTextures[2][y * texSize + x] = Color.rgb(r, g, b)
      }
    }

    // 3: Wood Plank
    for (y in 0 until texSize) {
      for (x in 0 until texSize) {
        val isSeam = x % 16 == 0
        val grain = ((y * 7) % 15)
        proceduralTextures[3][y * texSize + x] = if (isSeam) {
          Color.rgb(25, 15, 10)
        } else {
          val r = (85 + grain).coerceIn(0, 255)
          val g = (50 + grain / 2).coerceIn(0, 255)
          val b = (25 + grain / 3).coerceIn(0, 255)
          Color.rgb(r, g, b)
        }
      }
    }

    // 4: Barred Gate / Cellar Window
    for (y in 0 until texSize) {
      for (x in 0 until texSize) {
        val isBar = x % 10 in 0..1 || y == 4 || y == texSize - 5
        proceduralTextures[4][y * texSize + x] = if (isBar) {
          Color.rgb(90, 85, 80)
        } else {
          Color.rgb(10, 5, 8) // dark void behind bars
        }
      }
    }

    // 5: Bookshelf
    for (y in 0 until texSize) {
      for (x in 0 until texSize) {
        val isShelf = y % 20 in 0..2
        if (isShelf) {
          proceduralTextures[5][y * texSize + x] = Color.rgb(70, 45, 25)
        } else {
          val bookColor = ((x / 7 * 37) % 3)
          val shade = 60 + (x % 7) * 4
          proceduralTextures[5][y * texSize + x] = when (bookColor) {
            0 -> Color.rgb(shade, 20, 20)
            1 -> Color.rgb(20, shade, 30)
            else -> Color.rgb(shade - 10, shade - 20, 15)
          }
        }
      }
    }
  }

  fun render(
    player: PlayerState,
    monster: MonsterData,
    objects: List<InteractiveObject>,
    isVRMode: Boolean
  ): Bitmap {
    if (isVRMode) {
      renderStereoVR(player, monster, objects)
    } else {
      renderSingleView(player.x, player.y, player.angle, player.pitch, player, monster, objects, 0, renderWidth)
    }
    bitmap.setPixels(frameBuffer, 0, renderWidth, 0, 0, renderWidth, renderHeight)
    return bitmap
  }

  private fun renderStereoVR(
    player: PlayerState,
    monster: MonsterData,
    objects: List<InteractiveObject>
  ) {
    val halfWidth = renderWidth / 2
    val eyeSep = 0.08f // Eye separation for 3D stereoscopic depth

    // Perpendicular vector to player orientation
    val perpX = -sin(player.angle)
    val perpY = cos(player.angle)

    // Left Eye
    val leftX = player.x - perpX * eyeSep
    val leftY = player.y - perpY * eyeSep
    renderSingleView(leftX, leftY, player.angle, player.pitch, player, monster, objects, 0, halfWidth)

    // Right Eye
    val rightX = player.x + perpX * eyeSep
    val rightY = player.y + perpY * eyeSep
    renderSingleView(rightX, rightY, player.angle, player.pitch, player, monster, objects, halfWidth, halfWidth)

    // Draw central VR separator line and circular lens vignette
    for (y in 0 until renderHeight) {
      frameBuffer[y * renderWidth + halfWidth - 1] = Color.BLACK
      frameBuffer[y * renderWidth + halfWidth] = Color.BLACK
      frameBuffer[y * renderWidth + halfWidth + 1] = Color.BLACK
    }

    // VR Reticles (crosshair dots in both eyes)
    val centerY = renderHeight / 2 + (player.pitch * 30).toInt()
    drawReticle(halfWidth / 2, centerY)
    drawReticle(halfWidth + halfWidth / 2, centerY)
  }

  private fun drawReticle(cx: Int, cy: Int) {
    if (cy !in 2 until renderHeight - 2 || cx !in 2 until renderWidth - 2) return
    val reticleColor = Color.rgb(255, 60, 60)
    for (dy in -2..2) {
      for (dx in -2..2) {
        if (abs(dx) + abs(dy) <= 2) {
          frameBuffer[(cy + dy) * renderWidth + (cx + dx)] = reticleColor
        }
      }
    }
  }

  private fun renderSingleView(
    camX: Float,
    camY: Float,
    angle: Float,
    pitch: Float,
    player: PlayerState,
    monster: MonsterData,
    objects: List<InteractiveObject>,
    viewStartX: Int,
    viewWidth: Int
  ) {
    val dirX = cos(angle)
    val dirY = sin(angle)
    val fov = 0.85f // FOV plane scale
    val planeX = -dirY * fov
    val planeY = dirX * fov

    val pitchOffset = (pitch * (renderHeight * 0.4f)).toInt()
    val horizonY = renderHeight / 2 + pitchOffset

    // 1. Draw Ceiling and Floor with atmospheric lighting
    for (y in 0 until renderHeight) {
      val isFloor = y >= horizonY
      val distFactor = if (isFloor) {
        val row = (y - horizonY).coerceAtLeast(1)
        (renderHeight.toFloat() / (2.0f * row)).coerceIn(0.1f, 15f)
      } else {
        val row = (horizonY - y).coerceAtLeast(1)
        (renderHeight.toFloat() / (2.0f * row)).coerceIn(0.1f, 15f)
      }

      val fog = (1.0f / (1.0f + distFactor * 0.45f)).coerceIn(0f, 1f)
      val baseColor = if (isFloor) {
        // Dark aged floorboard
        val r = (32 * fog).toInt()
        val g = (20 * fog).toInt()
        val b = (15 * fog).toInt()
        Color.rgb(r, g, b)
      } else {
        // Dark decaying ceiling
        val r = (12 * fog).toInt()
        val g = (8 * fog).toInt()
        val b = (10 * fog).toInt()
        Color.rgb(r, g, b)
      }

      val rowOffset = y * renderWidth
      for (x in viewStartX until viewStartX + viewWidth) {
        // Flashlight spotlight effect
        val screenCenterX = viewStartX + viewWidth / 2
        val dx = x - screenCenterX
        val dy = y - horizonY
        val screenDistSq = dx * dx + dy * dy
        val flashBonus = if (player.flashlightOn) {
          val flashIntensity = (1.0f - screenDistSq / (viewWidth * viewWidth * 0.45f)).coerceIn(0f, 1f)
          flashIntensity * (player.flashlightBattery / 100f) * 0.7f
        } else 0f

        val finalR = (Color.red(baseColor) * (1f + flashBonus * 1.8f)).toInt().coerceIn(0, 255)
        val finalG = (Color.green(baseColor) * (1f + flashBonus * 1.6f)).toInt().coerceIn(0, 255)
        val finalB = (Color.blue(baseColor) * (1f + flashBonus * 1.2f)).toInt().coerceIn(0, 255)
        frameBuffer[rowOffset + x] = Color.rgb(finalR, finalG, finalB)
      }
    }

    // 2. DDA Raycasting for Walls
    for (x in 0 until viewWidth) {
      val globalX = viewStartX + x
      val cameraX = 2f * x / viewWidth - 1f
      val rayDirX = dirX + planeX * cameraX
      val rayDirY = dirY + planeY * cameraX

      var mapX = camX.toInt()
      var mapY = camY.toInt()

      val deltaDistX = if (rayDirX == 0f) 1e30f else abs(1f / rayDirX)
      val deltaDistY = if (rayDirY == 0f) 1e30f else abs(1f / rayDirY)

      var stepX: Int
      var sideDistX: Float
      if (rayDirX < 0) {
        stepX = -1
        sideDistX = (camX - mapX) * deltaDistX
      } else {
        stepX = 1
        sideDistX = (mapX + 1.0f - camX) * deltaDistX
      }

      var stepY: Int
      var sideDistY: Float
      if (rayDirY < 0) {
        stepY = -1
        sideDistY = (camY - mapY) * deltaDistY
      } else {
        stepY = 1
        sideDistY = (mapY + 1.0f - camY) * deltaDistY
      }

      var hit = false
      var side = 0 // 0 for X, 1 for Y
      var hitTile = 1
      var stepsCount = 0

      while (!hit && stepsCount < 40) {
        stepsCount++
        if (sideDistX < sideDistY) {
          sideDistX += deltaDistX
          mapX += stepX
          side = 0
        } else {
          sideDistY += deltaDistY
          mapY += stepY
          side = 1
        }

        val tile = MapDefinition.getTile(mapX, mapY)
        if (tile > 0) {
          hit = true
          hitTile = tile
        }
      }

      val perpWallDist = if (side == 0) {
        (mapX - camX + (1 - stepX) / 2f) / rayDirX
      } else {
        (mapY - camY + (1 - stepY) / 2f) / rayDirY
      }.coerceAtLeast(0.05f)

      zBuffer[globalX] = perpWallDist

      // Calculate wall slice height
      val lineHeight = (renderHeight / perpWallDist).toInt()
      val drawStart = (horizonY - lineHeight / 2).coerceIn(0, renderHeight - 1)
      val drawEnd = (horizonY + lineHeight / 2).coerceIn(0, renderHeight - 1)

      // Texture coordinate calculation
      var wallX = if (side == 0) camY + perpWallDist * rayDirY else camX + perpWallDist * rayDirX
      wallX -= floor(wallX)
      var texX = (wallX * texSize).toInt()
      if (side == 0 && rayDirX > 0) texX = texSize - texX - 1
      if (side == 1 && rayDirY < 0) texX = texSize - texX - 1

      // Lighting & Flashlight calculation
      val distFog = (1.0f / (1.0f + perpWallDist * 0.35f)).coerceIn(0.05f, 1.0f)
      val sideShade = if (side == 1) 0.75f else 1.0f

      val rayAngleOffset = abs(cameraX)
      val inFlashlightCone = rayAngleOffset < 0.65f
      val flashBonus = if (player.flashlightOn && inFlashlightCone) {
        val coneCenter = (1.0f - rayAngleOffset / 0.65f)
        (coneCenter * 1.5f / (1.0f + perpWallDist * 0.25f)) * (player.flashlightBattery / 100f)
      } else 0.0f

      val lightLevel = ((distFog * sideShade) + flashBonus).coerceIn(0.05f, 1.8f)

      // Render wall column
      val texArray = when (hitTile) {
        10, 11, 12, 13 -> proceduralTextures[3] // Doors look like heavy reinforced wood
        2 -> {
          if (wallTexture != null) null else proceduralTextures[2]
        }
        in 1..5 -> proceduralTextures[hitTile]
        else -> proceduralTextures[1]
      }

      for (y in drawStart..drawEnd) {
        val d = y - horizonY + lineHeight / 2
        val texY = ((d * texSize) / lineHeight).coerceIn(0, texSize - 1)
        val pixelColor = if (hitTile == 2 && wallTexture != null) {
          wallTexture!!.getPixel(texX.coerceIn(0, texSize - 1), texY)
        } else {
          texArray?.get(texY * texSize + texX.coerceIn(0, texSize - 1)) ?: Color.DKGRAY
        }

        // Apply lighting
        val r = (Color.red(pixelColor) * lightLevel).toInt().coerceIn(0, 255)
        val g = (Color.green(pixelColor) * lightLevel).toInt().coerceIn(0, 255)
        val b = (Color.blue(pixelColor) * lightLevel).toInt().coerceIn(0, 255)

        frameBuffer[y * renderWidth + globalX] = Color.rgb(r, g, b)
      }
    }

    // 3. Render 3D Sprites (Monster, Wardrobes, Items)
    renderSprites(camX, camY, dirX, dirY, planeX, planeY, horizonY, player, monster, objects, viewStartX, viewWidth)
  }

  private fun renderSprites(
    camX: Float,
    camY: Float,
    dirX: Float,
    dirY: Float,
    planeX: Float,
    planeY: Float,
    horizonY: Int,
    player: PlayerState,
    monster: MonsterData,
    objects: List<InteractiveObject>,
    viewStartX: Int,
    viewWidth: Int
  ) {
    data class SpriteItem(val x: Float, val y: Float, val type: String, val obj: InteractiveObject?, val dist: Float)

    val spriteList = mutableListOf<SpriteItem>()

    // Add monster
    val mDist = (camX - monster.x) * (camX - monster.x) + (camY - monster.y) * (camY - monster.y)
    spriteList.add(SpriteItem(monster.x, monster.y, "monster", null, mDist))

    // Add interactive objects
    for (obj in objects) {
      if (obj.isCollected) continue
      val oDist = (camX - obj.x) * (camX - obj.x) + (camY - obj.y) * (camY - obj.y)
      spriteList.add(SpriteItem(obj.x, obj.y, "object", obj, oDist))
    }

    // Sort far to near
    spriteList.sortByDescending { it.dist }

    for (sprite in spriteList) {
      val spriteX = sprite.x - camX
      val spriteY = sprite.y - camY

      // Inverse camera matrix
      val invDet = 1.0f / (planeX * dirY - dirX * planeY)
      val transformX = invDet * (dirY * spriteX - dirX * spriteY)
      val transformY = invDet * (-planeY * spriteX + planeX * spriteY) // depth

      if (transformY <= 0.1f) continue

      val spriteScreenX = (viewWidth / 2f * (1f + transformX / transformY)).toInt()
      val spriteHeight = abs((renderHeight / transformY).toInt())
      val spriteWidth = abs((renderHeight / transformY).toInt())

      val drawStartY = (horizonY - spriteHeight / 2).coerceIn(0, renderHeight - 1)
      val drawEndY = (horizonY + spriteHeight / 2).coerceIn(0, renderHeight - 1)

      val drawStartX = (spriteScreenX - spriteWidth / 2).coerceIn(0, viewWidth - 1)
      val drawEndX = (spriteScreenX + spriteWidth / 2).coerceIn(0, viewWidth - 1)

      val distFog = (1.0f / (1.0f + transformY * 0.3f)).coerceIn(0.1f, 1.0f)
      val flashBonus = if (player.flashlightOn && abs(transformX / transformY) < 0.6f) {
        (1.5f / (1.0f + transformY * 0.25f)) * (player.flashlightBattery / 100f)
      } else 0f
      val lightLevel = (distFog + flashBonus).coerceIn(0.1f, 2.0f)

      if (sprite.type == "monster") {
        renderMonsterSprite(drawStartX, drawEndX, drawStartY, drawEndY, spriteScreenX, spriteWidth, spriteHeight, transformY, horizonY, lightLevel, monster.state, viewStartX, viewWidth)
      } else {
        renderObjectSprite(sprite.obj!!, drawStartX, drawEndX, drawStartY, drawEndY, spriteScreenX, spriteWidth, spriteHeight, transformY, horizonY, lightLevel, viewStartX, viewWidth)
      }
    }
  }

  private fun renderMonsterSprite(
    drawStartX: Int,
    drawEndX: Int,
    drawStartY: Int,
    drawEndY: Int,
    screenX: Int,
    spriteWidth: Int,
    spriteHeight: Int,
    depth: Float,
    horizonY: Int,
    lightLevel: Float,
    monsterState: MonsterState,
    viewStartX: Int,
    viewWidth: Int
  ) {
    for (stripe in drawStartX until drawEndX) {
      val globalX = viewStartX + stripe
      if (depth < zBuffer[globalX]) {
        val texX = ((stripe - (screenX - spriteWidth / 2)) * texSize / spriteWidth).coerceIn(0, texSize - 1)
        for (y in drawStartY until drawEndY) {
          val d = y - horizonY + spriteHeight / 2
          val texY = ((d * texSize) / spriteHeight).coerceIn(0, texSize - 1)

          var pixelColor: Int
          if (monsterTexture != null) {
            pixelColor = monsterTexture!!.getPixel(texX, texY)
            // Filter dark background
            if (Color.red(pixelColor) < 18 && Color.green(pixelColor) < 18 && Color.blue(pixelColor) < 18) {
              continue
            }
          } else {
            // Procedural monster silhouette with glowing red eyes
            val isEye = (texY in 15..20) && (texX in 22..26 || texX in 38..42)
            val isBody = abs(texX - 32) < (45 - texY * 0.6) && texY in 10..60
            if (!isBody && !isEye) continue
            pixelColor = if (isEye) Color.RED else Color.rgb(25, 20, 22)
          }

          // Red glowing eyes boost & terror red pulse during chase
          val isChasing = monsterState == MonsterState.CHASE
          val rBoost = if (isChasing) 1.5f else 1.0f
          val r = (Color.red(pixelColor) * lightLevel * rBoost).toInt().coerceIn(0, 255)
          val g = (Color.green(pixelColor) * lightLevel).toInt().coerceIn(0, 255)
          val b = (Color.blue(pixelColor) * lightLevel).toInt().coerceIn(0, 255)

          frameBuffer[y * renderWidth + globalX] = Color.rgb(r, g, b)
        }
      }
    }
  }

  private fun renderObjectSprite(
    obj: InteractiveObject,
    drawStartX: Int,
    drawEndX: Int,
    drawStartY: Int,
    drawEndY: Int,
    screenX: Int,
    spriteWidth: Int,
    spriteHeight: Int,
    depth: Float,
    horizonY: Int,
    lightLevel: Float,
    viewStartX: Int,
    viewWidth: Int
  ) {
    for (stripe in drawStartX until drawEndX) {
      val globalX = viewStartX + stripe
      if (depth < zBuffer[globalX]) {
        val texX = ((stripe - (screenX - spriteWidth / 2)) * texSize / spriteWidth).coerceIn(0, texSize - 1)
        for (y in drawStartY until drawEndY) {
          val d = y - horizonY + spriteHeight / 2
          val texY = ((d * texSize) / spriteHeight).coerceIn(0, texSize - 1)

          val color = when (obj.type) {
            InteractiveType.WARDROBE -> {
              // Tall wooden wardrobe cabinet with brass handle
              val isFrame = texX in 0..4 || texX in 59..63 || texY in 0..4 || texY in 59..63
              val isHandle = abs(texX - 30) < 3 && abs(texY - 32) < 4
              if (isHandle) Color.rgb(230, 180, 50)
              else if (isFrame) Color.rgb(55, 30, 18)
              else Color.rgb(90, 50, 28)
            }
            InteractiveType.SAFE_BOX -> {
              // Metal safe box with dial
              val isDial = (texX - 32) * (texX - 32) + (texY - 32) * (texY - 32) < 100
              if (isDial) Color.rgb(180, 175, 160) else Color.rgb(45, 45, 50)
            }
            InteractiveType.GENERATOR_PANEL -> {
              // Electrical box with glowing indicator
              val isLight = abs(texX - 32) < 5 && abs(texY - 20) < 5
              if (isLight) Color.rgb(50, 255, 50) else Color.rgb(60, 65, 70)
            }
            InteractiveType.ITEM_PICKUP -> {
              // Golden/bright glowing key or item
              val distCenter = abs(texX - 32) + abs(texY - 32)
              if (distCenter > 20) continue
              Color.rgb(255, 215, 60)
            }
            InteractiveType.NOTE_PICKUP -> {
              // Parchment paper with blood writing
              if (texX !in 15..49 || texY !in 15..49) continue
              val isBlood = (texX + texY) % 7 == 0
              if (isBlood) Color.rgb(180, 20, 20) else Color.rgb(220, 205, 175)
            }
            else -> continue
          }

          val r = (Color.red(color) * lightLevel).toInt().coerceIn(0, 255)
          val g = (Color.green(color) * lightLevel).toInt().coerceIn(0, 255)
          val b = (Color.blue(color) * lightLevel).toInt().coerceIn(0, 255)
          frameBuffer[y * renderWidth + globalX] = Color.rgb(r, g, b)
        }
      }
    }
  }
}
