package com.popsa.interview.dragtoswap

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.popsa.interview.dragtoswap.MainActivityCoordinator.Events.ImageDropped
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_scrolling.*


/**
 * Place for applying view data to views, and passing actions to coordinator
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: MainActivityViewModel by viewModels()
    private lateinit var coordinator: MainActivityCoordinator

    private val imageViews: List<ImageView> by lazy { listOf(image1, image2, image3, image4) }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        coordinator = MainActivityCoordinator(viewModel)
        setSupportActionBar(toolbar)
        toolbar.title = title

        viewModel.images.observe(this, Observer { images ->
            // Load all the images from the viewModel into ImageViews
            imageViews.forEachIndexed { index, imageView ->
                Glide.with(this)
                    .load(images[index].imageUrl)
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .into(imageView)
                imageView.tag =
                    index // Quick&dirty: stash the index of this image in the ImageView tag
            }
        })

        list.setOnTouchListener { _, event ->
            val eventX = event.x.toInt()
            val eventY = event.y.toInt()
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { // Hunt for what's under the drag and start dragging
                    getImageViewAt(eventX, eventY)?.let {
                        val index = it.tag as Int
                        coordinator.startedSwap(index)
                        animateSourceImage(it)
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    coordinator.imageDragging(eventX, eventY)
                }
                MotionEvent.ACTION_UP -> { // If we are dragging to something valid, do the swap
                    coordinator.imageDropped(eventX, eventY)
                }
            }
            true
        }

        viewModel.events.observe(this, Observer {
            when (it) {
                is ImageDropped -> dropImage(it.x, it.y)
                is MainActivityCoordinator.Events.ImageDragging -> draggingImage(it.x, it.y)
            }
        })
    }

    private val dragListener = View.OnDragListener { view, event ->
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                true
            }
            DragEvent.ACTION_DRAG_ENTERED -> {
                view.invalidate()
                true
            }
            DragEvent.ACTION_DRAG_LOCATION -> true
            DragEvent.ACTION_DRAG_EXITED -> {
                view.invalidate()
                true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                view.invalidate()
                true
            }
            else -> false
        }
    }

    private fun draggingImage(eventX: Int, eventY: Int) {
        Log.d("Drag:", "Image dragging")
        val image = getImageViewAt(eventX, eventY)
        image?.setOnClickListener {
            val shadowBuilder = View.DragShadowBuilder(it)
            it.startDragAndDrop(null, shadowBuilder, it, 0)

            it.visibility = View.INVISIBLE
        }
        image?.setOnDragListener(dragListener)
    }

    private fun animateSourceImage(image: ImageView) {
        image.animate().apply {
            duration = 800
            scaleXBy(-1f)
            scaleYBy(-1f)
        }.withEndAction {
            image.animate().apply {
                duration = 500
                scaleXBy(1f)
                scaleYBy(1f)
            }.start()
        }.start()
    }

    private fun animateTargetImage(image: ImageView) {
        image.animate().apply {
            duration = 1000
            scaleX(1f)
            scaleY(1f)
        }.start()
    }

    private fun dropImage(eventX: Int, eventY: Int) {
        val sourceImageIndex = viewModel.draggingIndex.value
        val targetImage = getImageViewAt(eventX, eventY)
        val targetImageIndex = targetImage
            ?.let { it.tag as Int }
        if (targetImageIndex != null && sourceImageIndex != null && targetImageIndex != sourceImageIndex) {
            animateTargetImage(targetImage)
            coordinator.swapImages(sourceImageIndex, targetImageIndex)
        } else {
            coordinator.cancelSwap()
        }
    }

    private fun getImageViewAt(x: Int, y: Int): ImageView? {
        val hitRect = Rect()
        return imageViews.firstOrNull {
            it.getHitRect(hitRect)
            hitRect.contains(x, y)
        }
    }
}