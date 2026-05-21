package com.example.nammametrosahaya

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.example.nammametrosahaya.data.DataLoader
import com.example.nammametrosahaya.data.Station

class MetroCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    interface OnStationClickListener {
        fun onStationClicked(station: Station)
    }

    var onStationClickListener: OnStationClickListener? = null
    var highlightedStations: List<String> = emptyList()

    private val network   = DataLoader.loadNetwork(context)
    private val stations  = network.stations

    private val paintPurpleLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color       = Color.parseColor("#7B2D8B")
        strokeWidth = 10f
        style       = Paint.Style.STROKE
        strokeCap   = Paint.Cap.ROUND
        strokeJoin  = Paint.Join.ROUND
    }

    private val paintGreenLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color       = Color.parseColor("#2E7D32")
        strokeWidth = 10f
        style       = Paint.Style.STROKE
        strokeCap   = Paint.Cap.ROUND
        strokeJoin  = Paint.Join.ROUND
    }

    private val paintHighlight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color       = Color.parseColor("#FF6F00")
        strokeWidth = 14f
        style       = Paint.Style.STROKE
        strokeCap   = Paint.Cap.ROUND
        strokeJoin  = Paint.Join.ROUND
    }

    private val paintStationDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val paintStationBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color       = Color.WHITE
        strokeWidth = 3f
        style       = Paint.Style.STROKE
    }

    private val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = Color.parseColor("#1A1A1A")
        textSize  = 22f
        textAlign = Paint.Align.CENTER
    }

    private val paintInterchangeLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = Color.parseColor("#7B2D8B")
        textSize  = 24f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    // Scale and translate for zoom/pan
    private var scaleFactor    = 1f
    private var translateX     = 0f
    private var translateY     = 0f

    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor  = scaleFactor.coerceIn(0.5f, 4f)
                invalidate()
                return true
            }
        })

    private val gestureDetector = GestureDetector(context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?, e2: MotionEvent,
                dX: Float, dY: Float
            ): Boolean {
                translateX -= dX
                translateY -= dY
                invalidate()
                return true
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                handleTap(e.x, e.y)
                return true
            }
        })

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }

    private fun handleTap(tapX: Float, tapY: Float) {
        val bounds = getMapBounds()
        stations.forEach { station ->
            val sx = translateX + (station.lng - bounds.left) /
                    (bounds.right - bounds.left) * width * scaleFactor
            val sy = translateY + (station.lat - bounds.top) /
                    (bounds.bottom - bounds.top) * height * scaleFactor
            val dist = Math.sqrt(
                ((tapX - sx) * (tapX - sx) + (tapY - sy) * (tapY - sy)).toDouble()
            )
            if (dist < 40) {
                onStationClickListener?.onStationClicked(station)
                return
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.translate(translateX, translateY)
        canvas.scale(scaleFactor, scaleFactor)

        val bounds = getMapBounds()

        fun stationToX(lng: Double) =
            (lng - bounds.left) / (bounds.right - bounds.left) * width
        fun stationToY(lat: Double) =
            (lat - bounds.top) / (bounds.bottom - bounds.top) * height

        val purpleStations = stations
            .filter { it.line == "purple" }.sortedBy { it.order }
        val greenStations  = stations
            .filter { it.line == "green" }.sortedBy { it.order }

        // Draw highlighted route first (underneath lines)
        if (highlightedStations.isNotEmpty()) {
            val highlighted = stations.filter { it.name in highlightedStations }
                .sortedBy { it.order }
            for (i in 0 until highlighted.size - 1) {
                val s1 = highlighted[i]
                val s2 = highlighted[i + 1]
                canvas.drawLine(
                    stationToX(s1.lng).toFloat(), stationToY(s1.lat).toFloat(),
                    stationToX(s2.lng).toFloat(), stationToY(s2.lat).toFloat(),
                    paintHighlight
                )
            }
        }

        // Draw purple line
        for (i in 0 until purpleStations.size - 1) {
            val s1 = purpleStations[i]; val s2 = purpleStations[i + 1]
            canvas.drawLine(
                stationToX(s1.lng).toFloat(), stationToY(s1.lat).toFloat(),
                stationToX(s2.lng).toFloat(), stationToY(s2.lat).toFloat(),
                paintPurpleLine
            )
        }

        // Draw green line
        for (i in 0 until greenStations.size - 1) {
            val s1 = greenStations[i]; val s2 = greenStations[i + 1]
            canvas.drawLine(
                stationToX(s1.lng).toFloat(), stationToY(s1.lat).toFloat(),
                stationToX(s2.lng).toFloat(), stationToY(s2.lat).toFloat(),
                paintGreenLine
            )
        }

        // Draw station dots and labels
        stations.forEach { station ->
            val sx = stationToX(station.lng).toFloat()
            val sy = stationToY(station.lat).toFloat()
            val radius = if (station.isInterchange) 18f else 12f

            paintStationDot.color = if (station.line == "purple")
                Color.parseColor("#7B2D8B")
            else
                Color.parseColor("#2E7D32")

            canvas.drawCircle(sx, sy, radius, paintStationDot)
            canvas.drawCircle(sx, sy, radius, paintStationBorder)

            val label = if (station.isInterchange) paintInterchangeLabel else paintLabel
            canvas.drawText(station.name, sx, sy - radius - 8f, label)
        }

        canvas.restore()
    }

    private fun getMapBounds(): RectF {
        val lats = stations.map { it.lat }
        val lngs = stations.map { it.lng }
        val pad  = 0.01
        return RectF(
            (lngs.min() - pad).toFloat(),
            (lats.min() - pad).toFloat(),
            (lngs.max() + pad).toFloat(),
            (lats.max() + pad).toFloat()
        )
    }
}