package ru.company.sts.soundbound.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.ExtractorsFactory
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.Util
import okhttp3.OkHttpClient
import ru.company.sts.soundbound.R
import ru.company.sts.soundbound.model.ICallBackListMediaReady
import ru.company.sts.soundbound.model.MusicProvider
import ru.company.sts.soundbound.mvp.activities.MainActivitySB
import ru.company.sts.soundbound.playback.MediaNotificationManager
import ru.company.sts.soundbound.playback.PlaybackLocal
import ru.company.sts.soundbound.playback.PlaybackManager
import ru.company.sts.soundbound.playback.QueueManager
import ru.company.sts.soundbound.utils.CUSTOM_METADATA_TRACK_SOURCE
import ru.company.sts.soundbound.utils.Log
import java.io.File

/**
 * Created by sts on 20.03.2018.
 */
class PlayerService : MediaBrowserServiceCompat(), PlaybackManager.PlaybackServiceCallback {
    companion object {
        private const val TAG = "PlayerService"
        private const val NOTIFICATION_ID = 687
        private const val NOTIFICATION_DEFAULT_CHANNEL_ID: String = "CHANNEL_SOUNDBOUND_SERVICE"
    }

/*    private val stateBuilder: PlaybackStateCompat.Builder = PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY
            or PlaybackStateCompat.ACTION_STOP
            or PlaybackStateCompat.ACTION_PAUSE
            or PlaybackStateCompat.ACTION_PLAY_PAUSE
            or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            or PlaybackStateCompat.ACTION_PLAY_FROM_URI
            or PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
    )*/

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var audioManager: AudioManager

    private var extractorsFactory: ExtractorsFactory? = null
    private var dataSourceFactory: DataSource.Factory? = null

    private var mediaProvider: MusicProvider? = null
    private var mPlaybackManager: PlaybackManager? = null
    private var mMediaNotificationManager: MediaNotificationManager? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        //first of all request media list
        mediaProvider = MusicProvider()
        mediaProvider?.requestMediaList(this, null)
        mediaProvider?.let {
            val queueManager = QueueManager(it, resources,
                    object : QueueManager.MetadataUpdateListener {
                        override fun onMetadataChanged(metadata: MediaMetadataCompat) {
                            mediaSession.setMetadata(metadata)
                        }

                        override fun onMetadataRetrieveError() {
                            mPlaybackManager?.updatePlaybackState(
                                    getString(R.string.error_no_metadata))
                        }

                        override fun onCurrentQueueIndexUpdated(queueIndex: Int) {
                            mPlaybackManager?.handlePlayRequest()
                        }

                        override fun onQueueUpdated(title: String, newQueue: List<MediaSessionCompat.QueueItem>?) {
                            mediaSession.setQueue(newQueue)
                            mediaSession.setQueueTitle(title)
                        }
                    })

            val playback = PlaybackLocal(this, it)
            mPlaybackManager = PlaybackManager(this, resources, it, queueManager,
                    playback)
        }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager


        mediaSession = MediaSessionCompat(this, "StsPlayerService")
        sessionToken = mediaSession.sessionToken
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mPlaybackManager?.let { mediaSession.setCallback(it.mediaSessionCallback) }


        val appContext = applicationContext

