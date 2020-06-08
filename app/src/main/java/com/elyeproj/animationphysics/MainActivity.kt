package com.elyeproj.animationphysics

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FlingAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private val springForceX: SpringForce by lazy(LazyThreadSafetyMode.NONE) {
        SpringForce(0f).apply {
            stiffness = SpringForce.STIFFNESS_LOW
            dampingRatio = SpringForce.DAMPING_RATIO_HIGH_BOUNCY
        }
    }

    private val springForceY: SpringForce by lazy(LazyThreadSafetyMode.NONE) {
        SpringForce(0f).apply {
            stiffness = SpringForce.STIFFNESS_LOW
            dampingRatio = SpringForce.DAMPING_RATIO_HIGH_BOUNCY
        }
    }

    private val maxWidth by lazy { container.width.toFloat() - img_ball.width }
    private val maxHeight by lazy { container.height.toFloat() - img_ball.height }

    private val springAnimationX: SpringAnimation by lazy(LazyThreadSafetyMode.NONE) {
        SpringAnimation(img_ball, DynamicAnimation.X)
    }

    private val springAnimationY: SpringAnimation by lazy(LazyThreadSafetyMode.NONE) {
        SpringAnimation(img_ball, DynamicAnimation.Y)
    }

    private val flingAnimationX: FlingAnimation by lazy(LazyThreadSafetyMode.NONE) {
        FlingAnimation(img_ball, DynamicAnimation.X).setFriction(1.1f).apply {
            setMinValue(0f)
            setMaxValue(maxWidth)
            addEndListener { animation, canceled, value, velocity ->
                startStringAnimation(velocity, springAnimationX, springForceX, maxWidth)
            }
        }
    }

    private val flingAnimationY: FlingAnimation by lazy(LazyThreadSafetyMode.NONE) {
        FlingAnimation(img_ball, DynamicAnimation.Y).setFriction(1.1f).apply {
            setMinValue(0f)
            setMaxValue(maxHeight)
            addEndListener { animation, canceled, value, velocity ->
                startStringAnimation(velocity, springAnimationY, springForceY, maxHeight)
            }
        }
    }

    private fun startStringAnimation(velocity: Float, springAnimation: SpringAnimation,
                                     springForce: SpringForce, max: Float) {
        if (abs(velocity) > 0) {
            springAnimation
                .setSpring(springForce.setFinalPosition(
                    if (velocity > 0) max else 0f))
                .setStartVelocity(velocity)
                .start()
        }
    }

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent?): Boolean {
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            if (isAnimationRunning()) return false
            flingAnimationX.setStartVelocity(velocityX)
            flingAnimationY.setStartVelocity(velocityY)
            flingAnimationX.start()
            flingAnimationY.start()
            return true
        }
    }

    private val gestureDetector by lazy {
        GestureDetector(this, gestureListener)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        img_ball.setOnTouchListener { view, motionEvent ->
            gestureDetector.onTouchEvent(motionEvent)
            true
        }
    }

    internal fun isAnimationRunning() = (springAnimationX.isRunning || springAnimationY.isRunning
        || flingAnimationX.isRunning || flingAnimationY.isRunning)
}
