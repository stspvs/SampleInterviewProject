package ru.company.sts.soundbound.mvp.presenters

import android.app.Activity
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.widget.RecyclerView
import android.widget.SeekBar
import com.arellomobile.mvp.InjectViewState
import com.arellomobile.mvp.MvpPresenter
import ru.company.sts.soundbound.R
import ru.company.sts.soundbound.adapters.MedialListAdapter
import ru.company.sts.soundbound.mvp.views.IMainView
import ru.company.sts.soundbound.permissions.DynamicPermissionsHelper
import ru.company.sts.soundbound.utils.Log


/**
 * Created by sts on 22.03.2018.
 */
@InjectViewState
class PresenterMain : MvpPresenter<IMainView>() {
    companion object {
        private const val TAG = "PresenterMain"
        private const val REQUEST_CODE_WHILE_OPENING_LIST_MEDIA = 9450
    }

    private var dynamicPermissionsHelper: DynamicPermissionsHelper? = null
    var lastTrackState: PlaybackStateCompat? = null
    var lastTrack: MediaDescriptionCompat? = null
    var bottomPageState: Int = BottomSheetBehavior.STATE_HIDDEN

    fun onStartActivity(){
        viewState.connectToMediaService()

        val path = arrayOf(Direction.UP, Direction.LEFT, Direction.LEFT, Direction.UP, Direction.LEFT, Direction.UP, Direction.RIGHT, Direction.UP, Direction.UP, Direction.UP, Direction.RIGHT)
        val radius = 5
        checkPath(path, radius)
    }

    enum class Direction {
        UP, RIGHT, DOWN, LEFT
    }
    fun checkPath(path: Array<Direction>, radius: Int): Boolean {
        var xOffset = 0
        var yOffset = 0
        var inArea = false
        for(direction in path){
            if (direction == Direction.LEFT)
                xOffset++
            if (direction == Direction.RIGHT)
                xOffset--
            if (direction == Direction.DOWN)
                yOffset--
            if (direction == Direction.UP)
                yOffset++
            inArea = (xOffset >= radius*(-1) && xOffset <= radius && yOffset >= radius*(-1) && yOffset <= radius)
            if (!inArea)
                return inArea
        }
        return true
    }
    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (dynamicPermissionsHelper != null) {
            if (permissions.isEmpty()) {
                Log.w(TAG, "Discrepancy during permissions request!")
            } else {
                dynamicPermissionsHelper?.onRequestPermissionsResult(requestCode, permissions, grantResults)
                dynamicPermissionsHelper = null
            }
        }
    }

    fun onHolderClick(context: Activity, mediaController: MediaControllerCompat?, holder: RecyclerView.ViewHolder?) {
        if (mediaController != null && holder is MedialListAdapter.ViewHolder && holder.track != null) {
            if (holder.track!!.isPlayable) run {
                MediaControllerCompat.getMediaController(context).transportControls
                        .playFromMediaId(holder.track!!.mediaId, null)
            }
            mediaController.transportControls?.play()
        }
    }

    fun onMetadataChanged(metadata: MediaMetadataCompat?) {
        updateLastTrack(metadata?.description)
        viewState.onMediaDescriptionChanged(metadata?.description)
        viewState.updateDuration(metadata)
    }

    fun onConnectTosession(token: MediaSessionCompat.Token?) {
        viewState.afterConnectToSession(token)
    }

    fun afterMediaControllerCreated(mediaController: MediaControllerCompat?) {
        updateLastTrackState(mediaController?.playbackState)
        onMetadataChanged(mediaController?.metadata)

        viewState.updateSbProgress()

        viewState.requestListMedia()
    }

    fun updateLastTrackState(trackState: PlaybackStateCompat?) {
        if (trackState != lastTrackState) {
            lastTrackState = trackState
            viewState.updateInListLastTrackState(lastTrackState)
            viewState.onPlayBackStateChanged(lastTrackState)
            lastTrackState?.let {
                viewState.scheduleSeekbarUpdate()
            }
        }
    }

    fun updateLastTrack(track: MediaDescriptionCompat?) {
        if (lastTrack != track) {
            lastTrack = track
            viewState.updateInListLastTrack(lastTrack)
            viewState.onMediaDescriptionChanged(lastTrack)
        }
    }

    fun performRequestData(activity: Activity) {
        dynamicPermissionsHelper = DynamicPermissionsHelper(activity, DynamicPermissionsHelper.READ_EXTERNAL_STORAGE,
                activity.getString(R.string.msg_request_read_sd_explanation_open_list_media))
        if (!dynamicPermissionsHelper!!.checkPermissionsAreGranted(activity)) {
            dynamicPermissionsHelper!!.requestMissingPermissions(activity, REQUEST_CODE_WHILE_OPENING_LIST_MEDIA,
                    object : DynamicPermissionsHelper.IPermissionRequestResultListener {
                        override fun onAllPermissionsGranted() {
                            viewState.init()
                        }

                        override fun onAnyPermissionDenied() {
                            viewState.showMessage(R.string.msg_request_read_sd_denied_read_list_media, false)
                        }
                    })
        } else {
            viewState.init()
        }
    }

    fun togglePlayPause(controls: MediaControllerCompat.TransportControls?, trackState: PlaybackStateCompat?) {
        updateLastTrackState(trackState)
        controls?.let {
            when (lastTrackState?.state) {
                PlaybackStateCompat.STATE_PLAYING // fall through
                    , PlaybackStateCompat.STATE_BUFFERING -> {
                    it.pause()
                    viewState.stopSeekbarUpdate()
                }
                PlaybackStateCompat.STATE_PAUSED, PlaybackStateCompat.STATE_STOPPED -> {
                    it.play()
                    viewState.scheduleSeekbarUpdate()
                }
                else -> Log.d(TAG, "onClick with state ")
            }
        }
    }

    fun onSkipClick(controls: MediaControllerCompat.TransportControls?, trackState: PlaybackStateCompat?, isNext: Boolean) {
        updateLastTrackState(trackState)
        when (lastTrackState?.state) {
            PlaybackStateCompat.STATE_PLAYING // fall through
                , PlaybackStateCompat.STATE_BUFFERING -> {
                viewState.stopSeekbarUpdate()
            }
            else -> Log.d(TAG, "onSkipNextClick without state ")
        }

        if (isNext)
            controls?.skipToNext()
        else
            controls?.skipToPrevious()
    }

    fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        viewState.updatePlayingProgress(seekBar, progress, fromUser)
    }

    fun onStartProgressTrackingTouch() {
        viewState.stopSeekbarUpdate()
    }

    fun onProgressStopTrackingTouch(activity: Activity, seekBar: SeekBar) {
        MediaControllerCompat.getMediaController(activity).transportControls.seekTo(seekBar.progress.toLong())
        viewState.scheduleSeekbarUpdate()
    }
}