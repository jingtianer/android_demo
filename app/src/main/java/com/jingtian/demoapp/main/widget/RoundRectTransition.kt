package com.jingtian.demoapp.main.widget

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.transition.Transition
import android.transition.TransitionValues
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import androidx.annotation.Keep

class RoundRectTransition : Transition {
    constructor() : super()
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    override fun captureStartValues(transitionValues: TransitionValues?) {
        val view = transitionValues?.view as? RoundRectImageView ?: return
        transitionValues.values["lt"] = view.startRadii[0]
        transitionValues.values["rt"] = view.startRadii[2]
        transitionValues.values["rb"] = view.startRadii[4]
        transitionValues.values["lb"] = view.startRadii[6]

        val endLT: Float = transitionValues.values.get("lt") as? Float ?: 0f
        val endRT: Float = transitionValues.values.get("rt") as? Float ?: 0f
        val endRB: Float = transitionValues.values.get("rb") as? Float ?: 0f
        val endLB: Float = transitionValues.values.get("lb") as? Float ?: 0f
        Log.d("TAG", "captureStartValues: ${view.hashCode()} $endLT, $endRT, $endRB, $endLB")
    }

    override fun captureEndValues(transitionValues: TransitionValues?) {
        val view = transitionValues?.view as? RoundRectImageView ?: return
        transitionValues.values["lt"] = view.endRadii[0]
        transitionValues.values["rt"] = view.endRadii[2]
        transitionValues.values["rb"] = view.endRadii[4]
        transitionValues.values["lb"] = view.endRadii[6]

        val endLT: Float = transitionValues.values.get("lt") as? Float ?: 0f
        val endRT: Float = transitionValues.values.get("rt") as? Float ?: 0f
        val endRB: Float = transitionValues.values.get("rb") as? Float ?: 0f
        val endLB: Float = transitionValues.values.get("lb") as? Float ?: 0f
        Log.d("TAG", "captureEndValues: ${view.hashCode()} $endLT, $endRT, $endRB, $endLB")
    }

    override fun createAnimator(
        sceneRoot: ViewGroup?,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator {
        captureStartValues(startValues)
        captureEndValues(endValues)
        val startLT: Float = startValues?.values?.get("lt") as? Float ?: 0f
        val startRT: Float = startValues?.values?.get("rt") as? Float ?: 0f
        val startRB: Float = startValues?.values?.get("rb") as? Float ?: 0f
        val startLB: Float = startValues?.values?.get("lb") as? Float ?: 0f

        val endLT: Float = endValues?.values?.get("lt") as? Float ?: 0f
        val endRT: Float = endValues?.values?.get("rt") as? Float ?: 0f
        val endRB: Float = endValues?.values?.get("rb") as? Float ?: 0f
        val endLB: Float = endValues?.values?.get("lb") as? Float ?: 0f

        val view = endValues?.view as? RoundRectImageView

        if (view != null) {
            Log.d("TAG", "createAnimator: start ${view.hashCode()} $startLT, $startRT, $startRB, $startLB")
            Log.d("TAG", "createAnimator: end ${view.hashCode()} $endLT, $endRT, $endRB, $endLB")
        }

        val viewWrapper = object {
            private var progress = 0f
            @Keep
            fun setProgress(progress: Float) {
                this.progress = progress
                view?.updateRadii(
                    (endLT - startLT) * progress + startLT,
                    (endLB - startLB) * progress + startLB,
                    (endRT- startRT) * progress + startRT,
                    (endRB - startRB) * progress + startRB,
                )
            }

            @Keep
            fun getProgress(): Float {
                return progress
            }
        }
        return ObjectAnimator.ofFloat(viewWrapper, "progress", 0f, 1f)
    }
}