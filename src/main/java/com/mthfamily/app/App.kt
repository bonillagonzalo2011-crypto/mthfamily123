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

private const val PREFS = "mth"
private const val KEY_FLOAT = "float_on"
private const val KEY_APP = "app_pkg"

class IconView(ctx: Context, private val kind: Int) : View(ctx) {
    var slash = false
        set(v) { field = v; invalidate() }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val s = minOf(w, h)
        p.strokeWidth = s * 0.08f
        val cx = w / 2
        val cy = h / 2
        when (kind) {
            0 -> {
                val big = Path().apply { addCircle(cx, cy, s * 0.30f, Path.Direction.CW) }
                val cut = Path().apply { addCircle(cx + s * 0.16f, cy - s * 0.12f, s * 0.26f, Path.Direction.CW) }
                big.op(cut, Path.Op.DIFFERENCE)
                c.drawPath(big, p)
                if (slash) c.drawLine(cx - s * 0.32f, cy - s * 0.32f, cx + s * 0.32f, cy + s * 0.32f, p)
            }
            1 -> {
                c.drawCircle(cx, cy, s * 0.13f, p)
                c.drawCircle(cx, cy, s * 0.26f, p)
                for (i in 0 until 8) {
                    c.save()
                    c.rotate(i * 45f, cx, cy)
                    c.drawLine(cx, cy - s * 0.26f, cx, cy - s * 0.36f, p)
                    c.restore()
                }
            }
            2 -> {
                c.drawLine(cx + s * 0.10f, cy - s * 0.25f, cx - s * 0.12f, cy, p)
                c.drawLine(cx - s * 0.12f, cy, cx + s * 0.10f, cy + s * 0.25f, p)
            }
            else -> {
                c.drawLine(cx, cy - s * 0.28f, cx, cy + s * 0.28f, p)
                c.drawLine(cx - s * 0.28f, cy, cx + s * 0.28f, cy, p)
            }
        }
    }
}

