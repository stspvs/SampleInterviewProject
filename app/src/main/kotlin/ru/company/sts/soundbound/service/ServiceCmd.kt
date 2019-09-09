package ru.company.sts.soundbound.service

// Extra on MediaSession that contains the Cast device name currently connected to
val EXTRA_CONNECTED_CAST = "com.example.android.uamp.CAST_NAME"
// The action of the incoming Intent indicating that it contains a command
// to be executed (see {@link #onStartCommand})
val ACTION_CMD = "com.example.android.uamp.ACTION_CMD"
// The key in the extras of the incoming Intent indicating the command that
// should be executed (see {@link #onStartCommand})
val CMD_NAME = "CMD_NAME"
// A value of a CMD_NAME key in the extras of the incoming Intent that
// indicates that the music playback should be paused (see {@link #onStartCommand})
val CMD_PAUSE = "CMD_PAUSE"
// A value of a CMD_NAME key that indicates that the music playback should switch
// to local playback from cast playback.
val CMD_STOP_CASTING = "CMD_STOP_CASTING"