        val activityIntent = Intent(appContext, MainActivitySB::class.java)
        mediaSession.setSessionActivity(PendingIntent.getActivity(appContext, 0, activityIntent, 0))

        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON, null, appContext, MediaButtonReceiver::class.java)
        mediaSession.setMediaButtonReceiver(PendingIntent.getBroadcast(appContext, 0, mediaButtonIntent, 0))

        val httpDataSourceFactory = OkHttpDataSourceFactory(OkHttpClient(), Util.getUserAgent(this, getString(R.string.app_name)), null)
        val cache = SimpleCache(File(this.cacheDir.absolutePath + "/exoplayer"), LeastRecentlyUsedCacheEvictor((1024 * 1024 * 100).toLong())) // 100 Mb max
        this.dataSourceFactory = CacheDataSourceFactory(cache, httpDataSourceFactory, CacheDataSource.FLAG_BLOCK_ON_CACHE or CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        this.extractorsFactory = DefaultExtractorsFactory()
        //mediaRepository.getCurrent().uriSource?.let { prepareToPlay(it) }

        mPlaybackManager?.updatePlaybackState(null)
        try {
            mMediaNotificationManager = MediaNotificationManager(this)
        } catch (e: RemoteException) {
            throw IllegalStateException("Could not create a MediaNotificationManager", e)
        }

    }


    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        Log.d(TAG, "onGetRoot")
        //return MediaBrowserServiceCompat.BrowserRoot("Root", null)
        return BrowserRoot("Root", null)
    }


    override fun onCustomAction(action: String, extras: Bundle?, result: Result<Bundle>) {
        mPlaybackManager?.let {
            val bundle = Bundle()
            val item = it.getCurrentPlayingItem()
            item?.let {
                bundle.putParcelable(CUSTOM_METADATA_TRACK_SOURCE, it)
                result.sendResult(bundle)
            }

        }
    }

    override fun onLoadItem(itemId: String?, result: Result<MediaBrowserCompat.MediaItem>) {
        if (mediaProvider != null && mediaProvider!!.isInitialized()) {
            itemId?.let {
                result.sendResult(mediaProvider?.getItem(it))
            }
        }
    }

    override fun onLoadChildren(parentId: String, result: MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>>) {
        mediaProvider?.let {
            if (it.isInitialized()) {
                // if music library is ready, return immediately
                result.sendResult(it.getChildren())
            } else {
                // otherwise, only return results when the music library is retrieved
                result.detach()
                it.requestMediaList(this,
                        object : ICallBackListMediaReady {
                            override fun onMusicListReady(success: Boolean) {
                                result.sendResult(it.getChildren())
                            }
                        }
                )
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind")
        if (MediaBrowserServiceCompat.SERVICE_INTERFACE == intent?.action) {
            return super.onBind(intent)
        }

        return PlayerServiceBinder()
    }

    inner class PlayerServiceBinder : Binder() {
        val mediaSessionToken: MediaSessionCompat.Token
            get() = mediaSession.sessionToken
    }

    private fun refreshNotificationAndForegroundStatus(playbackState: Int) {
        Log.d(TAG, "refreshNotificationAndForegroundStatus")
        when (playbackState) {
            PlaybackStateCompat.STATE_PLAYING -> {
                startForeground(NOTIFICATION_ID, getNotification(playbackState))
            }
            PlaybackStateCompat.STATE_PAUSED -> {
                NotificationManagerCompat.from(this@PlayerService).notify(NOTIFICATION_ID, getNotification(playbackState))
                stopForeground(false)
            }
            else -> {
                stopForeground(true)
            }
        }
    }

    private fun getNotification(playbackState: Int): Notification {
        Log.d(TAG, "getNotification")
        val builder = MediaStyleHelper().from(this, mediaSession, NOTIFICATION_DEFAULT_CHANNEL_ID)
        builder.addAction(NotificationCompat.Action(android.R.drawable.ic_media_previous, getString(R.string.previous), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)))
        if (playbackState == PlaybackStateCompat.STATE_PLAYING) {
            builder.addAction(NotificationCompat.Action(android.R.drawable.ic_media_pause, getString(R.string.pause), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)))
        } else {
            builder.addAction(NotificationCompat.Action(android.R.drawable.ic_media_play, getString(R.string.play), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)))
        }
        builder.addAction(NotificationCompat.Action(android.R.drawable.ic_media_next, getString(R.string.next), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)))
        builder.setStyle(android.support.v4.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1)
                .setShowCancelButton(true)
                .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP))
                .setMediaSession(this.mediaSession.sessionToken)) // setMediaSession требуется для Android Wear
        builder.setSmallIcon(R.mipmap.ic_bg_main)
        builder.color = ContextCompat.getColor(this, R.color.colorPrimaryDark) // The whole background (in MediaStyle), not just icon background
        builder.setShowWhen(false)
        builder.priority = NotificationCompat.PRIORITY_HIGH
        builder.setOnlyAlertOnce(true)
        builder.setChannelId(NOTIFICATION_DEFAULT_CHANNEL_ID)

        return builder.build()
    }

    override fun onStartCommand(startIntent: Intent, flags: Int, startId: Int): Int {
        // val action = startIntent.getAction()
        // Try to handle the intent as a media button event wrapped by MediaButtonReceiver
        MediaButtonReceiver.handleIntent(mediaSession, startIntent)

        // Reset the delay handler to enqueue a message to stop the service if
        // nothing is playing.

        Log.d(TAG, "     onStartCommand 3 startIntent = $startIntent starflagsId = $flags startId = $startId")
        return Service.START_STICKY
        //return Service.START_NOT_STICKY
        // Log.d(TAG,"onStartCommand")
        // MediaButtonReceiver.handleIntent(mediaSession, startIntent)
        // return super.onStartCommand(startIntent, flags, startId)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        mediaSession.release()
        mediaProvider?.onDestroy()
        super.onDestroy()

    }

    override fun onPlaybackStart() {
        mediaSession.isActive = true

        // The service needs to continue running even after the bound client (usually a
        // MediaController) disconnects, otherwise the music playback will stop.
        // Calling startService(Intent) will keep the service running until it is explicitly killed.
        startService(Intent(applicationContext, PlayerService::class.java))
    }

    override fun onNotificationRequired() {
        mMediaNotificationManager?.startNotification()
    }

    override fun onPlaybackStop() {
        mediaSession.isActive = false
        // Reset the delayed stop handler, so after STOP_DELAY it will be executed again,
        // potentially stopping the service.
        stopForeground(true)
    }

    override fun onPlaybackStateUpdated(newState: PlaybackStateCompat) {
        mediaSession.setPlaybackState(newState)
    }


}