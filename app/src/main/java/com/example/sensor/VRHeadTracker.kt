package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.PI

class VRHeadTracker(context: Context) : SensorEventListener {
  private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
  private var rotationSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    ?: sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

  private val rotationMatrix = FloatArray(16)
  private val remappedMatrix = FloatArray(16)
  private val orientation = FloatArray(3)

  @Volatile var isTracking = false
    private set

  // Angles in radians
  var yaw: Float = 0f
    private set
  var pitch: Float = 0f
    private set

  private var initialYawOffset: Float = 0f
  private var isCalibrated: Boolean = false

  fun start() {
    if (isTracking || sensorManager == null || rotationSensor == null) return
    sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
    isTracking = true
  }

  fun stop() {
    if (!isTracking) return
    sensorManager?.unregisterListener(this)
    isTracking = false
  }

  fun calibrate(currentCameraYaw: Float) {
    initialYawOffset = yaw - currentCameraYaw
    isCalibrated = true
  }

  override fun onSensorChanged(event: SensorEvent?) {
    if (event == null) return
    if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR ||
        event.sensor.type == Sensor.TYPE_GAME_ROTATION_VECTOR
    ) {
      SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
      // Remap coordinates for landscape VR mode
      SensorManager.remapCoordinateSystem(
        rotationMatrix,
        SensorManager.AXIS_X,
        SensorManager.AXIS_Z,
        remappedMatrix
      )
      SensorManager.getOrientation(remappedMatrix, orientation)

      // orientation[0] = azimuth / yaw, orientation[1] = pitch, orientation[2] = roll
      val rawYaw = orientation[0]
      val rawPitch = orientation[1]

      if (!isCalibrated) {
        initialYawOffset = rawYaw
        isCalibrated = true
      }

      var normalizedYaw = rawYaw - initialYawOffset
      while (normalizedYaw < -PI) normalizedYaw += (2 * PI).toFloat()
      while (normalizedYaw > PI) normalizedYaw -= (2 * PI).toFloat()

      // Low pass filter
      yaw = yaw * 0.7f + normalizedYaw * 0.3f
      pitch = (pitch * 0.7f + rawPitch * 0.3f).coerceIn(-0.8f, 0.8f)
    }
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
