/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.warwick.postroom.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.*
import android.content.res.Configuration
import android.graphics.*
import android.hardware.display.DisplayManager
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.ImageButton
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.Navigation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.camera_ui_container.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import nl.dionsegijn.konfetti.KonfettiView
import nl.dionsegijn.konfetti.models.Shape
import uk.ac.warwick.postroom.OcrImageAnalyzer
import uk.ac.warwick.postroom.R
import uk.ac.warwick.postroom.activities.KEY_EVENT_ACTION
import uk.ac.warwick.postroom.activities.KEY_EVENT_EXTRA
import uk.ac.warwick.postroom.activities.SettingsActivity
import uk.ac.warwick.postroom.domain.ItemAdditionError
import uk.ac.warwick.postroom.domain.RecognisedBarcode
import uk.ac.warwick.postroom.services.*
import uk.ac.warwick.postroom.utils.simulateClick
import uk.ac.warwick.postroom.vm.CameraViewModel
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Main fragment for this app. Implements all camera operations including:
 * - Viewfinder
 * - Photo "taking"
 * - Image analysis (barcode scanning, OCR)
 */
@AndroidEntryPoint
class CameraFragment : Fragment() {

    private var dingSoundId: Int? = null
    private var popSoundId: Int? = null
    private var soundPool: SoundPool? = null

    private lateinit var container: ConstraintLayout
    private lateinit var viewFinder: PreviewView
    private lateinit var outputDirectory: File
    private lateinit var broadcastManager: LocalBroadcastManager

    @Inject
    lateinit var recipientDataService: RecipientDataService

    @Inject
    lateinit var sscPersistenceService: SscPersistenceService

    @Inject
    lateinit var baseUrl: ProvidesBaseUrl

    @Inject
    lateinit var courierMatchService: CourierMatchService

    @Inject
    lateinit var itemService: ItemService

    private var displayId: Int = -1
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private val executor1 = Executors.newSingleThreadExecutor()

