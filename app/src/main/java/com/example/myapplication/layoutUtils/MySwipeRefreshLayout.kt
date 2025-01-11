package com.example.myapplication.layoutUtils

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.google.android.material.slider.Slider

class MySwipeRefreshLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : androidx.swiperefreshlayout.widget.SwipeRefreshLayout(context, attrs) {

    private val excludedViews = mutableListOf<Slider>()

    fun addExcludedView(view: Slider) {
        excludedViews.add(view)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        for (view in excludedViews) {
            if (isTouchInsideView(ev, view)) {
                return false
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    private fun isTouchInsideView(event: MotionEvent, view: Slider): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1]
        return event.rawX >= x && event.rawX <= x + view.width &&
                event.rawY >= y && event.rawY <= y + view.height
    }
}
