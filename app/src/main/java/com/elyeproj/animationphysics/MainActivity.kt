package com.elyeproj.animationphysics

import android.animation.*
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FlingAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    companion object {
        private const val DEFAULT_FRICTION = 1.1f
    }

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
        SpringAnimation(img_ball, DynamicAnimation.X).apply {
            addUpdateListener { animation, value, velocity ->
                endCheck()
            }
        }
    }

    private val springAnimationY: SpringAnimation by lazy {
        SpringAnimation(img_ball, DynamicAnimation.Y).apply {
            addUpdateListener { animation, value, velocity ->
                endCheck()
            }
        }
    }

    private val flingAnimationX: FlingAnimation by lazy {
        FlingAnimation(img_ball, DynamicAnimation.X).setFriction(DEFAULT_FRICTION).apply {
            setMinValue(0f)
            setMaxValue(maxWidth)
            addEndListener { animation, canceled, value, velocity ->
                startStringAnimation(velocity, springAnimationX, springForce, maxWidth)
            }
            addUpdateListener { animation, value, velocity ->
                endCheck()
            }
        }
    }

    private val flingAnimationY: FlingAnimation by lazy {
        FlingAnimation(img_ball, DynamicAnimation.Y).setFriction(DEFAULT_FRICTION).apply {
            setMinValue(0f)
            setMaxValue(maxHeight)
            addEndListener { animation, canceled, value, velocity ->
                startStringAnimation(velocity, springAnimationY, springForce, maxHeight)
            }
            addUpdateListener { animation, value, velocity ->
                Log.d("Elisha", "flingAnimationY")
                endCheck()
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

    var isEnding = false

    private fun endCheck() {
        if (isEnding) return

        val ballCenterX = img_ball.x + img_ball.width / 2
        val ballCenterY = img_ball.y + img_ball.height / 2

        if ((ballCenterX >= img_droid.x && ballCenterX <= img_droid.x + img_droid.width) &&
            (ballCenterY >= img_droid.y && ballCenterY <= img_droid.y + img_droid.height)) {

            isEnding = true
            endAllPhysicAnimation()

            AnimatorSet().apply {
                play(ObjectAnimator.ofFloat(img_ball, View.ALPHA, 1f, 0f))
                    .with(ObjectAnimator.ofFloat(img_ball, View.SCALE_X, 1f, 0.5f))
                    .with(ObjectAnimator.ofFloat(img_ball, View.SCALE_Y, 1f, 0.5f)).after(
                        ObjectAnimator.ofPropertyValuesHolder(img_ball,
                            PropertyValuesHolder.ofFloat(View.X, img_droid.x),
                            PropertyValuesHolder.ofFloat(View.Y, img_droid.y)))

                addListener(object: AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        resetBall()
                    }
                })

            }.start()
        }
    }

    private fun resetBall() {
        img_ball.translationX = 0f
        img_ball.translationY = 0f
        img_ball.scaleX = 1f
        img_ball.scaleY = 1f
        flingAnimationX.friction = DEFAULT_FRICTION
        flingAnimationY.friction = DEFAULT_FRICTION
        ObjectAnimator.ofFloat(img_ball, View.ALPHA, 0f, 1f).start()

        isEnding = false
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

    private fun endAllPhysicAnimation() {
        springAnimationX.cancel()
        springAnimationY.cancel()
        flingAnimationY.friction = 5f
        flingAnimationX.friction = 5f
    }

    internal fun isAnimationRunning() = (springAnimationX.isRunning || springAnimationY.isRunning
        || flingAnimationX.isRunning || flingAnimationY.isRunning)
}