    private val displayManager by lazy {
        requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    /** Volume down button receiver used to trigger shutter */
    private val volumeDownReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(KEY_EVENT_EXTRA, KeyEvent.KEYCODE_UNKNOWN)) {
                // When the volume down button is pressed, simulate a shutter button click
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    val shutter = container
                        .findViewById<ImageButton>(R.id.camera_capture_button)
                    shutter.simulateClick()
                }
            }
        }
    }

    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@CameraFragment.displayId && view.display != null) {
                Log.d(TAG, "Rotation changed: ${view.display.rotation}")

                imageCapture?.targetRotation = view.display.rotation
                imageAnalyzer?.targetRotation = view.display.rotation
            }
        } ?: Unit
    }

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                CameraFragmentDirections.actionCameraToPermissions()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Clean up sound pool resources
        soundPool?.release()
        dingSoundId = null

        // Shut down our background executor
        cameraExecutor.shutdown()

        // Unregister the broadcast receivers and listeners
        broadcastManager.unregisterReceiver(volumeDownReceiver)
        displayManager.unregisterDisplayListener(displayListener)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        soundPool = SoundPool.Builder().setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).setMaxStreams(3).build()

        dingSoundId = soundPool?.load(context, R.raw.ding, 1)
        popSoundId = soundPool?.load(context, R.raw.pop, 1)

        model.initialFetchError.observe(viewLifecycleOwner) { newException ->
            val builder: AlertDialog.Builder = AlertDialog.Builder(this.requireContext())
            builder.setTitle("Failed to retrieve data from Postroom API")
            builder.setMessage("Your session might have expired, please link your identity again.")
            builder.setPositiveButton("Link identity now") { _: DialogInterface, _: Int ->
                ContextCompat.startActivity(
                    this.requireContext(),
                    Intent(
                        this.requireContext(),
                        SettingsActivity::class.java
                    ).putExtra("link", true),
                    null
                )
            }
            builder.setNegativeButton("Back to home") { _: DialogInterface, _: Int ->
                this.requireActivity().onBackPressed()
            }
            builder.create().show()
        }
        model.cacheData(recipientDataService, courierMatchService)
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    private val model: CameraViewModel by viewModels()

    private var canvas: Boolean = false

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model.uniId.observe(viewLifecycleOwner) { newUniId ->
            // Update the UI, in this case, a TextView.
            if (uniIdLbl != null && uniIdLbl?.text != newUniId) {
                uniIdLbl.text = newUniId ?: resources.getText(R.string.no_id)
            }
        }

        model.uniId.observe(viewLifecycleOwner) {
            evaluateCurrentStatus()
        }

        model.room.observe(viewLifecycleOwner) {
            evaluateCurrentStatus()
        }

        model.room.observe(viewLifecycleOwner) { newRoom ->
            // Update the UI, in this case, a TextView.
            if (detectedRoomLbl != null && detectedRoomLbl?.text != newRoom) {
                detectedRoomLbl.text = newRoom ?: resources.getText(R.string.no_room)
            }
        }

        model.barcodes.observe(viewLifecycleOwner, Observer { num ->
            if (barcodeCount != null) {
                barcodeCount.text = "$num barcode${if (num != 1) "s" else ""} in shot"
            }
        })

        model.allCollectedBarcodes.observe(
            viewLifecycleOwner
        ) { barcodes ->
            handleBarcodeSet(barcodes)

            if (barcodes.isEmpty()) {
                trackingLbl.text = getString(R.string.ocr_status_no_barcode)
            }
        }

        model.uniIds.observe(viewLifecycleOwner) { uniIds ->
            if (dataCount != null) {
                dataCount.text =
                    "${uniIds.size} known resident uni IDs, ${model.courierPatterns.value?.size ?: "?"} courier patterns"
            }
        }

        model.courierPatterns.observe(viewLifecycleOwner) { courierPatterns ->
            if (dataCount != null) {
                dataCount.text =
                    "${model.uniIds.value?.keys?.size ?: "?"} known resident uni IDs, ${courierPatterns.size} courier patterns"
            }
        }

        model.qrId.observe(viewLifecycleOwner) { newQrId ->
            // Update the UI, in this case, a TextView.
            if (qrId != null && qrId?.text != newQrId) {
                qrId.text = newQrId ?: getString(R.string.ocr_status_summary_no_qr)
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }

        model.uniIdBoundingBox.observe(viewLifecycleOwner) { newRect ->
            if (newRect != null && canvas) {
                val canvas = surfaceView.holder.lockCanvas()
                val rect: RectF
                if (isPortraitMode()) {
                    scaleFactorY = surfaceView.height.toFloat() / (model.width.value ?: 1600)
                    scaleFactorX = surfaceView.width.toFloat() / (model.height.value ?: 1200)
                    rect = translateRect(model.uniIdBoundingBox.value!!)

                } else {
                    scaleFactorY = surfaceView.height.toFloat() / (model.height.value ?: 720)
                    scaleFactorX = surfaceView.width.toFloat() / (model.width.value ?: 1280)
                    rect = translateRect(model.uniIdBoundingBox.value!!)
                }
                canvas.drawColor(0, PorterDuff.Mode.CLEAR)
                val cx = rect.left + (rect.right - rect.left) / 2
                val cy = rect.top + (rect.bottom - rect.top) / 2
                val radius = 16.0f
                val paint = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    color = Color.RED
                    strokeWidth = 10f
                }
                canvas.drawCircle(cx, cy, radius, paint)

                surfaceView.holder.unlockCanvasAndPost(canvas);
            } else if (newRect == null && canvas) {
                val canvas = surfaceView.holder.lockCanvas()
                canvas.drawColor(0, PorterDuff.Mode.CLEAR)
                surfaceView.holder.unlockCanvasAndPost(canvas)
            }
        }


        container = view as ConstraintLayout
        viewFinder = container.findViewById(R.id.view_finder)

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        broadcastManager = LocalBroadcastManager.getInstance(view.context)

        // Set up the intent filter that will receive events from our main activity
        val filter = IntentFilter().apply { addAction(KEY_EVENT_ACTION) }
        broadcastManager.registerReceiver(volumeDownReceiver, filter)

        // Every time the orientation of device changes, update rotation for use cases
        displayManager.registerDisplayListener(displayListener, null)

        // Wait for the views to be properly laid out
        viewFinder.post {
            // Keep track of the display in which this view is attached
            displayId = viewFinder.display.displayId
            viewFinder.preferredImplementationMode = PreviewView.ImplementationMode.TEXTURE_VIEW

            // Build UI controls
            updateCameraUi()

            // Bind use cases
            bindCameraUseCases()

            surfaceView.holder.setFormat(PixelFormat.TRANSPARENT)
            surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {

                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    canvas = false
                }

                override fun surfaceCreated(holder: SurfaceHolder) {
                    surfaceView.setZOrderOnTop(true)
                    canvas = true
                }
            })

            bottomStatusBar.setOnClickListener {
                promptForReset()
            }
        }
    }

    private fun promptForReset() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this.requireContext())
        builder.setTitle(getString(R.string.reset_confirmation_title))
        builder.setMessage(getString(R.string.reset_confirmation_body))
        builder.setNegativeButton("Close") { _, _ -> }
        builder.setPositiveButton("Confirm") { _, _ -> doReset() }
        builder.show()
    }

    private fun doReset() {
        model.recipientGuesses.postValue(emptySet())
        model.courierGuess.postValue(null)
        model.uniId.postValue(null)
        model.room.postValue(null)
        model.qrId.postValue(null)
        model.barcodes.postValue(0)
        model.allCollectedBarcodes.postValue(emptySet())
    }

    private fun handleBarcodeSet(barcodes: Set<RecognisedBarcode>) {
        val courierGuess = barcodes.map {
            it to courierMatchService.guessFromBarcode(
                model.courierPatterns.value ?: emptyList(),
                it
            )
        }.firstOrNull { it.second != null }
        if (courier != null) {
            courier.text = "${courierGuess?.second?.courier?.name ?: "No courier guess"}"
        }
        model.courierGuess.postValue(courierGuess?.second?.courier)
        val bestBarcode = courierGuess?.first ?: courierMatchService.excludeRejects(
            model.courierPatterns.value ?: emptyList(), barcodes
        ).firstOrNull()

        if (bestBarcode != null) {
            model.bestBarcode.postValue(bestBarcode)
        }

        if (trackingLbl != null && bestBarcode != null && trackingLbl?.text != bestBarcode.barcode) {
            trackingLbl.text = "Best barcode: " + bestBarcode.barcode
        }
        evaluateCurrentStatus()
    }

    private fun evaluateCurrentStatus() {
        if (statusIndicator != null) {
            var shouldPlayDing = false
            if (model.recipientGuesses.value?.size == 2 && model.recipientGuesses.value!!.distinctBy { it.id }.size == 1) {
                checkKnownRecipient.setImageResource(R.drawable.ic_baseline_check_circle_24)
                shouldPlayDing = true
            } else if (model.recipientGuesses.value?.size == 2 && model.recipientGuesses.value!!.distinctBy { it.id }.size == 2) {
                checkKnownRecipient.setImageResource(R.drawable.ic_baseline_error_24)
            } else if (model.recipientGuesses.value?.size == 1) {
                checkKnownRecipient.setImageResource(R.drawable.ic_baseline_check_circle_white_24)
                shouldPlayDing = true
            } else {
                checkKnownRecipient.setImageResource(R.drawable.ic_baseline_not_interested_24)
            }

            if (shouldPlayDing && checkKnownRecipient.tag != "played" && dingSoundId != null) {
                soundPool?.also {
                    it.play(dingSoundId!!, 1f, 1f, 1, 0, 1f)
                }
                checkKnownRecipient.tag = "played"
            } else if (!shouldPlayDing && model.recipientGuesses.value?.isEmpty() != false) {
                checkKnownRecipient.tag = ""
            }

            var shouldPlayPop = false

            if (model.courierGuess.value != null) {
                checkKnownCourier.setImageResource(R.drawable.ic_baseline_check_circle_24)
                shouldPlayPop = true
            } else {
                checkKnownCourier.setImageResource(R.drawable.ic_baseline_not_interested_24)
            }

            if (shouldPlayPop && popSoundId != null && checkKnownCourier.tag != "played") {
                soundPool?.also {
                    it.play(popSoundId!!, 1f, 1f, 1, 0, 1f)
                }
                checkKnownCourier.tag = "played"
            } else if (!shouldPlayPop) {
                checkKnownCourier.tag = ""
            }

            if (barcodeCount != null) {
                barcodeCount.text = (model.barcodes.value ?: 0).toString() + " barcodes in shot, " + (model.allCollectedBarcodes.value?.size ?: 0).toString() + " total collected"
            }

        }
    }

    /**
     * Inflate camera controls and update the UI manually upon config changes to avoid removing
     * and re-adding the view finder from the view hierarchy; this provides a seamless rotation
     * transition on devices that support it.
     *
     * NOTE: The flag is supported starting in Android 8 but there still is a small flash on the
     * screen for devices that run Android 9 or below.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateCameraUi()
    }

    private fun isPortraitMode(): Boolean {
        val orientation: Int = resources.configuration.orientation
        return orientation == Configuration.ORIENTATION_PORTRAIT
    }

    private var scaleFactorX = 1.0f
    private var scaleFactorY = 1.0f
    private fun translateX(x: Float): Float = x * scaleFactorX
    private fun translateY(y: Float): Float = y * scaleFactorY

    private fun translateRect(rect: Rect) = RectF(
        translateX(rect.left.toFloat()),
        translateY(rect.top.toFloat()),
        translateX(rect.right.toFloat()),
        translateY(rect.bottom.toFloat())
    )

    /** Declare and bind preview, capture and analysis use cases */
    private fun bindCameraUseCases() {

        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        val rotation = viewFinder.display.rotation

        // Bind the CameraProvider to the LifeCycleOwner
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {

            // CameraProvider
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            preview = Preview.Builder()
                // We request aspect ratio but no resolution
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation
                .setTargetRotation(rotation)
                .build()

            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalyzer!!.setAnalyzer(executor1, OcrImageAnalyzer(requireContext(), model))

            // ImageCapture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                // We request aspect ratio but no resolution to match preview config, but letting
                // CameraX optimize for whatever specific resolution best fits our use cases
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(rotation)
                .build()


            // Must unbind the use-cases before rebinding them
            cameraProvider.unbindAll()

            try {
                // A variable number of use-cases can be passed here -
                // camera provides access to CameraControl & CameraInfo
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )
                // Attach the viewfinder's surface provider to preview use case
                preview?.setSurfaceProvider(viewFinder.createSurfaceProvider(camera?.cameraInfo))
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /**
     *  [androidx.camera.core.ImageAnalysisConfig] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }


    private val jsonIgnoreKeysInstance = Json { ignoreUnknownKeys = true }

    /** Method used to re-draw the camera UI controls, called every time configuration changes. */
    private fun updateCameraUi() {

        // Remove previous UI if any
        container.findViewById<ConstraintLayout>(R.id.camera_ui_container)?.let {
            container.removeView(it)
        }

        // Inflate a new view containing all UI for controlling the camera
        val controls = View.inflate(requireContext(), R.layout.camera_ui_container, container)

        // Listener for button used to capture photo
        controls.findViewById<ImageButton>(R.id.camera_capture_button).setOnClickListener {
            view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            val addPhotoBottomDialogFragment: AddPhotoBottomDialogFragment = AddPhotoBottomDialogFragment.newInstance(sscPersistenceService, baseUrl, model, recipientDataService, itemService)

            val vfChild = viewFinder.getChildAt(0)
            if (vfChild is TextureView && checkPrerequisitesForAddDialog()) {
                preview?.setSurfaceProvider(null)
                imageAnalyzer?.clearAnalyzer()
                val bitmap = vfChild.getBitmap(viewFinder.width, viewFinder.height)
                addPhotoBottomDialogFragment.setBitmap(bitmap!!)
                addPhotoBottomDialogFragment.show(
                    parentFragmentManager,
                    "add_photo_dialog_fragment"
                )
            }


            addPhotoBottomDialogFragment.setOnDismissCallback {
                preview?.setSurfaceProvider(viewFinder.createSurfaceProvider(camera?.cameraInfo))
                imageAnalyzer!!.setAnalyzer(executor1, OcrImageAnalyzer(requireContext(), model))
            }

            addPhotoBottomDialogFragment.setOnSuccessCallback { item ->
                doReset()
                if (item.recipient?.accommodationBlock?.hub == null) {
                    val confetti = controls.findViewById<KonfettiView>(R.id.viewKonfetti)
                    confetti.build()
                        .addColors(
                            Color.parseColor("#1e90ff"),
                            Color.parseColor("#6b8e23"),
                            Color.parseColor("#ffd700"),
                            Color.parseColor("#ffc0cb"),
                            Color.parseColor("#6a5acd"),
                            Color.parseColor("#add8e6"),
                            Color.parseColor("#ee82ee"),
                            Color.parseColor("#98fb98"),
                            Color.parseColor("#4682b4"),
                            Color.parseColor("#f4a460"),
                            Color.parseColor("#d2691e"),
                            Color.parseColor("#dc143c")
                        )
                        .setDirection(0.0, 359.0)
                        .setSpeed(5f, 5f)
                        .setTimeToLive(5000L)
                        .addShapes(Shape.Square, Shape.Circle)
                        .addSizes(nl.dionsegijn.konfetti.models.Size(12))
                        .setPosition(-50f, confetti.width + 50f, -50f, -50f)
                        .streamFor(900, 500L)
                    return@setOnSuccessCallback
                }
                val bundle = HubInformationFragment.newBundle(item.recipient?.accommodationBlock?.hub!!)

                activity?.runOnUiThread {
                    Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                        R.id.action_camera_to_hub_information, bundle
                    )
                }
            }

            addPhotoBottomDialogFragment.setOnFailureCallback {
                var err = ""
                if (it.component2()!!.response.data.isNotEmpty()) {
                    err = it.component2()!!.response.body().toByteArray().decodeToString()
                    try {
                        val itemAdditionErr = jsonIgnoreKeysInstance.decodeFromString<ItemAdditionError>(err)
                        err = itemAdditionErr.error
                    } catch (e: Exception) {}
                } else {
                    err = "Something went wrong"
                }

                activity?.runOnUiThread {
                    val builder: AlertDialog.Builder = AlertDialog.Builder(this.requireContext())
                    builder.setTitle(getString(R.string.item_addition_error_title))
                    builder.setMessage(err)
                    builder.setNegativeButton("Close"){ _, _ -> promptForReset()}
                    builder.show()
                }
            }

        }

    }

    private fun checkPrerequisitesForAddDialog(): Boolean {
        val ready = model.couriers.value?.any() == true && model.qrId.value?.isNotEmpty() == true
        if (!ready) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this.requireContext())
            builder.setTitle(getString(R.string.pre_requisites_failed_title))
            builder.setMessage(getString(R.string.pre_requisites_failed_body))
            builder.setNegativeButton("Close"){ _, _ -> }
            builder.show()
        }
        return ready
    }


    companion object {

        private const val TAG = "CameraXBasic"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}
