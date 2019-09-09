package ru.company.sts.soundbound.mvp.activities

import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.RemoteException
import android.os.SystemClock
import android.support.annotation.StringRes
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.content.ContextCompat
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.text.format.DateUtils
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import com.arellomobile.mvp.MvpAppCompatActivity
import com.arellomobile.mvp.presenter.InjectPresenter
import kotlinx.android.synthetic.main.bottom_sheet_palyer.*
import kotlinx.android.synthetic.main.bottom_sheet_scene1_ml.*
import ru.company.sts.soundbound.R
import ru.company.sts.soundbound.adapters.MedialListAdapter
import ru.company.sts.soundbound.model.IHolderClickObserver
import ru.company.sts.soundbound.mvp.presenters.PresenterMain
import ru.company.sts.soundbound.mvp.views.IMainView
import ru.company.sts.soundbound.service.PlayerService
import ru.company.sts.soundbound.utils.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


class MainActivitySB : MvpAppCompatActivity(), IMainView {
    companion object {
        private const val TAG = "MainActivitySB"
        private const val PROGRESS_UPDATE_INTERNAL: Long = 1000
        private const val PROGRESS_UPDATE_INITIAL_INTERVAL: Long = 100
    }

    @InjectPresenter
    lateinit var presenter: PresenterMain

    private var ivBackgrounds: ImageView? = null

    private var rvMediaList: RecyclerView? = null
    private var mediaController: MediaControllerCompat? = null
    private var mMediaBrowser: MediaBrowserCompat? = null
    private var adapter: MedialListAdapter? = null

