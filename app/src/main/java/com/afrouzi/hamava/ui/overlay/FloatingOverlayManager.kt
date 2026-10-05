/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.ui.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.afrouzi.hamava.MainActivity
import com.afrouzi.hamava.R
import com.afrouzi.hamava.data.model.AudioSourceType
import com.afrouzi.hamava.data.model.DubSettings
import com.afrouzi.hamava.data.model.DubStatus
import kotlin.math.abs

class FloatingOverlayManager(
    private val context: Context,
    private val onToggleDubbing: () -> Unit,
    private val onVolumeChanged: (Float) -> Unit
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlayRootView: FrameLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    // UI Elements
    private var bubbleContainer: FrameLayout? = null
    private var bubbleGlowView: View? = null
    private var bubbleIcon: ImageView? = null
    private var panelContainer: LinearLayout? = null
    private var statusBadge: TextView? = null
    private var latencyBadge: TextView? = null
    private var subtitleTicker: TextView? = null
    private var playPauseButton: TextView? = null
    private var volumeSlider: SeekBar? = null
    private var volumePercentText: TextView? = null

    private var isExpanded = false
    private var isOverlayVisible = false

    private var screenWidth = 1080
    private var screenHeight = 1920

    // Touch & Drag state
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    private val tealColor = Color.parseColor("#00D4AA")
    private val purpleColor = Color.parseColor("#6750A4")
    private val darkSurfaceColor = Color.parseColor("#1C1C2E")
    private val darkBgColor = Color.parseColor("#0F0F1A")
    private val cardBorderColor = Color.parseColor("#2D2D44")

    init {
        updateScreenDimensions()
    }

    private fun updateScreenDimensions() {
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(dm)
        screenWidth = dm.widthPixels
        screenHeight = dm.heightPixels
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        ).toInt()
    }

    fun isPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun show(settings: DubSettings) {
        if (!isPermissionGranted() || isOverlayVisible) return

        try {
            updateScreenDimensions()

            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = screenWidth - dpToPx(76f)
                y = screenHeight / 3
            }

            buildViewHierarchy(settings)

            overlayRootView?.let { root ->
                windowManager.addView(root, layoutParams)
                isOverlayVisible = true
                Log.d("HamAva", "FloatingOverlayManager: Overlay attached successfully")
            }
        } catch (e: Exception) {
            Log.e("HamAva", "Failed to show floating overlay: ${e.localizedMessage}", e)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildViewHierarchy(settings: DubSettings) {
        val root = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // --- 1. Minimized Bubble ---
        val bubbleSize = dpToPx(56f)
        val bubble = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(bubbleSize, bubbleSize)
        }

        // Outer glow
        bubbleGlowView = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(bubbleSize, bubbleSize)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(purpleColor)
                setStroke(dpToPx(2.5f), tealColor)
            }
        }
        bubble.addView(bubbleGlowView)

        // Center mic icon
        bubbleIcon = ImageView(context).apply {
            val iconPad = dpToPx(12f)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(iconPad, iconPad, iconPad, iconPad)
            setImageResource(R.drawable.ic_tile_mic)
            setColorFilter(Color.WHITE)
        }
        bubble.addView(bubbleIcon)

        // Touch listener for dragging & click
        bubble.setOnTouchListener { _, event ->
            handleBubbleTouch(event)
        }

        bubbleContainer = bubble
        root.addView(bubble)

        // --- 2. Expanded Control Panel ---
        val panelWidth = dpToPx(250f)
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            visibility = View.GONE
            val pad = dpToPx(14f)
            setPadding(pad, pad, pad, pad)
            layoutParams = FrameLayout.LayoutParams(panelWidth, FrameLayout.LayoutParams.WRAP_CONTENT)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(16f).toFloat()
                setColor(darkSurfaceColor)
                setStroke(dpToPx(1.5f), cardBorderColor)
            }
        }

        // Header: App name + Close
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val titleText = TextView(context).apply {
            text = "همآوا • دوبله زنده"
            setTextColor(tealColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerRow.addView(titleText)

        val closeBtn = TextView(context).apply {
            text = "✕"
            setTextColor(Color.LTGRAY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            gravity = Gravity.CENTER
            val hitSize = dpToPx(32f)
            layoutParams = LinearLayout.LayoutParams(hitSize, hitSize)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#25FFFFFF"))
            }
            setOnClickListener { collapseToBubble() }
        }
        headerRow.addView(closeBtn)
        panel.addView(headerRow)

        // Status Row (Status Text + Latency pill)
        val statusRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val mTop = dpToPx(8f)
            setPadding(0, mTop, 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        statusBadge = TextView(context).apply {
            text = "● آماده"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        statusRow.addView(statusBadge)

        latencyBadge = TextView(context).apply {
            text = "-- ms"
            setTextColor(tealColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            val p = dpToPx(4f)
            setPadding(dpToPx(6f), p, dpToPx(6f), p)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(8f).toFloat()
                setColor(darkBgColor)
            }
        }
        statusRow.addView(latencyBadge)
        panel.addView(statusRow)

        // Source & Target Language indicator
        val langRow = TextView(context).apply {
            val sourceName = if (settings.audioSource == AudioSourceType.MIC) "میکروفون" else "سیستم"
            text = "$sourceName ➔ ${settings.targetLanguage.nameFa}"
            setTextColor(Color.parseColor("#A0A0C0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            val mTop = dpToPx(6f)
            setPadding(0, mTop, 0, 0)
        }
        panel.addView(langRow)

        // Subtitle Ticker (if enabled)
        subtitleTicker = TextView(context).apply {
            text = "در انتظار گفتار..."
            setTextColor(Color.parseColor("#E0E0FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            val mTop = dpToPx(8f)
            setPadding(dpToPx(8f), dpToPx(6f), dpToPx(8f), dpToPx(6f))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(8f).toFloat()
                setColor(Color.parseColor("#151528"))
            }
            visibility = if (settings.enableSubtitles) View.VISIBLE else View.GONE
        }
        panel.addView(subtitleTicker)

        // Volume row
        val volHeaderRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            val mTop = dpToPx(8f)
            setPadding(0, mTop, 0, 0)
        }
        val volLabel = TextView(context).apply {
            text = "صدای دوبله:"
            setTextColor(Color.LTGRAY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        volHeaderRow.addView(volLabel)

        volumePercentText = TextView(context).apply {
            text = "${(settings.dubVolumeRatio * 100).toInt()}%"
            setTextColor(tealColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }
        volHeaderRow.addView(volumePercentText)
        panel.addView(volHeaderRow)

        volumeSlider = SeekBar(context).apply {
            max = 150
            progress = (settings.dubVolumeRatio * 100).toInt().coerceIn(10, 150)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val ratio = progress / 100f
                        volumePercentText?.text = "$progress%"
                        onVolumeChanged(ratio)
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        panel.addView(volumeSlider)

        // Bottom action buttons: Toggle Dubbing + Open App
        val actionRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            val mTop = dpToPx(10f)
            setPadding(0, mTop, 0, 0)
        }

        playPauseButton = TextView(context).apply {
            text = "توقف"
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            val p = dpToPx(8f)
            setPadding(p, p, p, p)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(10f).toFloat()
                setColor(purpleColor)
            }
            setOnClickListener {
                onToggleDubbing()
            }
        }
        actionRow.addView(playPauseButton)

        val spacer = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(8f), 1)
        }
        actionRow.addView(spacer)

        val openAppBtn = TextView(context).apply {
            text = "برنامه"
            setTextColor(tealColor)
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            val p = dpToPx(8f)
            setPadding(p, p, p, p)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(10f).toFloat()
                setColor(darkBgColor)
                setStroke(dpToPx(1f), tealColor)
            }
            setOnClickListener {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
                context.startActivity(intent)
                collapseToBubble()
            }
        }
        actionRow.addView(openAppBtn)
        panel.addView(actionRow)

        panelContainer = panel
        root.addView(panel)

        overlayRootView = root
    }

    private fun handleBubbleTouch(event: MotionEvent): Boolean {
        val params = layoutParams ?: return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - initialTouchX).toInt()
                val dy = (event.rawY - initialTouchY).toInt()
                if (abs(dx) > 10 || abs(dy) > 10) {
                    isDragging = true
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager.updateViewLayout(overlayRootView, params)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    // Tap clicked!
                    expandToPanel()
                } else {
                    // Snap to closest screen edge (Left or Right)
                    snapToEdge()
                }
                return true
            }
        }
        return false
    }

    private fun snapToEdge() {
        val params = layoutParams ?: return
        val currentX = params.x
        val targetX = if (currentX + dpToPx(28f) < screenWidth / 2) dpToPx(8f) else screenWidth - dpToPx(64f)

        val animator = ValueAnimator.ofInt(currentX, targetX).apply {
            duration = 200
            interpolator = OvershootInterpolator(0.8f)
            addUpdateListener { va ->
                params.x = va.animatedValue as Int
                windowManager.updateViewLayout(overlayRootView, params)
            }
        }
        animator.start()
    }

    private fun expandToPanel() {
        if (isExpanded) return
        isExpanded = true
        bubbleContainer?.visibility = View.GONE
        panelContainer?.visibility = View.VISIBLE

        layoutParams?.let { params ->
            params.x = (screenWidth - dpToPx(260f)).coerceAtLeast(dpToPx(16f)) / 2
            windowManager.updateViewLayout(overlayRootView, params)
        }
    }

    private fun collapseToBubble() {
        if (!isExpanded) return
        isExpanded = false
        panelContainer?.visibility = View.GONE
        bubbleContainer?.visibility = View.VISIBLE

        layoutParams?.let { params ->
            params.x = screenWidth - dpToPx(64f)
            windowManager.updateViewLayout(overlayRootView, params)
        }
    }

    fun updateStatus(status: DubStatus) {
        mainHandler.post {
            val (text, color) = when (status) {
                DubStatus.IDLE -> "● آماده" to Color.LTGRAY
                DubStatus.CONNECTING -> "● در حال اتصال..." to tealColor
                DubStatus.ACTIVE_LISTENING -> "● در حال شنیدن..." to tealColor
                DubStatus.ACTIVE_SPEAKING -> "● در حال دوبله..." to Color.parseColor("#4CAF50")
                DubStatus.ERROR -> "● خطا در اتصال" to Color.parseColor("#CF6679")
                DubStatus.PAUSED -> "● متوقف" to Color.YELLOW
            }

            statusBadge?.text = text
            statusBadge?.setTextColor(color)

            playPauseButton?.text = if (status == DubStatus.IDLE || status == DubStatus.ERROR) "شروع" else "توقف"

            // Glow ring animation when speaking
            (bubbleGlowView?.background as? GradientDrawable)?.apply {
                val strokeColor = if (status == DubStatus.ACTIVE_SPEAKING) Color.parseColor("#00FFAA") else tealColor
                setStroke(dpToPx(2.5f), strokeColor)
            }
        }
    }

    fun updateLatency(latencyMs: Long) {
        mainHandler.post {
            latencyBadge?.text = "$latencyMs ms"
        }
    }

    fun updateSubtitle(text: String) {
        if (text.isNotBlank()) {
            mainHandler.post {
                subtitleTicker?.text = text
            }
        }
    }

    fun hide() {
        mainHandler.post {
            if (!isOverlayVisible || overlayRootView == null) return@post
            try {
                windowManager.removeView(overlayRootView)
                isOverlayVisible = false
                overlayRootView = null
                Log.d("HamAva", "FloatingOverlayManager: Overlay removed cleanly")
            } catch (e: Exception) {
                Log.w("HamAva", "Error removing floating overlay: ${e.localizedMessage}")
            }
        }
    }

    fun isVisible(): Boolean = isOverlayVisible
}
