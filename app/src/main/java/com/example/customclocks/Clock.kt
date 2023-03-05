package com.example.customclocks

import android.content.Context
import android.graphics.*
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.drawable.Drawable
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toRectF
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import kotlinx.coroutines.Runnable
import java.util.*

data class HandData(
    var handHalfWidth: Int = 0,
    var protrudingHeight: Int = 0,
    val handRect: Rect = Rect(),
    val handRotationMatrix: Matrix = Matrix(),
    val handPath: Path = Path(),
    val handHalfWidthCoefficient: Int = 1,
    val protrudingHeightCoefficient: Int = 1,
    val handHeightCoefficient: Float = 1f,
    val handPaint: Paint = Paint(ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        isAntiAlias = true
    }
)

class Clock(context: Context, attrs: AttributeSet): View(context, attrs) {
    companion object {
        const val DATE_FORMAT_PATTERN = "yyyy/MM/dd HH:mm:ss"
    }

    private var w = 0f
    private var h = 0f

    private var centerX = 0f
    private var centerY = 0f

    private val minHandHalfWidth get() = (this.w / 200).toInt()
    private val minProtrudingHeight get() = (this.h / 200).toInt()

    private val secondHandData = HandData(
        handHalfWidthCoefficient = 1,
        protrudingHeightCoefficient = 10,
        handHeightCoefficient = 1f
    )
    private val minuteHandData = HandData(
        handHalfWidthCoefficient = 2,
        protrudingHeightCoefficient = 10,
        handHeightCoefficient = 0.75f
    )
    private val hourHandData = HandData(
        handHalfWidthCoefficient = 4,
        protrudingHeightCoefficient = 10,
        handHeightCoefficient = 0.5f
    )

    private val secondHandRotateAngle get() = seconds % 3600 % 60 * 6
    private val minuteHandRotateAngle get() = seconds % 3600 / 60 * 6
    private val hourHandRotateAngle get() = seconds / 3600 % 12 * 30 +
            seconds % 3600 / 60 / 12 * 6

    private val clockFaceDrawable: Drawable = VectorDrawableCompat.create(
        context.resources, R.drawable.clock_face, null
    )!!

    private val dateFormat = SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.getDefault())

    private var seconds = 0L

    private val timer = Timer()

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.Clock, 0, 0).apply {
            seconds = try {
                dateFormat.parse(getString(
                    R.styleable.Clock_date
                )).time / 1000
            } catch(e: Exception) {
                Calendar.getInstance().time.time / 1000
            } finally {
                recycle()
            }
        }
    }

    init {
        timer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                Handler(Looper.getMainLooper()).post(Runnable {
                    seconds++
                    rotateHands()
                    this@Clock.invalidate()
                })
            }
        },
        0,
        1000
        )
    }

    private fun updateHandData(
        handData: HandData,
    ) {
        handData.apply {
            this.handHalfWidth = minHandHalfWidth * handHalfWidthCoefficient
            this.protrudingHeight = minProtrudingHeight * protrudingHeightCoefficient
            val secondHandWidth = handHalfWidth * 2
            val secondHandHeight = (centerY * handHeightCoefficient + protrudingHeight).toInt()
            this.handRect.set(0, 0, secondHandWidth, secondHandHeight)
        }
    }
    private fun updateHandsData() {
        updateHandData(secondHandData)
        updateHandData(minuteHandData)
        updateHandData(hourHandData)
    }

    private fun rotateHand(handData: HandData, angle: Float) {
        handData.apply {
            handPath.reset()
            handRotationMatrix.reset()

            handRotationMatrix.postRotate(
                angle,
                handHalfWidth.toFloat(),
                handRect.bottom - protrudingHeight + 0f
            )

            handPath.apply {
                addRect(handRect.toRectF(), Path.Direction.CW)
                transform(handRotationMatrix)
                offset(
                    centerX - handHalfWidth,
                    centerY - handRect.bottom + protrudingHeight,
                )
            }
        }
    }
    private fun rotateHands() {
        rotateHand(secondHandData, secondHandRotateAngle.toFloat())
        rotateHand(minuteHandData, minuteHandRotateAngle.toFloat())
        rotateHand(hourHandData, hourHandRotateAngle.toFloat())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        this.w = w - (paddingLeft + paddingRight).toFloat()
        this.h = h - (paddingTop + paddingBottom).toFloat()

        centerX = this.w / 2
        centerY = this.h / 2

        clockFaceDrawable.setBounds(0, 0, this.w.toInt(), this.h.toInt())

        updateHandsData()
        rotateHands()
    }

    private fun drawHand(canvas: Canvas, handData: HandData) {
        canvas.drawPath(
            handData.handPath,
            handData.handPaint
        )
    }
    private fun drawHands(canvas: Canvas) {
        drawHand(canvas, secondHandData)
        drawHand(canvas, minuteHandData)
        drawHand(canvas, hourHandData)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        clockFaceDrawable.draw(canvas)
        drawHands(canvas)
    }

    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle().apply {
            putParcelable("superState", super.onSaveInstanceState())
            putLong("seconds", seconds)
        }
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val bundle = state as Bundle
        this.seconds = state.getLong("seconds")
        val state = bundle.getParcelable<Parcelable>("superState")!!
        super.onRestoreInstanceState(state)
    }
}