class MainActivity : Activity() {
    private lateinit var main: FrameLayout
    private lateinit var settings: FrameLayout
    private lateinit var title: TextView
    private lateinit var dot: View
    private lateinit var moon: IconView
    private lateinit var toggle: Button
    private lateinit var shortName: TextView
    private lateinit var abrirBtn: Button
    private var dark = false
    private var d = 1f
    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }

    private fun box(color: String, r: Float) = GradientDrawable().apply {
        setColor(Color.parseColor(color))
        cornerRadius = r * d
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        d = resources.displayMetrics.density
        val root = FrameLayout(this)
        main = FrameLayout(this)
        settings = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            visibility = View.GONE
        }
        root.addView(main, FrameLayout.LayoutParams(-1, -1))
        root.addView(settings, FrameLayout.LayoutParams(-1, -1))

        val m = (16 * d).toInt()
        val sz = (56 * d).toInt()

        moon = IconView(this, 0).apply {
            slash = true
            background = box("#FF2D55", 16f)
            setOnClickListener { dark = !dark; applyTheme() }
        }
        main.addView(moon, FrameLayout.LayoutParams(sz, sz, Gravity.TOP or Gravity.START).apply { setMargins(m, m, 0, 0) })

        val gear = IconView(this, 1).apply {
            background = box("#FF2D55", 16f)
            setOnClickListener { settings.visibility = View.VISIBLE }
        }
        main.addView(gear, FrameLayout.LayoutParams(sz, sz, Gravity.TOP or Gravity.END).apply { setMargins(0, m, m, 0) })

        val center = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val logoId = resources.getIdentifier("logo", "drawable", packageName)
        if (logoId != 0) {
            center.addView(ImageView(this).apply { setImageResource(logoId) },
                LinearLayout.LayoutParams((90 * d).toInt(), (90 * d).toInt()))
        }
        title = TextView(this).apply {
            text = "TEAM FAMILI"
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD_ITALIC)
            letterSpacing = 0.08f
            alpha = 0.4f
            setShadowLayer(1f, 4f, 4f, Color.parseColor("#FF2D55"))
        }
        center.addView(title)

        toggle = Button(this).apply {
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
            setPadding((16 * d).toInt(), 0, (16 * d).toInt(), 0)
            background = box("#FF2D55", 10f)
            setOnClickListener { setFloat(!prefs.getBoolean(KEY_FLOAT, false)) }
        }
        center.addView(toggle, LinearLayout.LayoutParams((280 * d).toInt(), (44 * d).toInt())
            .apply { topMargin = (18 * d).toInt() })

        dot = View(this)
        center.addView(dot, LinearLayout.LayoutParams((12 * d).toInt(), (12 * d).toInt())
            .apply { topMargin = (12 * d).toInt() })
        main.addView(center, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL)
            .apply { topMargin = (80 * d).toInt() })

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val plus = IconView(this, 3).apply {
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke((3 * d).toInt(), Color.WHITE)
                cornerRadius = 16 * d
            }
            setOnClickListener { elegirApp() }
        }
        row.addView(plus, LinearLayout.LayoutParams((64 * d).toInt(), (64 * d).toInt()))
        shortName = TextView(this).apply {
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setPadding((14 * d).toInt(), 0, 0, 0)
        }
        row.addView(shortName, LinearLayout.LayoutParams(0, -2, 1f))
        bottom.addView(row, LinearLayout.LayoutParams(-1, -2))

        abrirBtn = Button(this).apply {
            text = "ABRIR"
            setTextColor(Color.WHITE)
            background = box("#FF2D55", 10f)
            setOnClickListener { abrirApp() }
        }
        bottom.addView(abrirBtn, LinearLayout.LayoutParams((200 * d).toInt(), (44 * d).toInt())
            .apply { topMargin = (14 * d).toInt() })
        main.addView(bottom, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM)
            .apply { setMargins(m, 0, m, (28 * d).toInt()) })

        val back = IconView(this, 2).apply { setOnClickListener { settings.visibility = View.GONE } }
        settings.addView(back, FrameLayout.LayoutParams(sz, sz, Gravity.TOP or Gravity.START).apply { setMargins(m, m, 0, 0) })

        setContentView(root)
        applyTheme()
    }

    private fun applyTheme() {
        main.setBackgroundColor(if (dark) Color.parseColor("#0B0B10") else Color.WHITE)
        val fg = if (dark) Color.WHITE else Color.parseColor("#111114")
        title.setTextColor(fg)
        shortName.setTextColor(fg)
        moon.slash = !dark
    }

    private fun setFloat(on: Boolean) {
        if (on && !Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        prefs.edit().putBoolean(KEY_FLOAT, on).apply()
        val svc = Intent(this, OverlayService::class.java)
        if (on) startForegroundService(svc) else stopService(svc)
        refresh()
    }

    private fun refresh() {
        var on = prefs.getBoolean(KEY_FLOAT, false)
        val ok = Settings.canDrawOverlays(this)
        if (on && !ok) {
            on = false
            prefs.edit().putBoolean(KEY_FLOAT, false).apply()
        }
        toggle.text = if (on) "BOTÓN FLOTANTE: ON" else "BOTÓN FLOTANTE: OFF"
        dot.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(if (ok) "#2ECC71" else "#FF2D55"))
        }
        val pkg = prefs.getString(KEY_APP, null)
        val label = pkg?.let {
            try {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(it, 0)).toString()
            } catch (e: Exception) {
                null
            }
        }
        shortName.text = label ?: "Ninguna app elegida"
        abrirBtn.visibility = if (label != null) View.VISIBLE else View.GONE
    }

    private fun elegirApp() {
        val pm = packageManager
        val apps = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        )
            .map { it.loadLabel(pm).toString() to it.activityInfo.packageName }
            .distinctBy { it.second }
            .sortedBy { it.first.lowercase() }
        AlertDialog.Builder(this)
            .setTitle("Elegí una app")
            .setItems(apps.map { it.first }.toTypedArray()) { _, i ->
                prefs.edit().putString(KEY_APP, apps[i].second).apply()
                refresh()
            }
            .show()
    }

    private fun abrirApp() {
        val pkg = prefs.getString(KEY_APP, null) ?: return
        packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (settings.visibility == View.VISIBLE) settings.visibility = View.GONE else super.onBackPressed()
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
        nm.createNotificationChannel(NotificationChannel("mth", "TEAM FAMILI", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "mth")
            .setContentTitle("TEAM FAMILI")
            .setContentText("Botón flotante activo")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }

        bp = params((65 * d).toInt(), (65 * d).toInt()).apply {
            x = (16 * d).toInt()
            y = (200 * d).toInt()
        }
        pp = params((250 * d).toInt(), (300 * d).toInt())

        bubble = TextView(this).apply {
            text = "MTH\nFAMILY"
            setTextColor(Color.WHITE)
            textSize = 7.5f
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
                text = "TEAM FAMILI"
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

        try {
            wm.addView(bubble, bp)
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private fun params(w: Int, h: Int) = WindowManager.LayoutParams(
        w, h,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START }

    private fun arrastrar(v: View, p: WindowManager.LayoutParams, tap: (() -> Unit)?, largo: (() -> Unit)?) {
        var sx = 0f
        var sy = 0f
        var ox = 0
        var oy = 0
        var moved = false
        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    sx = e.rawX; sy = e.rawY
                    ox = p.x; oy = p.y
                    moved = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - sx
                    val dy = e.rawY - sy
                    if (Math.abs(dx) + Math.abs(dy) > 12 * d) moved = true
                    if (moved) {
                        p.x = (ox + dx).toInt()
                        p.y = (oy + dy).toInt()
                        try { wm.updateViewLayout(v, p) } catch (ex: Exception) {}
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
        try {
            wm.removeView(bubble)
            wm.addView(panel, pp)
        } catch (e: Exception) {}
    }

    private fun cerrar() {
        bp.x = pp.x
        bp.y = pp.y
        try {
            wm.removeView(panel)
            wm.addView(bubble, bp)
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_FLOAT, false).apply()
        try { wm.removeView(bubble) } catch (e: Exception) {}
        try { wm.removeView(panel) } catch (e: Exception) {}
        super.onDestroy()
    }
}
