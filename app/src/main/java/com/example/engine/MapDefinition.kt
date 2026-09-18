package com.example.engine

import com.example.model.InteractiveObject
import com.example.model.InteractiveType
import com.example.model.ItemType

object MapDefinition {
  const val MAP_WIDTH = 20
  const val MAP_HEIGHT = 20

  // 0: Floor
  // 1: Stone Brick Wall
  // 2: Victorian Wallpaper Wall
  // 3: Wood Plank Wall
  // 4: Barred Cellar Wall
  // 5: Bookshelf Wall
  // 10: Cellar Door
  // 11: Study Door
  // 12: Generator Door
  // 13: Main Exit Door
  val MAP = arrayOf(
    intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
    intArrayOf(1, 0, 0, 0, 4, 1, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1),
    intArrayOf(1, 0, 0, 0, 4, 1, 0, 0, 0, 1, 1, 0, 3, 3, 3, 3, 3, 0, 0, 1),
    intArrayOf(1, 0, 0, 0, 10, 0, 0, 0, 0, 1, 1, 0, 3, 0, 0, 0, 3, 0, 0, 1),
    intArrayOf(1, 4, 4, 4, 1, 1, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 3, 0, 0, 1),
    intArrayOf(1, 1, 1, 1, 1, 1, 0, 2, 2, 2, 2, 0, 3, 3, 11, 3, 3, 0, 0, 1),
    intArrayOf(1, 0, 0, 0, 0, 0, 0, 2, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 1),
    intArrayOf(1, 0, 2, 2, 2, 0, 0, 2, 0, 0, 2, 0, 5, 5, 5, 5, 5, 0, 0, 1),
    intArrayOf(1, 0, 2, 0, 2, 0, 0, 2, 2, 0, 2, 0, 5, 0, 0, 0, 5, 0, 0, 1),
    intArrayOf(1, 0, 2, 0, 2, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 5, 0, 0, 1),
    intArrayOf(1, 0, 2, 2, 2, 0, 0, 2, 2, 0, 2, 0, 5, 5, 0, 5, 5, 0, 0, 1),
    intArrayOf(1, 0, 0, 0, 0, 0, 0, 2, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 1),
    intArrayOf(1, 1, 1, 12, 1, 1, 0, 2, 2, 2, 2, 0, 3, 3, 0, 3, 3, 0, 0, 1),
    intArrayOf(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 3, 0, 0, 1),
    intArrayOf(1, 0, 0, 0, 0, 1, 0, 2, 2, 2, 2, 0, 3, 0, 0, 0, 3, 0, 0, 1),
    intArrayOf(1, 0, 0, 0, 0, 1, 0, 2, 0, 0, 2, 0, 3, 3, 3, 3, 3, 0, 0, 1),
    intArrayOf(1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
    intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1),
    intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 1, 13, 13, 1, 0, 0, 0, 0, 0, 0, 0, 1),
    intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
  )

  // Waypoints for Monster patrol
  val MONSTER_WAYPOINTS = listOf(
    Pair(6.5f, 6.5f),
    Pair(11.5f, 6.5f),
    Pair(11.5f, 11.5f),
    Pair(17.5f, 6.5f),
    Pair(17.5f, 14.5f),
    Pair(11.5f, 17.5f),
    Pair(6.5f, 17.5f),
    Pair(6.5f, 11.5f),
    Pair(14.5f, 9.5f)
  )

  fun getInitialObjects(): List<InteractiveObject> {
    return listOf(
      // Cellar items
      InteractiveObject(
        id = "flashlight",
        type = InteractiveType.ITEM_PICKUP,
        x = 2.0f,
        y = 1.5f,
        itemToGrant = ItemType.FLASHLIGHT,
        labelEn = "Pick up Flashlight",
        labelFa = "برداشتن چراغ‌قوه"
      ),
      InteractiveObject(
        id = "cellar_key",
        type = InteractiveType.ITEM_PICKUP,
        x = 3.5f,
        y = 1.5f,
        itemToGrant = ItemType.CELLAR_KEY,
        labelEn = "Take Rusty Cellar Key",
        labelFa = "برداشتن کلید زنگ‌زده زیرزمین"
      ),
      InteractiveObject(
        id = "note_creature",
        type = InteractiveType.NOTE_PICKUP,
        x = 1.5f,
        y = 3.5f,
        itemToGrant = ItemType.NOTE_CREATURE,
        labelEn = "Read Bloody Warning Note",
        labelFa = "خواندن یادداشت خونین روی دیوار"
      ),
      // Cellar door
      InteractiveObject(
        id = "cellar_door",
        type = InteractiveType.CELLAR_DOOR,
        x = 4.0f,
        y = 3.0f,
        requiredItem = ItemType.CELLAR_KEY,
        labelEn = "Unlock Cellar Door",
        labelFa = "باز کردن درب زیرزمین"
      ),
      // Living Room Wardrobe (Hiding spot!)
      InteractiveObject(
        id = "wardrobe_hallway",
        type = InteractiveType.WARDROBE,
        x = 8.5f,
        y = 7.5f,
        labelEn = "Hide Inside Wardrobe",
        labelFa = "پنهان شدن داخل کمد دیواری"
      ),
      // Study Key in hallway side table
      InteractiveObject(
        id = "study_key",
        type = InteractiveType.ITEM_PICKUP,
        x = 9.5f,
        y = 10.5f,
        itemToGrant = ItemType.STUDY_KEY,
        labelEn = "Take Study Key",
        labelFa = "برداشتن کلید اتاق مطالعه"
      ),
      // Study Door
      InteractiveObject(
        id = "study_door",
        type = InteractiveType.STUDY_DOOR,
        x = 14.0f,
        y = 5.0f,
        requiredItem = ItemType.STUDY_KEY,
        labelEn = "Unlock Study Door",
        labelFa = "باز کردن درب اتاق مطالعه"
      ),
      // Study Safe Box (with 4-digit code)
      InteractiveObject(
        id = "study_safe",
        type = InteractiveType.SAFE_BOX,
        x = 15.0f,
        y = 2.5f,
        labelEn = "Crack Safe Dial",
        labelFa = "گشودن رمز گاوصندوق"
      ),
      // Note with safe code hint inside the study
      InteractiveObject(
        id = "note_safe",
        type = InteractiveType.NOTE_PICKUP,
        x = 13.5f,
        y = 3.5f,
        itemToGrant = ItemType.NOTE_SAFE,
        labelEn = "Read Scorched Diary",
        labelFa = "خواندن یادداشت نیم‌سوخته"
      ),
      // Bedroom Wardrobe (Second Hiding spot)
      InteractiveObject(
        id = "wardrobe_bedroom",
        type = InteractiveType.WARDROBE,
        x = 15.0f,
        y = 15.0f,
        labelEn = "Hide Inside Wardrobe",
        labelFa = "پنهان شدن داخل کمد دیواری"
      ),
      // Master Key in the master bedroom
      InteractiveObject(
        id = "master_key",
        type = InteractiveType.ITEM_PICKUP,
        x = 13.5f,
        y = 14.5f,
        itemToGrant = ItemType.MASTER_KEY,
        labelEn = "Take Golden Master Key",
        labelFa = "برداشتن کلید طلایی عمارت"
      ),
      // Note explaining exit procedure
      InteractiveObject(
        id = "note_exit",
        type = InteractiveType.NOTE_PICKUP,
        x = 16.5f,
        y = 13.5f,
        itemToGrant = ItemType.NOTE_EXIT,
        labelEn = "Read Architect Escape Note",
        labelFa = "خواندن نقشه خروج عمارت"
      ),
      // Fuse in hallway cabinet
      InteractiveObject(
        id = "fuse_item",
        type = InteractiveType.ITEM_PICKUP,
        x = 6.5f,
        y = 15.5f,
        itemToGrant = ItemType.FUSE,
        labelEn = "Pick up Generator Fuse",
        labelFa = "برداشتن فیوز ژنراتور"
      ),
      // Generator room door
      InteractiveObject(
        id = "generator_door",
        type = InteractiveType.GENERATOR_DOOR,
        x = 3.0f,
        y = 12.0f,
        labelEn = "Open Generator Room Door",
        labelFa = "گشودن درب اتاق ژنراتور"
      ),
      // Generator Circuit Panel
      InteractiveObject(
        id = "generator_panel",
        type = InteractiveType.GENERATOR_PANEL,
        x = 2.5f,
        y = 14.5f,
        requiredItem = ItemType.FUSE,
        labelEn = "Configure Generator Circuit",
        labelFa = "تنظیم مدار و کلیدهای ژنراتور"
      ),
      // Main Exit Double Doors
      InteractiveObject(
        id = "exit_door",
        type = InteractiveType.EXIT_DOOR,
        x = 9.5f,
        y = 18.0f,
        labelEn = "Examine Front Exit Door",
        labelFa = "بررسی درب خروج اصلی"
      )
    )
  }

  fun isSolid(x: Float, y: Float): Boolean {
    val ix = x.toInt()
    val iy = y.toInt()
    if (ix !in 0 until MAP_WIDTH || iy !in 0 until MAP_HEIGHT) return true
    val tile = MAP[iy][ix]
    return tile in 1..9 || tile == 13 // 13 is exit door, solid until opened
  }

  fun getTile(x: Int, y: Int): Int {
    if (x !in 0 until MAP_WIDTH || y !in 0 until MAP_HEIGHT) return 1
    return MAP[y][x]
  }

  fun setTile(x: Int, y: Int, tile: Int) {
    if (x in 0 until MAP_WIDTH && y in 0 until MAP_HEIGHT) {
      MAP[y][x] = tile
    }
  }
}
