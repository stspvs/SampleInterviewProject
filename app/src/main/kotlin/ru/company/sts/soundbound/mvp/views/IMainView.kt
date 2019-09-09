package ru.company.sts.soundbound.mvp.views

import android.support.annotation.MainThread
import android.support.annotation.StringRes
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.SeekBar
import com.arellomobile.mvp.MvpView
import com.arellomobile.mvp.viewstate.strategy.AddToEndSingleStrategy
import com.arellomobile.mvp.viewstate.strategy.OneExecutionStateStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType

/**
 * Created by sts on 22.03.2018.
 */
@StateStrategyType(AddToEndSingleStrategy::class)
interface IMainView : MvpView {

    @MainThread
    @StateStrategyType(AddToEndSingleStrategy::class)
    fun init()

    @MainThread
    @StateStrategyType(OneExecutionStateStrategy::class)
    fun showMessage(@StringRes msgResourceId: Int, isShortDuration: Boolean)

    @MainThread
    @StateStrategyType(OneExecutionStateStrategy::class)
    fun showMessage(message: String, isShortDuration: Boolean)

    @MainThread
    @StateStrategyType(AddToEndSingleStrategy::class)
    fun onPlayBackStateChanged(state: PlaybackStateCompat?)

    @MainThread
    @StateStrategyType(AddToEndSingleStrategy::class)
    fun afterConnectToSession(token: MediaSessionCompat.Token?)

    @MainThread
    @StateStrategyType(OneExecutionStateStrategy::class)
    fun performConnectToService()

    @MainThread
    @StateStrategyType(OneExecutionStateStrategy::class)
    fun updateInListLastTrack(item: MediaDescriptionCompat?)

    @MainThread
    @StateStrategyType(OneExecutionStateStrategy::class)
    fun updateInListLastTrackState(state: PlaybackStateCompat?)

    @MainThread
    @StateStrategyType(OneExecutionStateStrategy::class)
    fun updateSbProgress()

    @MainThread
    @StateStrategyType(OneExecutionStateStrategy::class)
    fun scheduleSeekbarUpdate()

    @MainThread
    @StateStrategyType(AddToEndSingleStrategy::class)
    fun requestListMedia()

    @MainThread
    @StateStrategyType(OneExecutionStateStrategy::class)
    fun stopSeekbarUpdate()

    @MainThread
    @StateStrategyType(AddToEndSingleStrategy::class)
    fun updatePlayingProgress(seekBar: SeekBar, progress: Int, fromUser: Boolean)

    @MainThread
    @StateStrategyType(OneExecutionStateStrategy::class)
    fun onMediaDescriptionChanged(description: MediaDescriptionCompat?)

    @MainThread
    @StateStrategyType(AddToEndSingleStrategy::class)
    fun updateDuration(metadata: MediaMetadataCompat?)

    @MainThread
    @StateStrategyType(AddToEndSingleStrategy::class)
    fun connectToMediaService()
}