package com.example.engine

import com.example.model.MonsterData
import com.example.model.MonsterState
import com.example.model.PlayerState
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MonsterController {
  val monster = MonsterData()
  private var currentWaypointIndex = 0

  init {
    reset()
  }

  fun reset() {
    val startWp = MapDefinition.MONSTER_WAYPOINTS[0]
    monster.x = startWp.first
    monster.y = startWp.second
    monster.state = MonsterState.PATROL
    monster.speed = 1.6f
    monster.alertLevel = 0f
    monster.searchTimer = 0f
    currentWaypointIndex = 0
    monster.targetX = startWp.first
    monster.targetY = startWp.second
  }

  fun update(
    deltaTime: Float,
    player: PlayerState,
    onJumpscare: () -> Unit,
    onChaseStart: () -> Unit,
    onAlertSound: () -> Unit
  ) {
    val dx = player.x - monster.x
    val dy = player.y - monster.y
    val distToPlayer = sqrt(dx * dx + dy * dy)
    monster.distanceToPlayer = distToPlayer

    // Calculate line of sight between monster and player
    val hasLos = hasLineOfSight(monster.x, monster.y, player.x, player.y)

    // Calculate player noise level
    val playerNoiseRadius = when {
      player.isHiddenInWardrobe -> 0.0f
      player.isCrouching -> 1.8f
      player.isRunning -> 10.0f
      else -> 5.0f
    }

    // Sight detection
    val seesPlayer = hasLos && !player.isHiddenInWardrobe && (
      (player.flashlightOn && distToPlayer < 12.0f) ||
      (distToPlayer < 4.0f)
    )

    // Sound detection
    val hearsPlayer = !player.isHiddenInWardrobe && (distToPlayer < playerNoiseRadius)

    // State machine
    when (monster.state) {
      MonsterState.PATROL -> {
        monster.speed = 1.4f

        if (seesPlayer) {
          monster.state = MonsterState.CHASE
          monster.targetX = player.x
          monster.targetY = player.y
          monster.lastKnownPlayerX = player.x
          monster.lastKnownPlayerY = player.y
          onChaseStart()
        } else if (hearsPlayer) {
          monster.state = MonsterState.ALERT
          monster.targetX = player.x
          monster.targetY = player.y
          monster.searchTimer = 3.0f
          onAlertSound()
        } else {
          // Move towards current waypoint
          patrolMove(deltaTime)
        }
      }

      MonsterState.ALERT -> {
        monster.speed = 1.8f
        moveTowards(monster.targetX, monster.targetY, monster.speed, deltaTime)

        if (seesPlayer) {
          monster.state = MonsterState.CHASE
          onChaseStart()
        } else {
          val distToTarget = distance(monster.x, monster.y, monster.targetX, monster.targetY)
          if (distToTarget < 0.6f) {
            monster.state = MonsterState.SEARCHING
            monster.searchTimer = 4.0f
          }
        }
      }

      MonsterState.CHASE -> {
        monster.speed = 2.4f

        if (player.isHiddenInWardrobe) {
          // Player hid! Monster goes to last seen spot
          monster.state = MonsterState.SEARCHING
          monster.searchTimer = 4.5f
          monster.targetX = monster.lastKnownPlayerX
          monster.targetY = monster.lastKnownPlayerY
        } else if (seesPlayer) {
          monster.lastKnownPlayerX = player.x
          monster.lastKnownPlayerY = player.y
          monster.targetX = player.x
          monster.targetY = player.y
          moveTowards(monster.targetX, monster.targetY, monster.speed, deltaTime)

          // Catch player!
          if (distToPlayer < 0.8f) {
            monster.state = MonsterState.JUMPSCARE
            onJumpscare()
          }
        } else {
          // Lost sight, move to last known position
          moveTowards(monster.lastKnownPlayerX, monster.lastKnownPlayerY, monster.speed, deltaTime)
          val distToTarget = distance(monster.x, monster.y, monster.lastKnownPlayerX, monster.lastKnownPlayerY)
          if (distToTarget < 0.8f) {
            monster.state = MonsterState.SEARCHING
            monster.searchTimer = 5.0f
          }
        }
      }

      MonsterState.SEARCHING -> {
        monster.speed = 0.8f
        monster.searchTimer -= deltaTime
        // Look around slowly
        monster.angle += 1.5f * deltaTime

        if (seesPlayer) {
          monster.state = MonsterState.CHASE
          onChaseStart()
        } else if (hearsPlayer) {
          monster.state = MonsterState.ALERT
          monster.targetX = player.x
          monster.targetY = player.y
        } else if (monster.searchTimer <= 0f) {
          // Resume patrol
          monster.state = MonsterState.PATROL
          chooseNearestWaypoint()
        }
      }

      MonsterState.JUMPSCARE -> {
        // Handled by game view model
      }
    }
  }

  private fun patrolMove(deltaTime: Float) {
    val wp = MapDefinition.MONSTER_WAYPOINTS[currentWaypointIndex]
    val dist = distance(monster.x, monster.y, wp.first, wp.second)
    if (dist < 0.7f) {
      currentWaypointIndex = (currentWaypointIndex + 1) % MapDefinition.MONSTER_WAYPOINTS.size
    } else {
      moveTowards(wp.first, wp.second, monster.speed, deltaTime)
    }
  }

  private fun chooseNearestWaypoint() {
    var bestDist = Float.MAX_VALUE
    var bestIdx = 0
    for (i in MapDefinition.MONSTER_WAYPOINTS.indices) {
      val wp = MapDefinition.MONSTER_WAYPOINTS[i]
      val d = distance(monster.x, monster.y, wp.first, wp.second)
      if (d < bestDist) {
        bestDist = d
        bestIdx = i
      }
    }
    currentWaypointIndex = bestIdx
  }

  private fun moveTowards(targetX: Float, targetY: Float, speed: Float, deltaTime: Float) {
    val dx = targetX - monster.x
    val dy = targetY - monster.y
    val angle = atan2(dy, dx)
    monster.angle = angle

    val moveDist = speed * deltaTime
    val nextX = monster.x + cos(angle) * moveDist
    val nextY = monster.y + sin(angle) * moveDist

    // Simple collision check with padding
    if (!MapDefinition.isSolid(nextX + 0.2f * cos(angle), monster.y)) {
      monster.x = nextX
    }
    if (!MapDefinition.isSolid(monster.x, nextY + 0.2f * sin(angle))) {
      monster.y = nextY
    }
  }

  private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x2 - x1
    val dy = y2 - y1
    return sqrt(dx * dx + dy * dy)
  }

  private fun hasLineOfSight(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
    val dist = distance(x1, y1, x2, y2)
    val steps = (dist * 4).toInt().coerceAtLeast(1)
    val stepX = (x2 - x1) / steps
    val stepY = (y2 - y1) / steps

    var currX = x1
    var currY = y1
    for (i in 0..steps) {
      if (MapDefinition.isSolid(currX, currY)) {
        return false
      }
      currX += stepX
      currY += stepY
    }
    return true
  }
}
