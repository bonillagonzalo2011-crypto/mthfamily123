package com.mthfamily.app

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*

class MainActivity : Activity() {
    private lateinit var dot: View

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val d = resources.displayMetrics.density
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0B0B10"))
            setPadding((24 * d).toInt(), 0, (24 * d).toInt(), 0)
        }
        val title = TextView(this).apply {
            text = "MTH FAMILY"
            setTextColor(Color.WHITE)
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        val btn = Button(this).apply {
            text = "ACTIVAR FLOTANTE"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FF2D55"))
                cornerRadius = 10 * d
            }
            setOnClickListener { activar() }
        }
        dot = View(this)
        root.addView(title)
        root.addView(btn, LinearLayout.LayoutParams(-1, (48 * d).toInt()).apply { topMargin = (18 * d).toInt() })
        root.addView(dot, LinearLayout.LayoutParams((12 * d).toInt(), (12 * d).toInt()).apply {
            topMargin = (10 * d).toInt()
            gravity = Gravity.CENTER_HORIZONTAL
        })
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        val ok = Settings.canDrawOverlays(this)
        dot.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(if (ok) "#2ECC71" else "#FF2D55"))
        }
    }

    private fun activar() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } else {
            startForegroundService(Intent(this, OverlayService::class.java))
            moveTaskToBack(true)
        }
    }
}

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var bubble: View
    private lateinit var panel: View
    private lateinit var bp: WindowManager.LayoutParams
    private lateinit var pp: WindowManager.LayoutParams
    private var d = 1f

    override fun onBind(i: Intent?): IBinder? = null
    override fun onStartCommand(i: Intent?, f: Int, s: Int) = START_STICKY

    override fun onCreate() {
        super.onCreate()
        d = resources.displayMetrics.density
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("mth", "MTH FAMILY", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "mth")
            .setContentTitle("MTH FAMILY")
            .setContentText("Botón flotante activo")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }

        bp = params((65 * d).toInt(), (65 * d).toInt()).apply { x = (16 * d).toInt(); y = (200 * d).toInt() }
        pp = params((250 * d).toInt(), (300 * d).toInt())

        bubble = TextView(this).apply {
            text = "MTH\nFAMILY"
            setTextColor(Color.WHITE)
            textSize = 8f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1E88E5"))
                setStroke((3 * d).toInt(), Color.WHITE)
            }
        }
        arrastrar(bubble, bp, { abrir() }, { stopSelf() })

        val bar = LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#1E88E5"))
            gravity = Gravity.CENTER_VERTICAL
            setPadding((16 * d).toInt(), (10 * d).toInt(), (8 * d).toInt(), (10 * d).toInt())
            addView(TextView(this@OverlayService).apply {
                text = "MTH FAMILY"
                setTextColor(Color.WHITE)
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(TextView(this@OverlayService).apply {
                text = "—"
                setTextColor(Color.WHITE)
                textSize = 20f
                gravity = Gravity.CENTER
                setOnClickListener { cerrar() }
            }, LinearLayout.LayoutParams((40 * d).toInt(), -2))
        }
        panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#121820"))
                cornerRadius = 18 * d
            }
            clipToOutline = true
            addView(bar, LinearLayout.LayoutParams(-1, -2))
        }
        arrastrar(panel, pp, null, null)
        wm.addView(bubble, bp)
    }

    private fun params(w: Int, h: Int) = WindowManager.LayoutParams(
        w, h,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START }

    private fun arrastrar(v: View, p: WindowManager.LayoutParams, tap: (() -> Unit)?, largo: (() -> Unit)?) {
        var sx = 0f; var sy = 0f; var ox = 0; var oy = 0; var moved = false
        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; ox = p.x; oy = p.y; moved = false }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - sx
                    val dy = e.rawY - sy
                    if (Math.abs(dx) + Math.abs(dy) > 12 * d) moved = true
                    if (moved) {
                        p.x = (ox + dx).toInt()
                        p.y = (oy + dy).toInt()
                        wm.updateViewLayout(v, p)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        if (largo != null && e.eventTime - e.downTime > 800) largo() else tap?.invoke()
                    }
                }
            }
            true
        }
    }

    private fun abrir() {
        pp.x = bp.x
        pp.y = bp.y
        wm.removeView(bubble)
        wm.addView(panel, pp)
    }

    private fun cerrar() {
        bp.x = pp.x
        bp.y = pp.y
        wm.removeView(panel)
        wm.addView(bubble, bp)
    }

    override fun onDestroy() {
        try { wm.removeView(bubble) } catch (e: Exception) {}
        try { wm.removeView(panel) } catch (e: Exception) {}
        super.onDestroy()
    }
}
