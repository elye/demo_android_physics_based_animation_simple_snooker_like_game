package com.elyeproj.animationphysics

import android.animation.*
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
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
        private const val BREAK_FRICTION = 5f
        private const val VELOCITY_THRESHOLD = 300
    }

    private var velocityFlingX = 0f
    private var velocityFlingY = 0f
    private var velocitySpringX = 0f
    private var velocitySpringY = 0f

    // To flag stop animation so spring animation don't
    // get trigger after ball has enter hole
    private var stopAnimation = false

    private val springForce: SpringForce
        get() {
            return SpringForce(0f).apply {
                stiffness = SpringForce.STIFFNESS_LOW
                dampingRatio = SpringForce.DAMPING_RATIO_HIGH_BOUNCY
            }
        }

    private val maxWidth by lazy { container.width.toFloat() - img_ball.width }
    private val maxHeight by lazy { container.height.toFloat() - img_ball.height }

    private val holes by lazy {
        listOf(img_hole1, img_hole2, img_hole3, img_hole4, img_hole5, img_hole6, img_hole7, img_hole8)
    }

    private val springAnimationX: SpringAnimation by lazy {
        SpringAnimation(img_ball, DynamicAnimation.X).apply {
            addEndListener { animation, canceled, value, velocity ->
                velocitySpringY = 0f
            }
            addUpdateListener { animation, value, velocity ->
                velocitySpringY = velocity
                if (isSlowEnoughToEnterHole())
                    endCheck()
            }
        }
    }

    private val springAnimationY: SpringAnimation by lazy {
        SpringAnimation(img_ball, DynamicAnimation.Y).apply {
            addEndListener { animation, canceled, value, velocity ->
                velocitySpringX = 0f
            }
            addUpdateListener { animation, value, velocity ->
                velocitySpringX = velocity
                if (isSlowEnoughToEnterHole())
                    endCheck()
            }
        }
    }

    private val flingAnimationX: FlingAnimation by lazy {
        FlingAnimation(img_ball, DynamicAnimation.X).setFriction(DEFAULT_FRICTION).apply {
            setMinValue(0f)
            setMaxValue(maxWidth)
            addEndListener { animation, canceled, value, velocity ->
                velocityFlingX = 0f
                startStringAnimation(velocity, springAnimationX, springForce, maxWidth)
            }
            addUpdateListener { animation, value, velocity ->
                velocityFlingX = velocity
                if (isSlowEnoughToEnterHole())
                    endCheck()
            }
        }
    }

    private val flingAnimationY: FlingAnimation by lazy {
        FlingAnimation(img_ball, DynamicAnimation.Y).setFriction(DEFAULT_FRICTION).apply {
            setMinValue(0f)
            setMaxValue(maxHeight)
            addEndListener { animation, canceled, value, velocity ->
                velocityFlingY = 0f
                startStringAnimation(velocity, springAnimationY, springForce, maxHeight)
            }
            addUpdateListener { animation, value, velocity ->
                velocityFlingY = velocity
                if (isSlowEnoughToEnterHole())
                    endCheck()
            }
        }
    }

    private fun isSlowEnoughToEnterHole(): Boolean {
        return abs(velocityFlingY) < VELOCITY_THRESHOLD &&
            abs(velocityFlingX) < VELOCITY_THRESHOLD &&
            abs(velocitySpringY) < VELOCITY_THRESHOLD &&
            abs(velocitySpringX) < VELOCITY_THRESHOLD
    }

    private fun startStringAnimation(velocity: Float, springAnimation: SpringAnimation,
                                     springForce: SpringForce, max: Float) {
        if (abs(velocity) > 0 && !stopAnimation) {
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

        holes.any{ isEnteringHole(it, ballCenterX, ballCenterY) }
    }

    private fun isEnteringHole(
        hold: ImageView,
        ballCenterX: Float,
        ballCenterY: Float
    ): Boolean {
        if ((ballCenterX >= hold.x && ballCenterX <= hold.x + hold.width) &&
            (ballCenterY >= hold.y && ballCenterY <= hold.y + hold.height)) {

            isEnding = true
            endAllPhysicAnimation()

            AnimatorSet().apply {
                play(ObjectAnimator.ofFloat(img_ball, View.ALPHA, 1f, 0f))
                    .with(ObjectAnimator.ofFloat(img_ball, View.SCALE_X, 1f, 0.5f))
                    .with(ObjectAnimator.ofFloat(img_ball, View.SCALE_Y, 1f, 0.5f)).after(
                        ObjectAnimator.ofPropertyValuesHolder(img_ball,
                            PropertyValuesHolder.ofFloat(View.X, hold.x),
                            PropertyValuesHolder.ofFloat(View.Y, hold.y)))

                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        resetBall()
                    }
                })

            }.start()

            return true
        }
        return false
    }

    private fun resetBall() {
        img_ball.translationX = 0f
        img_ball.translationY = 0f
        img_ball.scaleX = 1f
        img_ball.scaleY = 1f
        flingAnimationX.friction = DEFAULT_FRICTION
        flingAnimationY.friction = DEFAULT_FRICTION
        ObjectAnimator.ofFloat(img_ball, View.ALPHA, 0f, 1f).apply {
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    isEnding = false
                }
            })
        }.start()
    }

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent?): Boolean {
            stopAnimation = false
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
        flingAnimationY.friction = BREAK_FRICTION
        flingAnimationX.friction = BREAK_FRICTION
        stopAnimation = true
    }

    internal fun isAnimationRunning() = (springAnimationX.isRunning || springAnimationY.isRunning
        || flingAnimationX.isRunning || flingAnimationY.isRunning)
}
