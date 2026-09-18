package com.example

import com.example.engine.MapDefinition
import com.example.engine.MonsterController
import com.example.model.ItemType
import com.example.model.MonsterState
import com.example.model.PlayerState
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testMapSolidBoundaries() {
    // Outer border should be solid wall
    assertTrue(MapDefinition.isSolid(0f, 0f))
    assertTrue(MapDefinition.isSolid(19f, 19f))
    // Starting cellar room should have walkable floor
    assertFalse(MapDefinition.isSolid(2.5f, 2.5f))
  }

  @Test
  fun testInteractiveObjectsSetup() {
    val objects = MapDefinition.getInitialObjects()
    assertTrue(objects.any { it.itemToGrant == ItemType.FLASHLIGHT })
    assertTrue(objects.any { it.itemToGrant == ItemType.CELLAR_KEY })
    assertTrue(objects.any { it.id == "wardrobe_hallway" })
    assertTrue(objects.any { it.id == "study_safe" })
  }

  @Test
  fun testMonsterControllerPatrol() {
    val monsterController = MonsterController()
    assertEquals(MonsterState.PATROL, monsterController.monster.state)

    val player = PlayerState(x = 2.5f, y = 2.5f, isHiddenInWardrobe = true)
    var jumpscareTriggered = false
    monsterController.update(
      0.1f,
      player,
      onJumpscare = { jumpscareTriggered = true },
      onChaseStart = {},
      onAlertSound = {}
    )

    // Hidden player should not trigger jumpscare or chase
    assertFalse(jumpscareTriggered)
    assertNotEquals(MonsterState.CHASE, monsterController.monster.state)
  }
}
