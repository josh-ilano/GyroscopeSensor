package com.example.gyroscopesensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gyroscopesensor.ui.theme.GyroscopeSensorTheme
import kotlin.math.absoluteValue

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var gyroscope: Sensor? = null
    private var magnetometer: Sensor? = null
    private var accelerometer:  Sensor? = null

    private var _xRotation by mutableStateOf(0f)
    private var _yRotation by mutableStateOf(0f)
    private var _zRotation by mutableStateOf(0f)

    private var _xAccel by mutableStateOf(0f)
    private var _yAccel by mutableStateOf(0f)
    private var _zAccel by mutableStateOf(0f)

    private var _xMag by mutableStateOf(0f)
    private var _yMag by mutableStateOf(0f)
    private var _zMag by mutableStateOf(0f)


    private var _azimuth by mutableStateOf(0f)
    private var _roll by mutableStateOf(0f)
    private var _pitch by mutableStateOf(0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Sensor Manager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setContent {
            GyroscopeSensorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // initially rotation
                    GyroscopeScreen(azimuth = _azimuth, roll = _roll, pitch = _pitch)
                }
            }
        }
    }

    // Registers the gyroscope sensor when the app starts (onResume)
    override fun onResume() {
        super.onResume()
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    // Unregisters the sensor when the app is paused (onPause) to save battery
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }


    // Sensor event listener
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (event.sensor.type) {
                Sensor.TYPE_GYROSCOPE -> {
                    _xRotation = event.values[0] // Rotation around X-axis
                    _yRotation = event.values[1] // Rotation around Y-axis
                    _zRotation = event.values[2] // Rotation around Z-axis
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    _xAccel = event.values[0]
                    _yAccel = event.values[1]
                    _zAccel = event.values[2]
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    _xMag = event.values[0]
                    _yMag = event.values[1]
                    _zMag = event.values[2]
                }
            }



            _roll += _xRotation
            _pitch += _yRotation
            _pitch = _pitch.coerceIn(0f, 150f)

            Log.d("Pitch", _pitch.toString())
            // The calculations for azimuth, which is where compass is pointed to, were generated
            // by Chat-GPT
            // Now calculate the azimuth if both accelerometer and magnetometer data are available

                // Create arrays for gravity and geomagnetic data
                val gravity = FloatArray(3)
                val geomagnetic = FloatArray(3)

                // Populate gravity and geomagnetic arrays with the sensor data
                gravity[0] = _xAccel; gravity[1] = _yAccel; gravity[2] = _zAccel
                geomagnetic[0] = _xMag; geomagnetic[1] = _yMag; geomagnetic[2] = _zMag

                // Compute the rotation matrix and the orientation
                val R = FloatArray(9)
                val I = FloatArray(9)

                if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(R, orientation)

                    // Calculate the azimuth (heading) in degrees
                    var azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()

                    // Normalize azimuth to be between 0 and 360 degrees
                    if (azimuth < 0) { azimuth += 360 }

                    // Update your compass view or UI
                    _azimuth = azimuth
                }

        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this example
    }
}

@Composable
fun GyroscopeScreen(azimuth: Float, roll: Float, pitch: Float) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Compass", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)

        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.compass),
            contentDescription = "Compass",
            modifier = Modifier.rotate(azimuth)
                .fillMaxWidth(.9f)
        )

        Spacer(modifier = Modifier.height(32.dp))


        Text(text = "Barrel", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)

        Box(modifier=Modifier.rotate(roll)) {
            Image(
                painter = painterResource(id = R.drawable.barrel),
                contentDescription = "Barrel",
                modifier = Modifier.fillMaxWidth(.9f)
            )
            Box(
                modifier = Modifier
                    .width(5.dp)  // Set the width of the divider
                    .height((pitch.absoluteValue.toInt()).dp)  // Set the height of the divider (not full screen)
                    .background(Color.Black)  // Divider color
                    .align(Alignment.Center)
            )
        }


    }
}

@Composable
fun SensorValue(label: String, value: Float) {
    Text(
        text = "$label: ${"%.2f".format(value)}",
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        color = Color.DarkGray
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    GyroscopeSensorTheme {
//        GyroscopeScreen(x = 0f, y = 0f, z = 0f)
    }
}