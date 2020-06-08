package com.elyeproj.animationphysics

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FlingAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private val springForce: SpringForce
    get() {
        return SpringForce(0f).apply {
            stiffness = SpringForce.STIFFNESS_LOW
            dampingRatio = SpringForce.DAMPING_RATIO_HIGH_BOUNCY
        }
    }

    private val maxWidth by lazy { container.width.toFloat() - img_ball.width }
    private val maxHeight by lazy { container.height.toFloat() - img_ball.height }

    private val springAnimationX: SpringAnimation by lazy {
        SpringAnimation(img_ball, DynamicAnimation.X).addEndListener { animation, canceled, value, velocity ->
            endCheck()
        }
    }

    private val springAnimationY: SpringAnimation by lazy {
        SpringAnimation(img_ball, DynamicAnimation.Y).addEndListener { animation, canceled, value, velocity ->
            endCheck()
        }
    }

    private val flingAnimationX: FlingAnimation by lazy {
        FlingAnimation(img_ball, DynamicAnimation.X).setFriction(1.1f).apply {
            setMinValue(0f)
            setMaxValue(maxWidth)
            addEndListener { animation, canceled, value, velocity ->
                startStringAnimation(velocity, springAnimationX, springForce, maxWidth)
            }
        }
    }

    private val flingAnimationY: FlingAnimation by lazy {
        FlingAnimation(img_ball, DynamicAnimation.Y).setFriction(1.1f).apply {
            setMinValue(0f)
            setMaxValue(maxHeight)
            addEndListener { animation, canceled, value, velocity ->
                startStringAnimation(velocity, springAnimationY, springForce, maxHeight)
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
        } else {
            endCheck()
        }
    }

    private fun endCheck() {
        if (isAnimationRunning()) return
        if (((img_ball.x >= img_droid.x && img_ball.x <= img_droid.x + img_droid.width) ||
                (img_ball.x + img_ball.width >= img_droid.x && img_ball.x + img_ball.width <= img_droid.x + img_droid.width)) &&
            ((img_ball.y >= img_droid.y && img_ball.y <= img_droid.y + img_droid.height) ||
                (img_ball.y + img_ball.height >= img_droid.y && img_ball.y + img_ball.height <= img_droid.y + img_droid.height))) {

            AnimatorSet().apply {
                play(ObjectAnimator.ofFloat(img_ball, View.ALPHA, 1f, 0f))
                    .with(ObjectAnimator.ofFloat(img_ball, View.SCALE_X, 1f, 0.5f))
                    .with(ObjectAnimator.ofFloat(img_ball, View.SCALE_Y, 1f, 0.5f)).after(
                    ObjectAnimator.ofPropertyValuesHolder(img_ball,
                        PropertyValuesHolder.ofFloat(View.X, img_droid.x),
                        PropertyValuesHolder.ofFloat(View.Y, img_droid.y)))
            }.start()
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
