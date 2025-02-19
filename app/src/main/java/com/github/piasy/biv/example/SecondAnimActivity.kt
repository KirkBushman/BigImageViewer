package com.github.piasy.biv.example

import android.app.Activity
import android.app.SharedElementCallback
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.transition.Transition
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.net.toUri
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.fresco.FrescoImageLoader
import com.github.piasy.biv.loader.glide.GlideImageLoader
import com.github.piasy.biv.view.BigImageView
import com.github.piasy.biv.view.FrescoImageViewFactory
import com.github.piasy.biv.view.GlideImageViewFactory
import com.github.piasy.biv.view.ImageShownCallback
import com.google.android.material.button.MaterialButton

class SecondAnimActivity : AppCompatActivity() {

    companion object {

        private const val THUMB_PARAM = "intent_param_thumbnail"
        private const val SOURCE_PARAM = "intent_param_source"

        private const val FLAG_USEGLIDE = "intent_param_flag_use_glide"
        private const val FLAG_USEFRESCO = "intent_param_flag_use_fresco"
        private const val FLAG_USEVIEWFACTORY = "intent_param_flag_use_view_factory"

        private const val ANIM_DURATION = 500
        private const val ANIM_QUICK_DURATION = 200

        fun start(activity: Activity, imageView: ImageView,
                  thumbUrl: String, sourceUrl: String,
                  useGlide: Boolean, useFresco: Boolean, useViewFactory: Boolean) {

            val intent = Intent(activity, SecondAnimActivity::class.java)
            intent.putExtra(THUMB_PARAM, thumbUrl)
            intent.putExtra(SOURCE_PARAM, sourceUrl)

            intent.putExtra(FLAG_USEGLIDE, useGlide)
            intent.putExtra(FLAG_USEFRESCO, useFresco)
            intent.putExtra(FLAG_USEVIEWFACTORY, useViewFactory)

            val transitionName = activity.resources.getString(R.string.transition_name)
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, imageView, transitionName)
            val bundle = options.toBundle()

            if (Build.VERSION.SDK_INT >= 16) {

                if (useFresco && Build.VERSION.SDK_INT >= 21) {
                    activity.setExitSharedElementCallback(object : SharedElementCallback() {

                        override fun onSharedElementEnd(
                                names: MutableList<String>?,
                                elements: MutableList<View>?,
                                snapshots: MutableList<View>?
                        ) {
                            super.onSharedElementEnd(names, elements, snapshots)

                            elements?.forEach {
                                if (it is SimpleDraweeView) {
                                    it.post { it.setVisibility(View.VISIBLE) }
                                }
                            }
                        }
                    })
                }

                activity.startActivity(intent, bundle)
            } else {
                Log.i("SecondAnimActivity", "Animation not available for this SDK Versions.")
            }
        }
    }

    private val thumbUrl by lazy { intent.getStringExtra(THUMB_PARAM)!! }
    private val sourceUrl by lazy { intent.getStringExtra(SOURCE_PARAM)!! }

    private val useGlide by lazy { intent.getBooleanExtra(FLAG_USEGLIDE, true) }
    private val useFresco by lazy { intent.getBooleanExtra(FLAG_USEFRESCO, false) }
    private val useViewFactory by lazy { intent.getBooleanExtra(FLAG_USEVIEWFACTORY, false) }

    private val biv by lazy { findViewById<BigImageView>(R.id.sourceView) }

    private val minScale by lazy { findViewById<MaterialButton>(R.id.min_scale) }
    private val maxScale by lazy { findViewById<MaterialButton>(R.id.max_scale) }
    private val halfScale by lazy { findViewById<MaterialButton>(R.id.half_scale) }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (useFresco) {
            Fresco.initialize(this)
        }

        super.onCreate(savedInstanceState)

        if (useGlide) {
            BigImageViewer.initialize(GlideImageLoader.with(applicationContext))
        }

        if (useFresco) {
            BigImageViewer.initialize(FrescoImageLoader.with(applicationContext))
        }

        setContentView(R.layout.activity_anim_second)

        if (Build.VERSION.SDK_INT >= 21) {

            if (useFresco) {
                setEnterSharedElementCallback(object : SharedElementCallback() {

                    override fun onSharedElementEnd(
                            names: MutableList<String>?,
                            elements: MutableList<View>?,
                            snapshots: MutableList<View>?
                    ) {
                        super.onSharedElementEnd(names, elements, snapshots)

                        elements?.forEach {
                            if (it is SimpleDraweeView) {
                                it.post { it.setVisibility(View.VISIBLE) }
                            }
                        }
                    }
                })
            }

            window.sharedElementEnterTransition.addListener(object : Transition.TransitionListener {

                override fun onTransitionStart(transition: Transition?) {}
                override fun onTransitionCancel(transition: Transition?) {}
                override fun onTransitionPause(transition: Transition?) {}
                override fun onTransitionResume(transition: Transition?) {}

                override fun onTransitionEnd(transition: Transition?) {

                    biv.loadMainImageNow()
                }
            })
        }

        if (useGlide && useViewFactory) {
            biv.setImageViewFactory(GlideImageViewFactory())
        }

        if (useFresco && useViewFactory) {
            biv.setImageViewFactory(FrescoImageViewFactory())
        }

        minScale.setOnClickListener {
            if (biv.isZoomEnabled) {
                biv.animateMinScale(ANIM_DURATION)
            }
        }

        maxScale.setOnClickListener {
            if (biv.isZoomEnabled) {
                biv.animateMaxScale(ANIM_DURATION)
            }
        }

        halfScale.setOnClickListener {
            if (biv.isZoomEnabled) {
                val halfScale = ((biv.maxScale - biv.minScale) / 2) + biv.minScale
                biv.animateToScale(halfScale, ANIM_DURATION)
            }
        }

        biv.setImageShownCallback(object : ImageShownCallback {

            override fun onThumbnailShown() {
                showToast("onThumbnailShown triggered!")
            }

            override fun onMainImageShown() {
                showToast("onMainImageShown triggered!")
            }
        })

        biv.showImage(thumbUrl.toUri(), sourceUrl.toUri(), true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId) {
            android.R.id.home -> {
                supportFinishAfterTransition()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        setResultAndFinish()
    }

    override fun onNavigateUp(): Boolean {
        setResultAndFinish()
        return true
    }

    private fun setResultAndFinish() {
        if (Build.VERSION.SDK_INT >= 21) {

            if (biv.currentScale == biv.minScale) {

                finishAfterTransition()
            } else {

                biv.animateMinScale(ANIM_QUICK_DURATION, object : SubsamplingScaleImageView.OnAnimationEventListener {

                    override fun onComplete() {
                        finishAfterTransition()
                    }

                    override fun onInterruptedByUser() {
                        finishAfterTransition()
                    }

                    override fun onInterruptedByNewAnim() {
                        finishAfterTransition()
                    }
                })
            }
        }
    }

    private fun showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}