    private val mUpdateProgressTask = Runnable { updateSbProgress() }
    private val mExecutorService = Executors.newSingleThreadScheduledExecutor()
    private var mScheduleFuture: ScheduledFuture<*>? = null
    private val mHandler = Handler()

    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    private val mMediaBrowserConnectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            mMediaBrowser?.let {
                presenter.onConnectTosession(it.sessionToken)

            }
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            Log.d(TAG, "onConnectionSuspended")
        }

        override fun onConnectionFailed() {
            Log.d(TAG, "onConnectionFailed")
        }
    }


    private val mCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            state?.let { presenter.updateLastTrackState(it) }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            metadata?.let { presenter.onMetadataChanged(it) }
        }
    }

    private var mPauseDrawable: Drawable? = null
    private var mPlayDrawable: Drawable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_sb)
        performConnectToService()
    }

    private fun initGui() {
        mPauseDrawable = ContextCompat.getDrawable(this, R.drawable.icv_pause)
        mPlayDrawable = ContextCompat.getDrawable(this, R.drawable.icv_play)

        rvMediaList = findViewById(R.id.rv_media_list)

        adapter = MedialListAdapter(mPlayDrawable, mPauseDrawable, object : IHolderClickObserver {
            override fun onHolderClick(holder: RecyclerView.ViewHolder?) {
                presenter.onHolderClick(this@MainActivitySB, mediaController, holder)
            }
        })

        adapter?.setItemPlaying(presenter.lastTrack)
        adapter?.setItemState(presenter.lastTrackState)

        val llManager = LinearLayoutManager(this)
        rvMediaList?.adapter = adapter
        rvMediaList?.layoutManager = llManager

        initScene()
    }


    override fun afterConnectToSession(token: MediaSessionCompat.Token?) {
        try {
            token?.let {
                mediaController = MediaControllerCompat(
                        this@MainActivitySB, token)

                MediaControllerCompat.setMediaController(this@MainActivitySB, mediaController)
                mediaController?.registerCallback(mCallback)
                presenter.afterMediaControllerCreated(mediaController)
            }
        } catch (e: RemoteException) {
            Log.e(TAG, e.message)
        }
    }

    private fun createMediaBrowser() {
        mMediaBrowser = MediaBrowserCompat(this,
                ComponentName(this, PlayerService::class.java), mMediaBrowserConnectionCallback, null)

    }

    override fun connectToMediaService() {
        mMediaBrowser?.connect()
    }

    override fun init() {
        initGui()
        createMediaBrowser()
    }

    override fun onStop() {
        super.onStop()
        MediaControllerCompat.getMediaController(this)?.unregisterCallback(mCallback)
        mMediaBrowser?.disconnect()
    }

    override fun onPlayBackStateChanged(state: PlaybackStateCompat?) {
        when (state?.state) {
            PlaybackStateCompat.STATE_PLAYING -> {
                iv_playback?.visibility = View.VISIBLE
                iv_playback?.setImageDrawable(mPauseDrawable)
                bottomSheetBehavior?.let {
                    if (it.state == BottomSheetBehavior.STATE_HIDDEN) {
                        it.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                }
            }
            PlaybackStateCompat.STATE_PAUSED -> {
                iv_playback?.visibility = View.VISIBLE
                iv_playback?.setImageDrawable(mPlayDrawable)
                bottomSheetBehavior?.let {
                    if (it.state == BottomSheetBehavior.STATE_HIDDEN) {
                        it.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                }
            }
            PlaybackStateCompat.STATE_NONE, PlaybackStateCompat.STATE_STOPPED -> {
                iv_playback?.visibility = View.VISIBLE
                iv_playback?.setImageDrawable(mPlayDrawable)
                bottomSheetBehavior?.let {
                    if (it.state != BottomSheetBehavior.STATE_HIDDEN) {
                        it.state = BottomSheetBehavior.STATE_HIDDEN
                    }
                }
            }
            else -> Log.d(TAG, "Unhandled state ")
        }
    }

    override fun showMessage(@StringRes msgResourceId: Int, isShortDuration: Boolean) {
        Toast.makeText(this, msgResourceId, if (isShortDuration) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
    }


    override fun showMessage(message: String, isShortDuration: Boolean) {
        Toast.makeText(this, message, if (isShortDuration) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        presenter.onRequestPermissionsResult(requestCode, permissions, grantResults)

    }

    override fun onStart() {
        super.onStart()
        presenter.onStartActivity()
    }

    override fun onResume() {
        super.onResume()
        setVolumeControlStream(AudioManager.STREAM_MUSIC)
    }

    private fun requestCurrentTrack() {
        mMediaBrowser?.let {
            it.sendCustomAction(CUSTOM_ACTION_GET_CURRENT_QUEUE_ITEM, null,
                    object : MediaBrowserCompat.CustomActionCallback() {
                        override fun onResult(action: String?, extras: Bundle?, resultData: Bundle?) {
                            if (!TextUtils.isEmpty(action) && CUSTOM_ACTION_GET_CURRENT_QUEUE_ITEM == action) {
                                resultData?.let {
                                    val item = it.getParcelable<MediaBrowserCompat.MediaItem>(CUSTOM_METADATA_TRACK_SOURCE)
                                    presenter.updateLastTrack(item.description)
                                }
                            }
                        }

                        override fun onError(action: String?, extras: Bundle?, data: Bundle?) {
                            Log.d(TAG, "getCurrentItemPlaying onError: ")
                        }
                    })
        }
    }

    override fun requestListMedia() {
        mMediaBrowser?.let {
            val root: String = it.root
            it.subscribe(root,
                    object : MediaBrowserCompat.SubscriptionCallback() {
                        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
                            adapter?.setDatas(children)
                            adapter?.notifyDataSetChanged()
                        }
                    }
            )
        }
    }

    override fun updateSbProgress() {

        presenter.lastTrackState?.let {
            var currentPosition = it.position
            if (it.state == PlaybackStateCompat.STATE_PLAYING) {
                val timeDelta = SystemClock.elapsedRealtime() - it.lastPositionUpdateTime
                currentPosition += (timeDelta.toInt() * it.playbackSpeed).toLong()
                tv_start?.text = DateUtils.formatElapsedTime((currentPosition / 1000))
                tv_start?.text = DateUtils.formatElapsedTime((currentPosition / 1000))
            }
            sbProgress?.setProgress(currentPosition.toInt())
        }
    }

    override fun scheduleSeekbarUpdate() {
        stopSeekbarUpdate()
        if (!mExecutorService.isShutdown) {
            mScheduleFuture = mExecutorService.scheduleAtFixedRate(
                    { mHandler.post(mUpdateProgressTask) }, PROGRESS_UPDATE_INITIAL_INTERVAL,
                    PROGRESS_UPDATE_INTERNAL, TimeUnit.MILLISECONDS)
        }
    }

    override fun stopSeekbarUpdate() {
        mScheduleFuture?.cancel(false)
    }

    override fun updateDuration(metadata: MediaMetadataCompat?) {
        metadata?.let {
            val duration = it.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).toInt()
            sbProgress?.max = duration
            tv_end?.text = DateUtils.formatElapsedTime((duration / 1000).toLong())
        }
    }

    override fun onMediaDescriptionChanged(description: MediaDescriptionCompat?) {
        if (description == null) {
            return
        }

        tv_title_bt?.text = description.title
        if (TextUtils.isEmpty(description.title)) tv_title_bt?.visibility = GONE else tv_title_bt?.visibility = VISIBLE

        tv_subtitle_bt?.text = description.subtitle
        if (TextUtils.isEmpty(description.subtitle)) tv_subtitle_bt?.visibility = GONE else tv_subtitle_bt?.visibility = VISIBLE


        fetchImageAsync(description)
        scene_root.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        bottomSheetBehavior?.peekHeight = scene_root.measuredHeight
    }

    private var mCurrentArtUrl: String? = null

    private fun fetchImageAsync(description: MediaDescriptionCompat) {
        description.iconUri?.let {
            val artUrl = it.toString()
            mCurrentArtUrl = artUrl
            val cache = AlbumArtCache.instance
            var art = cache.getBigImage(artUrl)
            if (art == null) {
                art = description.iconBitmap
            }
            if (art != null) {
                // if we have the art cached or from the MediaDescription, use it:
                ivBackgrounds?.setImageBitmap(art)
            } else {
                cache.fetch(this, artUrl, object : AlbumArtCache.FetchListener() {
                    override fun onFetched(artUrl: String, bigImage: Bitmap, iconImage: Bitmap) {
                        if (artUrl == mCurrentArtUrl) {
                            ivBackgrounds?.setImageBitmap(bigImage)
                        }
                    }
                })
            }
        }
        if (description.iconUri == null) {
            ivBackgrounds?.setImageResource(AlbumArtCache.instance.getDefaultResBitmap())
        }
    }

    private fun initScene() {
        ivBackgrounds = findViewById(R.id.ivBackground)
        initControls()
        presenter.updateLastTrack(mediaController?.metadata?.description)
        presenter.updateLastTrackState(mediaController?.playbackState)

        initBottomPage()
    }

    private fun initControls() {
        iv_playback?.setOnClickListener {
            val state = MediaControllerCompat.getMediaController(this@MainActivitySB).playbackState
            val controls = MediaControllerCompat.getMediaController(this@MainActivitySB).transportControls
            presenter.togglePlayPause(controls, state)
        }

        iv_skip_previous?.setOnClickListener {
            val state = MediaControllerCompat.getMediaController(this@MainActivitySB).playbackState
            val controls = MediaControllerCompat.getMediaController(this@MainActivitySB).transportControls
            presenter.onSkipClick(controls, state, false)
        }


        iv_skip_next?.setOnClickListener {
            val state = MediaControllerCompat.getMediaController(this@MainActivitySB).playbackState
            val controls = MediaControllerCompat.getMediaController(this@MainActivitySB).transportControls
            presenter.onSkipClick(controls, state, true)
        }

        sbProgress?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                presenter.onProgressChanged(seekBar, progress, fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                presenter.onStartProgressTrackingTouch()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                presenter.onProgressStopTrackingTouch(this@MainActivitySB, seekBar)
            }
        })
    }

    private fun initBottomPage() {
        // init the bottom sheet behavior
        bottomSheetBehavior = BottomSheetBehavior.from(scene_root)

        // change the state of the bottom sheet
        bottomSheetBehavior?.state = presenter.bottomPageState
        if (bottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
            constraint_container.progress = 1.0f
        }

        // set callback for changes
        bottomSheetBehavior?.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                constraint_container.progress = slideOffset
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                presenter.bottomPageState = newState
            }
        })
    }

    override fun performConnectToService() {
        if (!isExternalStorageAvailable()) {
            showMessage("no storage", false)
        } else {
            presenter.performRequestData(this)
        }
    }

    override fun updateInListLastTrack(item: MediaDescriptionCompat?) {
        adapter?.let {
            if (it.setItemPlaying(item)) {
                it.notifyDataSetChanged()
            }
        }
    }

    override fun updateInListLastTrackState(state: PlaybackStateCompat?) {
        adapter?.let {
            if (it.setItemState(state)) {
                it.notifyDataSetChanged()
            }
        }
    }

    override fun updatePlayingProgress(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        tv_start?.text = DateUtils.formatElapsedTime((progress / 1000).toLong())
    }

}
