package com.autobox.autoboxlauncher.signs

import android.content.Context
import android.graphics.Bitmap
import com.polishsigns.SignClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SignsRepository(private val context: Context, private val scope: CoroutineScope) {

    // Guards classifier access between the camera executor thread and the main thread (close()).
    private val classifierLock = Any()
    private var classifier: SignClassifier? = null

    private val _speedLimitSign = MutableStateFlow("")
    val speedLimitSign: StateFlow<String> = _speedLimitSign.asStateFlow()

    private val _otherSign = MutableStateFlow("")
    val otherSign: StateFlow<String> = _otherSign.asStateFlow()

    // Debounce state — accessed only from the single-threaded camera executor.
    private var pendingCode: String? = null
    private var pendingCount: Int = 0

    private var speedLimitTtlJob: Job? = null
    private var otherSignTtlJob: Job? = null

    /**
     * Called from the camera background executor with each captured frame.
     *
     * Crops the center square of the frame, runs the TFLite classifier, and applies
     * debounce filtering: the same sign must be detected in [DEBOUNCE_FRAMES] consecutive
     * frames with sufficient confidence and margin before being confirmed.
     *
     * Confirmed signs are emitted to [speedLimitSign] or [otherSign] and expire
     * after [SIGN_TTL_MS] if not re-seen.
     */
    fun recognizeSigns(bitmap: Bitmap) {
        val cropped = centerCrop(bitmap)
        val topN = synchronized(classifierLock) {
            // Lazy-init classifier; returns null if already closed.
            val cls = classifier ?: SignClassifier(context).also { classifier = it }
            cls.classifyTopN(cropped, n = 2)
        }
        val top1 = topN.getOrNull(0) ?: return
        val top2 = topN.getOrNull(1)

        val margin = top1.confidence - (top2?.confidence ?: 0f)
        if (top1.confidence < CONFIDENCE_THRESHOLD || margin < MARGIN_THRESHOLD) {
            pendingCode = null
            pendingCount = 0
            return
        }

        val code = top1.label.polishCode
        val drawable = SIGN_DRAWABLE_MAP[code] ?: run {
            // Detected class has no drawable in this project — reset debounce.
            pendingCode = null
            pendingCount = 0
            return
        }

        if (code == pendingCode) pendingCount++ else { pendingCode = code; pendingCount = 1 }
        if (pendingCount < DEBOUNCE_FRAMES) return

        if (SPEED_LIMIT_CODES.contains(code)) {
            _speedLimitSign.value = drawable
            speedLimitTtlJob?.cancel()
            speedLimitTtlJob = scope.launch {
                delay(SIGN_TTL_MS)
                _speedLimitSign.value = ""
            }
        } else {
            _otherSign.value = drawable
            otherSignTtlJob?.cancel()
            otherSignTtlJob = scope.launch {
                delay(SIGN_TTL_MS)
                _otherSign.value = ""
            }
        }
    }

    fun close() {
        speedLimitTtlJob?.cancel()
        otherSignTtlJob?.cancel()
        synchronized(classifierLock) {
            classifier?.close()
            classifier = null
        }
    }

    private fun centerCrop(src: Bitmap): Bitmap {
        val side = (minOf(src.width, src.height) * CROP_FRACTION).toInt()
        val x = (src.width - side) / 2
        val y = (src.height - side) / 2
        return Bitmap.createBitmap(src, x, y, side, side)
    }

    companion object {
        private const val CONFIDENCE_THRESHOLD = 0.90f  // minimum top-1 confidence
        private const val MARGIN_THRESHOLD = 0.40f      // required gap between 1st and 2nd class
        private const val DEBOUNCE_FRAMES = 4           // consecutive detections before confirming
        private const val SIGN_TTL_MS = 5_000L          // sign expires after 5 s without re-detection
        private const val CROP_FRACTION = 0.6f          // fraction of the shorter side used for crop

        // Codes that should be shown in the speed-limit slot rather than the warning slot.
        private val SPEED_LIMIT_CODES = setOf(
            "B-33(20)", "B-33(30)", "B-33(50)", "B-33(60)",
            "B-33(70)", "B-33(80)", "B-33(100)", "B-33(120)", "B-34",
        )

        // Maps polishCode (from SignLabel) -> drawable resource name in res/drawable/.
        // Only signs that have a matching image in this project are included;
        // model classes without a drawable are silently ignored.
        private val SIGN_DRAWABLE_MAP: Map<String, String> = mapOf(
            // Speed limits
            "B-33(20)"  to "p1_speed_limit_20",
            "B-33(30)"  to "p1_speed_limit_30",
            "B-33(50)"  to "p1_speed_limit_50",
            "B-33(60)"  to "p1_speed_limit_60",
            "B-33(70)"  to "p1_speed_limit_70",
            "B-33(80)"  to "p1_speed_limit_80",
            "B-33(100)" to "p1_speed_limit_100",
            "B-33(120)" to "p1_speed_limit_120",
            "B-34"      to "p1_end_all_restrictions",  // end of speed limit / end of all prohibitions
            // Warning and regulatory signs
            "B-25"      to "p2_no_overtaking",
            "A-6a"      to "p2_give_way_oncoming",
            "D-1"       to "p2_priority_road",
            "A-7"       to "p2_yield",
            "B-2"       to "p2_no_entry",
            "A-30"      to "p2_danger",
            "A-2"       to "p2_curve_left",
            "A-1"       to "p2_curve_right",
            "A-3"       to "p2_double_curve_left",
            "A-11"      to "p2_uneven_road",
            "A-18"      to "p2_slippery_road",
            "A-14"      to "p2_road_works",
            "A-29"      to "p2_traffic_light_ahead",
            "A-16"      to "p2_crosswalk",
            "A-17"      to "p2_children",
            "C-12"      to "p2_roundabout_ahead",
        )
    }
}
