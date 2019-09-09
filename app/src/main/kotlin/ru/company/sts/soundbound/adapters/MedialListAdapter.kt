package ru.company.sts.soundbound.adapters

import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import ru.company.sts.soundbound.R
import ru.company.sts.soundbound.model.IHolderClickObserver
import ru.company.sts.soundbound.model.SelectionHolderHelper
import ru.company.sts.soundbound.utils.AlbumArtCache


/**
 * Created by sts on 22.03.2018.
 */
class MedialListAdapter(val mPlayDrawable: Drawable?, val mPauseDrawable: Drawable?,
                        listener: IHolderClickObserver) : RecyclerView.Adapter<MedialListAdapter.ViewHolder>() {
    private var selectionHelper: SelectionHolderHelper? = null
    private var datas: List<MediaBrowserCompat.MediaItem>? = null

    private var itemPlaying: MediaDescriptionCompat? = null
    private var itemState: PlaybackStateCompat? = null

    init {
        selectionHelper = SelectionHolderHelper()
        selectionHelper?.registerHolderClickObserver(listener)
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder? {
        val inflater = LayoutInflater.from(parent?.context)
        val view = inflater.inflate(R.layout.item_media_in_player, parent, false)
        return selectionHelper?.wrapClickable(ViewHolder(view))
    }

    override fun getItemCount(): Int {
        return datas?.size ?: 0
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        datas?.get(position)?.let { holder?.setDatas(it) }
    }

    fun setDatas(datas: List<MediaBrowserCompat.MediaItem>?) {
        this.datas = datas
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var track: MediaBrowserCompat.MediaItem? = null
        private val tvTitle: TextView? = view.findViewById(R.id.tv_title)
        private val ivLogo: ImageView? = view.findViewById(R.id.iv_media_image)
        private val ivControl: ImageView? = view.findViewById(R.id.iv_media_control)
        private val cvBackground: CardView? = view.findViewById(R.id.cv_bg)

        fun setDatas(track: MediaBrowserCompat.MediaItem) {
            this.track = track
            val description: MediaDescriptionCompat = track.description
            tvTitle?.text = description.title

            if (description.iconUri != null) {
                description.iconUri?.let {
                    val uriArt: String = it.toString()
                    ivLogo?.setImageURI(Uri.parse(uriArt))
                }
            } else {
                ivLogo?.setImageResource(AlbumArtCache.instance.getDefaultResBitmap())
            }

            if (itemPlaying != null && itemPlaying!!.mediaId == track.mediaId) {
                ivControl?.visibility = View.VISIBLE
                var isPlaying: Boolean? = null
                itemState?.let {
                    isPlaying = (it.state == PlaybackStateCompat.STATE_PLAYING || it.state == PlaybackStateCompat.STATE_BUFFERING)
                }

                if (isPlaying != null) {
                    ivControl?.setImageDrawable(if (isPlaying!!) mPauseDrawable else mPlayDrawable)
                } else {
                    ivControl?.visibility = View.GONE
                }
            } else {
                ivControl?.visibility = View.GONE
                /*val states: IntArray = intArrayOf(android.R.attr.state_enabled)
                val colors: IntArray = intArrayOf(selectedBackGroundColor)

                val lala: Array<IntArray> = arrayOf(states, colors)
                cvBackground?.cardBackgroundColor = ColorStateList(lala)*/
            }
        }
    }

    fun setItemPlaying(itemPlaying: MediaDescriptionCompat?): Boolean {
        return if (this.itemPlaying != itemPlaying) {
            this.itemPlaying = itemPlaying
            true
        } else {
            false
        }
    }

    fun setItemState(itemState: PlaybackStateCompat?): Boolean {
        return if (this.itemState != itemState) {
            this.itemState = itemState
            true
        } else {
            false
        }
    }
}