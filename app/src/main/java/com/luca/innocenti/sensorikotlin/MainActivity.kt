package com.luca.innocenti.sensorikotlin


import android.Manifest
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.util.Log
import android.graphics.*
import android.os.*

import android.util.Size

import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.grumpyshoe.module.locationmanager.LocationManager
import com.grumpyshoe.module.locationmanager.impl.LocationManagerImpl
import com.grumpyshoe.module.locationmanager.models.LocationTrackerConfig
import java.io.File
import java.io.PrintWriter
import java.util.concurrent.Executors


private const val REQUEST_CODE_PERMISSIONS = 10
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)


class MainActivity : AppCompatActivity(), SensorEventListener {
   var imageCapture: ImageCapture? = null
    val locationManager: LocationManager = LocationManagerImpl()

    lateinit var mainHandler: Handler

    val sensorManager: SensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);


        locationManager.startLocationTracker(
            activity = this,
            onLocationChange = {
                Log.d(
                    "Location",
                    "New Location - found - lat: " + it.latitude + " lng:" + it.longitude
                )
            },
            config = LocationTrackerConfig()
        )
        /////////// CAMERA ///////////////////
        viewFinder = findViewById(R.id.view_finder)

        // Request camera permissions
        if (allPermissionsGranted()) {
            viewFinder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Every time the provided texture view changes, recompute layout
        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }
        /////////// CAMERA ///////////////////

        mainHandler = Handler(Looper.getMainLooper())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        locationManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
            ?: super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    }

    override fun onStart() {
        super.onStart()
    }


    /////////////////CAMERA////////////////
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var viewFinder: TextureView
    private fun startCamera() {
        // Create configuration object for the viewfinder use case
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetResolution(Size(640, 480))
        }.build()


        // Build the viewfinder use case
        val preview = Preview(previewConfig)

        // Every time the viewfinder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener {

            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        // Create configuration object for the image capture use case
        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .apply {
                setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
            }.build()

        // Build the image capture use case and attach button click listener
        imageCapture = ImageCapture(imageCaptureConfig)



        // Bind use cases to lifecycle
        // If Android Studio complains about "this" being not a LifecycleOwner
        // try rebuilding the project or updating the appcompat dependency to
        // version 1.1.0 or higher.
        CameraX.bindToLifecycle(this, preview,imageCapture)
    }

    private fun updateTransform() {
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when(viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        // Finally, apply transformations to our TextureView
        viewFinder.setTransform(matrix)    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
    /////////////////CAMERA////////////////

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )

        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
            SensorManager.SENSOR_DELAY_NORMAL
        )


        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
            SensorManager.SENSOR_DELAY_NORMAL
        )

        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )
        mainHandler.post(updatimelapse)

    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        mainHandler.removeCallbacks(updatimelapse)
    }

    fun write_to_file(valori: String) {


    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor!!.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            Log.d(
                "Lin: ",
                event.values[0].toString() + ";" + event.values[0].toString() + ";" + event.values[0].toString()
            )
        }

        if ((if (event != null) event.sensor else null)!!.type == Sensor.TYPE_GYROSCOPE) {
            Log.d(
                "Gyr: ",
                event.values[0].toString() + ";" + event.values[0].toString() + ";" + event.values[0].toString()
            )
        }

        if (event?.sensor!!.type == Sensor.TYPE_ACCELEROMETER) {
            Log.d(
                "Acc: ",
                event.values[0].toString() + ";" + event.values[0].toString() + ";" + event.values[0].toString()
            )
        }

        locationManager.getLastKnownPosition(
            activity = this,
            onLastLocationFound = { location ->
                Log.d(
                    "Location",
                    "Last Location - found - lat: " + location.latitude + " lng:" + location.longitude
                )
            },
            onNoLocationFound = {
                // handle no location data
            })
        //write_to_file("Luca")
    }

    /////////////////////// INTENT //////////////
    private val updatimelapse = object : Runnable {
        override fun run() {
            timelapse()
            mainHandler.postDelayed(this, 1000)
        }
    }


    fun timelapse() {
        Log.d("timelapse","Timelapse")
        val file = File(externalMediaDirs.first(), "${System.currentTimeMillis()}.jpg")
        this.imageCapture?.takePicture(file, executor,
            object : ImageCapture.OnImageSavedListener {
                override fun onError(
                    imageCaptureError: ImageCapture.ImageCaptureError,
                    message: String,
                    exc: Throwable?
                ) {
                    val msg = "Photo capture failed: $message"
                    Log.e("CameraXApp", msg, exc)
                    viewFinder.post {
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onImageSaved(file: File) {
                    val msg = "Photo capture succeeded: ${file.absolutePath}"
                    Log.d("CameraXApp", msg)
                    //viewFinder.post {
                    //    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    //}
                }
            })

    }
}




