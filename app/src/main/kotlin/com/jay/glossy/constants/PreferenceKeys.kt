/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.constants

import com.jay.glossy.R

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.time.LocalDateTime
import java.time.ZoneOffset

val EnableHighRefreshRateKey = booleanPreferencesKey("enableHighRefreshRate")
val EnableLandscapeScalingKey = booleanPreferencesKey("enableLandscapeScaling")
<<<<<<< HEAD

/**
 * Turns off the app's ambient, always-running animations — the mini player's
 * equalizer and drifting glow, the wash behind the home header, the notes over
 * the vinyl record.
 *
 * Low-RAM phones start with this on, so a budget device is calm without anyone
 * having to find the switch. Android's own "Remove animations" developer
 * setting is obeyed on top of it either way — see
 * `rememberAmbientMotionEnabled`.
 */
val ReduceMotionKey = booleanPreferencesKey("reduceMotion")
=======
>>>>>>> origin/main
val DynamicThemeKey = booleanPreferencesKey("dynamicTheme")
val SelectedThemeColorKey = intPreferencesKey("selectedThemeColor")
val DarkModeKey = stringPreferencesKey("darkMode")
val PureBlackKey = booleanPreferencesKey("pureBlack")
val PureBlackMiniPlayerKey = booleanPreferencesKey("pureBlackMiniPlayer")
val MiniPlayerOutlineKey = booleanPreferencesKey("miniPlayerOutline")
val MiniPlayerBackgroundStyleKey = stringPreferencesKey("miniPlayerBackgroundStyle")

enum class MiniPlayerBackgroundStyle {
    DEFAULT,
    TRANSPARENT,
    BLUR,
    GRADIENT,
    PURE_BLACK,
<<<<<<< HEAD

    /** Drifting, pulsing artwork-palette glow behind the mini player. */
    GLOW,
}

/**
 * How the mini player marks the track that is playing: the flat equalizer bars,
 * or small music notes drifting upwards.
 *
 * Both are frozen — not hidden — while playback is paused, so the song row never
 * shifts when the music stops. See `NowPlayingAnimationIndicator`.
 */
val MiniPlayerPlayingAnimationKey = stringPreferencesKey("miniPlayerPlayingAnimation")

enum class MiniPlayerPlayingAnimation {
    /** Four bars that rise and fall. The default, and the cheapest to draw. */
    BARS,

    /** Three small notes that float out of the indicator and fade away. */
    NOTES,
=======
    ANIMATED_MESH,
>>>>>>> origin/main
}

val DensityScaleKey = floatPreferencesKey("density_scale_factor")
val CustomDensityScaleKey = floatPreferencesKey("custom_density_scale_value")

enum class DensityScale(
    val value: Float,
    val label: String,
) {
    NATIVE(1.0f, "Native (100%)"),
    SLIGHTLY_COMPACT(0.85f, "Slightly Compact (85%)"),
    COMPACT(0.75f, "Compact (75%)"),
    VERY_COMPACT(0.65f, "Very Compact (65%)"),
    ULTRA_COMPACT(0.55f, "Ultra Compact (55%)"),
    ;

    companion object {
        fun fromValue(value: Float): DensityScale = entries.find { it.value == value } ?: NATIVE
    }
}

val DefaultOpenTabKey = stringPreferencesKey("defaultOpenTab")
val SlimNavBarKey = booleanPreferencesKey("slimNavBar")
val UseFloatingNavBarKey = booleanPreferencesKey("useFloatingNavBar")

// App-wide background blur (frosted gradient backdrop behind Home/Library/etc.)
val BackgroundBlurEnabledKey = booleanPreferencesKey("backgroundBlurEnabled")
val BackgroundBlurStrengthKey = floatPreferencesKey("backgroundBlurStrength")
val GridItemsSizeKey = stringPreferencesKey("gridItemSize")
val SliderStyleKey = stringPreferencesKey("sliderStyle")
val SquigglySliderKey = booleanPreferencesKey("squigglySlider")
val SwipeToSongKey = booleanPreferencesKey("SwipeToSong")
val SwipeToRemoveSongKey = booleanPreferencesKey("SwipeToRemoveSong")
val UseNewPlayerDesignKey = booleanPreferencesKey("useNewPlayerDesign")

val PlayerStyleKey = stringPreferencesKey("playerStyle")

enum class PlayerStyle {
    CLASSIC,
    MODERN,
    WAVY,
    VIVI_NEW,
<<<<<<< HEAD
    APPLE_MUSIC,

    /** Spinning vinyl record with the animated canvas rendered on the disc. */
    VINYL,

    /** Maroon gradient with the transport parked inside one dark pill. */
    CAPSULE,

    /** Full-bleed album artwork behind glass controls. */
    CINEMATIC
=======
    APPLE_MUSIC
>>>>>>> origin/main
}

val MiniPlayerStyleKey = stringPreferencesKey("miniPlayerStyle")

enum class MiniPlayerStyle {
    LEGACY,
    MODERN,
<<<<<<< HEAD
    GLOSSY_SPECIAL,

    /** The teal design system's compact bar with a teal progress hairline. */
    STUDIO
}

/**
 * Which visual language the app paints itself with.
 *
 * [TEAL] is the dark design system with the fixed palette, and [CLASSIC] is
 * the previous Material You look. Only dark mode is themed: light mode keeps
 * the Material You scheme in both cases.
 */
val DesignStyleKey = stringPreferencesKey("designStyle")

/**
 * The look the app actually paints with.
 *
 * The app runs the Materialistic skin: the redesigned screens keep their
 * layout, but every colour comes from the Material 3 scheme — tonal surfaces,
 * the primary role as the accent, a `primaryContainer` home header — so the
 * whole app repaints from the seed colour and follows Material You on
 * Android 12+. The classic near-black/teal palette and the teal design are
 * still described by [DesignStyle] so those code paths keep compiling, but
 * nothing selects them.
 */
val ActiveDesignStyle = DesignStyle.MATERIALISTIC

/** Nickname a guest picks on the welcome screen. */
val GuestNameKey = stringPreferencesKey("guest_name")

/**
 * Set by the sign-in flow right before it restarts the app, so the relaunched
 * process can show the community page as the last onboarding step instead of
 * dropping the user straight on Home.
 */
val PendingCommunityIntroKey = booleanPreferencesKey("pending_community_intro")

/**
 * Material You wallpaper colour for the classic look. Off by default so the
 * app opens on its own brand colour instead of whatever wallpaper is set;
 * the new design ignores it either way.
 */
val DynamicColorEnabledKey = booleanPreferencesKey("dynamicColorEnabled")

/**
 * Only start downloads on an unmetered (Wi-Fi) connection. On mobile data the
 * download is skipped with a short explanation instead of silently burning the
 * user's data plan.
 */
val DownloadOverWifiOnlyKey = booleanPreferencesKey("downloadOverWifiOnly")

enum class DesignStyle {
    /** The hand-tuned dark design with the fixed teal palette. */
    TEAL,

    /** The previous Material You styling, with the classic Glossy tokens. */
    CLASSIC,

    /** Material 3 tonal skin: the Glossy tokens read the scheme. */
    MATERIALISTIC,
    ;

    companion object {
        fun fromValue(value: String?): DesignStyle = entries.find { it.name == value } ?: TEAL
    }
=======
    GLOSSY_SPECIAL
>>>>>>> origin/main
}

val UseNewMiniPlayerDesignKey = booleanPreferencesKey("useNewMiniPlayerDesign")
val HidePlayerThumbnailKey = booleanPreferencesKey("hidePlayerThumbnail")
val CropAlbumArtKey = booleanPreferencesKey("cropAlbumArt")
<<<<<<< HEAD

/**
 * Drops a shadow under the album artwork in the player and the mini player.
 * On by default. minSdk is 26, so the shadow stays plain black: a coloured spot
 * colour is only honoured from API 28 up.
 */
val ThumbnailShadowKey = booleanPreferencesKey("thumbnailShadow")
=======
>>>>>>> origin/main
val SeekExtraSeconds = booleanPreferencesKey("seekExtraSeconds")
val PauseOnMute = booleanPreferencesKey("pauseOnMute")
val ResumeOnBluetoothConnectKey = booleanPreferencesKey("resumeOnBluetoothConnect")
val KeepScreenOn = booleanPreferencesKey("keepScreenOn")
val AlarmEnabledKey = booleanPreferencesKey("alarmEnabled")
val AlarmHourKey = intPreferencesKey("alarmHour")
val AlarmMinuteKey = intPreferencesKey("alarmMinute")
val AlarmPlaylistIdKey = stringPreferencesKey("alarmPlaylistId")
val AlarmRandomSongKey = booleanPreferencesKey("alarmRandomSong")
val AlarmNextTriggerAtKey = longPreferencesKey("alarmNextTriggerAt")
val AlarmEntriesKey = stringPreferencesKey("alarmEntries")
val DeveloperModeKey = booleanPreferencesKey("developerMode")
val CanvasThumbnailAnimationKey = booleanPreferencesKey("canvasThumbnailAnimation")

<<<<<<< HEAD
/**
 * Animated canvases on the home screen's Featured Spotlight carousel.
 *
 * Deliberately its own switch, and on by default. The carousel is a browsing
 * surface, not the player: someone who keeps "Canvas Background" off because a
 * full-screen moving backdrop behind the lyrics is distracting may still want
 * the featured cards to move. It also has its own data cost — up to one clip
 * per visible card — so it is worth being able to turn off on its own.
 */
val SpotlightCanvasKey = booleanPreferencesKey("spotlightCanvasAnimation")

/**
 * Whether animated canvases may also load on mobile data. On by default — a
 * canvas is a looping video download, so this is the switch to turn off for
 * anyone who wants animated artwork to stay on Wi-Fi, and with it off a
 * cellular connection simply keeps the static artwork instead of buffering
 * video. Governs every canvas surface, the Spotlight carousel included.
 *
 * Answered by the connection's transport, not by "metered": Wi-Fi the system
 * has flagged as metered still counts as Wi-Fi here (see
 * `isMobileDataConnection`).
 */
val CanvasOnMobileDataKey = booleanPreferencesKey("canvasOnMobileData")

/**
 * Whether upcoming canvases are prepared before they are on screen.
 *
 * A canvas is a provider lookup plus a video download, so without this the
 * card (or the next track) only starts that work once it is already the one
 * being shown — which is exactly the late arrival the preload exists to fix.
 * On by default; off means nothing is resolved or downloaded until a canvas is
 * actually visible. The mobile-data rule still applies on top of it.
 */
val CanvasPreloadKey = booleanPreferencesKey("canvasPreload")

/**
 * Corner radius, in dp, of every animated-canvas surface (the player artwork
 * and the Featured Spotlight cards). Kept as a plain int so the Appearance
 * slider can write it directly; 28dp is the Spotlight card's original shape.
 */
val CanvasCornerSizeKey = intPreferencesKey("canvasCornerSize")

/** Default [CanvasCornerSizeKey]: the Spotlight card's design radius. */
const val DefaultCanvasCornerSize = 28

/** Square, so a canvas can be drawn edge to edge in its slot. */
const val MinCanvasCornerSize = 0

/** Fully round for a square canvas: half of the 252dp Spotlight card. */
const val MaxCanvasCornerSize = 48

/**
 * Shows the current synced lyric line on the now-playing screen itself, in
 * every player design. Toggled from the player overflow menu
 * ("Show Lyrics" / "Hide Lyrics") and remembered across restarts.
 */
val ShowLyricsOnPlayerKey = booleanPreferencesKey("showLyricsOnPlayer")

/**
 * How the line being sung is shown on the now-playing screen when
 * [ShowLyricsOnPlayerKey] is on. Chosen in Appearance.
 */
val MiniLyricsStyleKey = stringPreferencesKey("miniLyricsStyle")

enum class MiniLyricsStyle {
    /**
     * The original strip: a frosted card under the artwork carrying the active
     * line large and the next line dimmed under it. It lives *beside* the
     * artwork, so it takes height from the design.
     */
    CLASSIC,

    /**
     * The active line is drawn on the artwork itself, over the animated canvas,
     * glowing in the accent colour. Nothing is taken from the layout — the lyric
     * sits in space the design already leaves empty — and tapping it still opens
     * the full lyrics view.
     */
    CANVAS_GLOW,
}

/**
 * Word-by-word animation style for mini lyrics on the player.
 * Chosen in Appearance.
 */
val MiniLyricsAnimationStyleKey = stringPreferencesKey("miniLyricsAnimationStyle")

enum class MiniLyricsAnimationStyle {
    NONE,
    FADE,
    GLOW,
    SLIDE,
    KARAOKE,
    APPLE,
}

=======
>>>>>>> origin/main
/** Which canvas provider strategy to use for animated artwork. */
val CanvasStyleKey = stringPreferencesKey("canvasStyle")
enum class CanvasStyle {
    /**
     * Race every provider (Spotify + ArchiveTune + Tidal + Apple = the
     * Glossy engine) concurrently and show the first canvas that comes back.
     */
    ALL,
    /** Glossy engine: Tidal + Apple Music (original behavior). */
    GLOSSY,
    /** ArchiveTune engine: BetterLyrics community artwork service. */
    ARCHIVE_TUNE,
    /** Try BetterLyrics first, then fall back to Tidal + Apple Music. */
    BOTH,
    /** Spotify web-player Canvas artwork. */
    SPOTIFY,
}

val CanvasCacheModeKey = stringPreferencesKey("canvasCacheMode")
enum class CanvasCacheMode {
    URL_ONLY,
    VIDEO_AND_URL
}

enum class SliderStyle {
    DEFAULT,
    WAVY,
    SLIM,
}

const val SYSTEM_DEFAULT = "SYSTEM_DEFAULT"
val AppLanguageKey = stringPreferencesKey("appLanguage")
val ContentLanguageKey = stringPreferencesKey("contentLanguage")
val ContentCountryKey = stringPreferencesKey("contentCountry")
val EnableKugouKey = booleanPreferencesKey("enableKugou")
val EnableLrcLibKey = booleanPreferencesKey("enableLrclib")
<<<<<<< HEAD

/**
 * Lyrics from the logged-in Spotify account. Off by default: it only does
 * anything once the user is signed in to Spotify, and it stores a local copy
 * of whatever it fetches.
 */
val EnableSpotifyLyricsKey = booleanPreferencesKey("enableSpotifyLyrics")
=======
>>>>>>> origin/main
val EnableBetterLyricsKey = booleanPreferencesKey("enableBetterLyrics")
val EnablePaxsenixKey = booleanPreferencesKey("enablePaxsenix")
val EnableLyricsPlus = booleanPreferencesKey("enableLyricsPlus")
val EnableMusixmatchKey = booleanPreferencesKey("enableMusixmatch")
val EnableYouLyPlusKey = booleanPreferencesKey("enableYouLyPlus") 
val EnableUnisonKey = booleanPreferencesKey("enableUnison")
val EnableBiniLyricsKey = booleanPreferencesKey("enableBiniLyrics") 
val EnableSimpMusicKey = booleanPreferencesKey("enableSimpMusic") 
val EnableNetEaseKey = booleanPreferencesKey("enableNetEase") 
val EnableMegalobizKey = booleanPreferencesKey("enableMegalobiz") 
val HideExplicitKey = booleanPreferencesKey("hideExplicit")
val HideVideoSongsKey = booleanPreferencesKey("hideVideoSongs")
val HideYoutubeShortsKey = booleanPreferencesKey("hideYoutubeShorts")
val ShowArtistDescriptionKey = booleanPreferencesKey("showArtistDescription")
val ShowArtistSubscriberCountKey = booleanPreferencesKey("showArtistSubscriberCount")
val ShowMonthlyListenersKey = booleanPreferencesKey("showMonthlyListeners")
val ProxyEnabledKey = booleanPreferencesKey("proxyEnabled")
val ProxyUrlKey = stringPreferencesKey("proxyUrl")
val ProxyTypeKey = stringPreferencesKey("proxyType")
val ProxyUsernameKey = stringPreferencesKey("proxyUsername")
val ProxyPasswordKey = stringPreferencesKey("proxyPassword")
val YtmSyncKey = booleanPreferencesKey("ytmSync")
val SelectedYtmPlaylistsKey = stringPreferencesKey("selectedYtmPlaylists")
val CheckForUpdatesKey = booleanPreferencesKey("checkForUpdates")
val UpdateNotificationsEnabledKey = booleanPreferencesKey("updateNotifications")
val LastUpdateCheckTimeKey = longPreferencesKey("lastUpdateCheckTime")

val AudioQualityKey = stringPreferencesKey("audioQuality")

enum class AudioQuality {
    AUTO,
    LOW,
    HIGH,
}

val AudioOffload = booleanPreferencesKey("enableOffload")
val AudioTrackPlaybackParamsKey = booleanPreferencesKey("audioTrackPlaybackParams")

val VarispeedKey = booleanPreferencesKey("varispeed")

val PersistentQueueKey = booleanPreferencesKey("persistentQueue")
val PersistentShuffleAcrossQueuesKey = booleanPreferencesKey("persistentShuffleAcrossQueues")
val RememberShuffleAndRepeatKey = booleanPreferencesKey("rememberShuffleAndRepeat")
val ShuffleModeKey = booleanPreferencesKey("shuffleMode")
val SkipSilenceKey = booleanPreferencesKey("skipSilence")
val SkipSilenceInstantKey = booleanPreferencesKey("skipSilenceInstant")
val AudioNormalizationKey = booleanPreferencesKey("audioNormalization")

<<<<<<< HEAD
/** Mid/side stereo widening ("Spatial Audio") applied in the audio sink. */
val SpatialAudioKey = booleanPreferencesKey("spatialAudio")

=======
>>>>>>> origin/main
val LoudnessLevelKey = stringPreferencesKey("loudnessLevel")

enum class LoudnessLevel(
    val targetLufs: Float
) {
    AGGRESSIVE(-7f),
    LOUD(-11f),
    BALANCED(-14f),
    QUIET(-19f),
}

// Sound FX (system audio effect equalizer: bands, output gain, bass boost, virtualizer)
val SoundFxEnabledKey = booleanPreferencesKey("soundFxEnabled")
val SoundFxControlModeKey = stringPreferencesKey("soundFxControlMode")
val SoundFxBandLevelsMbKey = stringPreferencesKey("soundFxBandLevelsMb")
val SoundFxSelectedProfileIdKey = stringPreferencesKey("soundFxSelectedProfileId")
val SoundFxOutputGainEnabledKey = booleanPreferencesKey("soundFxOutputGainEnabled")
val SoundFxOutputGainMbKey = intPreferencesKey("soundFxOutputGainMb")
val SoundFxBassBoostEnabledKey = booleanPreferencesKey("soundFxBassBoostEnabled")
val SoundFxBassBoostStrengthKey = intPreferencesKey("soundFxBassBoostStrength")
val SoundFxVirtualizerEnabledKey = booleanPreferencesKey("soundFxVirtualizerEnabled")
val SoundFxVirtualizerStrengthKey = intPreferencesKey("soundFxVirtualizerStrength")
val SoundFxAutoHeadroomKey = booleanPreferencesKey("soundFxAutoHeadroom")
val SoundFxProfilesJsonKey = stringPreferencesKey("soundFxProfilesJson")

<<<<<<< HEAD
/**
 * The one-tap loudness boost, stored as the name of a
 * [com.jay.glossy.eq.soundfx.AudioBoostLevel]. It is not a separate audio
 * effect: the level is folded into the sound fx settings as they are applied to
 * the live session, so it drives the very same LoudnessEnhancer the equalizer
 * screen writes to, and it rides along with the existing "settings changed"
 * observer in MusicService.
 */
val AudioBoostLevelKey = stringPreferencesKey("audioBoostLevel")

=======
>>>>>>> origin/main
val AutoLoadMoreKey = booleanPreferencesKey("autoLoadMore")
val AutoRadioQueueKey = booleanPreferencesKey("autoRadioQueue")
val DisableLoadMoreWhenRepeatAllKey = booleanPreferencesKey("disableLoadMoreWhenRepeatAll")
val AutoDownloadOnLikeKey = booleanPreferencesKey("autoDownloadOnLike")
val SimilarContent = booleanPreferencesKey("similarContent")
val AutoSkipNextOnErrorKey = booleanPreferencesKey("autoSkipNextOnError")
val AutoplayKey = booleanPreferencesKey("autoplay")
val StopMusicOnTaskClearKey = booleanPreferencesKey("stopMusicOnTaskClear")
val ShufflePlaylistFirstKey = booleanPreferencesKey("shufflePlaylistFirst")
val PreventDuplicateTracksInQueueKey = booleanPreferencesKey("preventDuplicateTracksInQueue")
val CrossfadeEnabledKey = booleanPreferencesKey("crossfadeEnabled")
val CrossfadeDurationKey = floatPreferencesKey("crossfadeDurationFloat")
val CrossfadeGaplessKey = booleanPreferencesKey("crossfadeGapless")

val MaxImageCacheSizeKey = intPreferencesKey("maxImageCacheSize")
val MaxSongCacheSizeKey = intPreferencesKey("maxSongCacheSize")
val MaxCanvasCacheSizeKey = intPreferencesKey("maxCanvasCacheSize")

val EnableSongCacheKey = booleanPreferencesKey("enableSongCache")
val SmartTrimmerKey = booleanPreferencesKey("smart_trimmer")

val PauseListenHistoryKey = booleanPreferencesKey("pauseListenHistory")
val PauseSearchHistoryKey = booleanPreferencesKey("pauseSearchHistory")
val DisableScreenshotKey = booleanPreferencesKey("disableScreenshot")

val StreamSourceWebRemixKey = booleanPreferencesKey("streamSourceWebRemix")
val StreamSourceTVHTML5Key = booleanPreferencesKey("streamSourceTVHTML5")
val StreamSourceAndroidVRKey = booleanPreferencesKey("streamSourceAndroidVR")
val StreamSourceVisionOSKey = booleanPreferencesKey("streamSourceVisionOS")
val StreamSourceIOSKey = booleanPreferencesKey("streamSourceIOS")
val StreamSourceWebCreatorKey = booleanPreferencesKey("streamSourceWebCreator")
val StreamSourceAndroidCreatorKey = booleanPreferencesKey("streamSourceAndroidCreator")

val EnableDynamicIconKey = booleanPreferencesKey("enableDynamicIcon")

val EnableDiscordRPCKey = booleanPreferencesKey("discordRPCEnable")
val DiscordInfoDismissedKey = booleanPreferencesKey("discordInfoDismissed")
val DiscordUsernameKey = stringPreferencesKey("discordUsername")
val DiscordNameKey = stringPreferencesKey("discordName")
val DiscordAvatarKey = stringPreferencesKey("discordAvatar")

val DiscordAdvancedModeKey = booleanPreferencesKey("discordAdvancedMode")
val DiscordActivityTypeKey = stringPreferencesKey("discordActivityType")
val DiscordActivityNameKey = stringPreferencesKey("discordActivityName")
val DiscordStateTemplateKey = stringPreferencesKey("discordStateTemplate")
val DiscordDetailsTemplateKey = stringPreferencesKey("discordDetailsTemplate")
val DiscordButton1EnabledKey = booleanPreferencesKey("discordButton1Enabled")
val DiscordButton1LabelKey = stringPreferencesKey("discordButton1Label")
val DiscordButton1UrlKey = stringPreferencesKey("discordButton1Url")
val DiscordButton2EnabledKey = booleanPreferencesKey("discordButton2Enabled")
val DiscordButton2LabelKey = stringPreferencesKey("discordButton2Label")
val DiscordButton2UrlKey = stringPreferencesKey("discordButton2Url")
val DiscordUserStatusKey = stringPreferencesKey("discordUserStatus")

val EnableGoogleCastKey = booleanPreferencesKey("enableGoogleCast")

val ListenTogetherServerUrlKey = stringPreferencesKey("listenTogetherServerUrl")
val ListenTogetherUsernameKey = stringPreferencesKey("listenTogetherUsername")
val EnableListenTogetherKey = booleanPreferencesKey("enableListenTogether")
val ListenTogetherAutoApprovalKey = booleanPreferencesKey("listenTogetherAutoApproval")
val ListenTogetherAutoApproveSuggestionsKey = booleanPreferencesKey("listenTogetherAutoApproveSuggestions")
val ListenTogetherSyncVolumeKey = booleanPreferencesKey("listenTogetherSyncVolume")
val ListenTogetherBlockedUsersKey = stringPreferencesKey("listenTogetherBlockedUsers")
<<<<<<< HEAD
val ListenTogetherPendingActionsKey = stringPreferencesKey("listenTogetherPendingActions")
=======
>>>>>>> origin/main
val ListenTogetherInTopBarKey = booleanPreferencesKey("listenTogetherInTopBar")

val ListenTogetherSessionTokenKey = stringPreferencesKey("listenTogetherSessionToken")
val ListenTogetherRoomCodeKey = stringPreferencesKey("listenTogetherRoomCode")
<<<<<<< HEAD

/**
 * After a Listen Together reconnect, ask the host for the current state again
 * (instead of trusting the state snapshot that arrived with the event).
 */
val ListenTogetherSmartResyncKey = booleanPreferencesKey("listenTogetherSmartResync")
=======
>>>>>>> origin/main
val ListenTogetherUserIdKey = stringPreferencesKey("listenTogetherUserId")
val ListenTogetherIsHostKey = booleanPreferencesKey("listenTogetherIsHost")
val ListenTogetherSessionTimestampKey = longPreferencesKey("listenTogetherSessionTimestamp")

val LastFMSessionKey = stringPreferencesKey("lastfmSession")
val LastFMUsernameKey = stringPreferencesKey("lastfmUsername")
val EnableLastFMScrobblingKey = booleanPreferencesKey("lastfmScrobblingEnable")
val LastFMUseNowPlaying = booleanPreferencesKey("lastfmUseNowPlaying")

val LastFMUseSendLikes = booleanPreferencesKey("lastfmUseSendLikes")

val ScrobbleDelayPercentKey = floatPreferencesKey("scrobbleDelayPercent")
val ScrobbleMinSongDurationKey = intPreferencesKey("scrobbleMinSongDuration")
val ScrobbleDelaySecondsKey = intPreferencesKey("scrobbleDelaySeconds")

val ChipSortTypeKey = stringPreferencesKey("chipSortType")

val SongSortTypeKey = stringPreferencesKey("songSortType")
val SongSortDescendingKey = booleanPreferencesKey("songSortDescending")
val PlaylistSongSortTypeKey = stringPreferencesKey("playlistSongSortType")
val PlaylistSongSortDescendingKey = booleanPreferencesKey("playlistSongSortDescending")
val AutoPlaylistSongSortTypeKey = stringPreferencesKey("autoPlaylistSongSortType")
val AutoPlaylistSongSortDescendingKey = booleanPreferencesKey("autoPlaylistSongSortDescending")
val ArtistSortTypeKey = stringPreferencesKey("artistSortType")
val ArtistSortDescendingKey = booleanPreferencesKey("artistSortDescending")
val AlbumSortTypeKey = stringPreferencesKey("albumSortType")
val AlbumSortDescendingKey = booleanPreferencesKey("albumSortDescending")
val PlaylistSortTypeKey = stringPreferencesKey("playlistSortType")
val PlaylistSortDescendingKey = booleanPreferencesKey("playlistSortDescending")
val AddToPlaylistSortTypeKey = stringPreferencesKey("addToPlaylistSortType")
val AddToPlaylistSortDescendingKey = booleanPreferencesKey("addToPlaylistSortDescending")
val ArtistSongSortTypeKey = stringPreferencesKey("artistSongSortType")
val ArtistSongSortDescendingKey = booleanPreferencesKey("artistSongSortDescending")
val MixSortTypeKey = stringPreferencesKey("mixSortType")
val MixSortDescendingKey = booleanPreferencesKey("albumSortDescending")

val SongFilterKey = stringPreferencesKey("songFilter")
val ArtistFilterKey = stringPreferencesKey("artistFilter")
val AlbumFilterKey = stringPreferencesKey("albumFilter")
val PodcastFilterKey = stringPreferencesKey("podcastFilter")

val LastLikeSongSyncKey = longPreferencesKey("last_like_song_sync")
val LastLibSongSyncKey = longPreferencesKey("last_library_song_sync")
val LastAlbumSyncKey = longPreferencesKey("last_album_sync")
val LastArtistSyncKey = longPreferencesKey("last_artist_sync")
val LastPlaylistSyncKey = longPreferencesKey("last_playlist_sync")
val LastFullSyncKey = longPreferencesKey("last_full_sync")
val LastWeeklyMostPlaylistSyncKey = longPreferencesKey("last_weekly_most_playlist_sync")
val LastMonthlyMostPlaylistSyncKey = longPreferencesKey("last_monthly_most_playlist_sync")
val ShowMostStatsPlaylistsKey = booleanPreferencesKey("show_most_stats_playlists")

const val SYNC_COOLDOWN = 30 * 60L

val ArtistViewTypeKey = stringPreferencesKey("artistViewType")
val AlbumViewTypeKey = stringPreferencesKey("albumViewType")
val PlaylistViewTypeKey = stringPreferencesKey("playlistViewType")

val PlaylistEditLockKey = booleanPreferencesKey("playlistEditLock")
val QuickPicksKey = stringPreferencesKey("discover")
val PreferredLyricsProviderKey = stringPreferencesKey("lyricsProvider")
val LyricsProviderOrderKey = stringPreferencesKey("lyricsProviderOrder")
val SimpMusicMigrationDoneKey = booleanPreferencesKey("simpMusicMigrationDone")
val QueueEditLockKey = booleanPreferencesKey("queueEditLock")
val ShowWrappedCardKey = booleanPreferencesKey("show_wrapped_card")
val WrappedSeenKey = booleanPreferencesKey("wrapped_seen")
val LastSeenVersionKey = stringPreferencesKey("lastSeenVersion")
val RandomizeHomeOrderKey = booleanPreferencesKey("randomizeHomeOrder")

val ShowLikedPlaylistKey = booleanPreferencesKey("show_liked_playlist")
val ShowDownloadedPlaylistKey = booleanPreferencesKey("show_downloaded_playlist")
val ShowTopPlaylistKey = booleanPreferencesKey("show_top_playlist")
val ShowCachedPlaylistKey = booleanPreferencesKey("show_cached_playlist")
val ShowUploadedPlaylistKey = booleanPreferencesKey("show_uploaded_playlist")

enum class LibraryViewType {
    LIST,
    GRID,
    ;

    fun toggle() =
        when (this) {
            LIST -> GRID
            GRID -> LIST
        }
}

enum class SongFilter {
    LIBRARY,
    LIKED,
    DOWNLOADED,
    UPLOADED,
}

enum class ArtistFilter {
    LIBRARY,
    LIKED,
}

enum class AlbumFilter {
    LIBRARY,
    LIKED,
    UPLOADED,
}

enum class PodcastFilter {
    EPISODES,
    CHANNELS,
    DOWNLOADED,
}

enum class SongSortType {
    CREATE_DATE,
    NAME,
    ARTIST,
    PLAY_TIME,
}

enum class PlaylistSongSortType {
    CUSTOM,
    CREATE_DATE,
    NAME,
    ARTIST,
    PLAY_TIME,
}

enum class AutoPlaylistSongSortType {
    CREATE_DATE,
    NAME,
    ARTIST,
    PLAY_TIME,
}

enum class ArtistSortType {
    CREATE_DATE,
    NAME,
    SONG_COUNT,
    PLAY_TIME,
}

enum class ArtistSongSortType {
    CREATE_DATE,
    NAME,
    PLAY_TIME,
}

enum class AlbumSortType {
    CREATE_DATE,
    NAME,
    ARTIST,
    YEAR,
    SONG_COUNT,
    LENGTH,
    PLAY_TIME,
}

enum class PlaylistSortType {
    CREATE_DATE,
    NAME,
    SONG_COUNT,
    LAST_UPDATED,
}

enum class MixSortType {
    CREATE_DATE,
    NAME,
    LAST_UPDATED,
}

enum class GridItemSize {
    BIG,
    SMALL,
}

enum class MyTopFilter {
    ALL_TIME,
    DAY,
    WEEK,
    MONTH,
    YEAR,
    ;

    fun toLocalDateTime(): LocalDateTime =
        when (this) {
            DAY -> {
                LocalDateTime
                    .now()
                    .minusDays(1)
            }

            WEEK -> {
                LocalDateTime
                    .now()
                    .minusWeeks(1)
            }

            MONTH -> {
                LocalDateTime
                    .now()
                    .minusMonths(1)
            }

            YEAR -> {
                LocalDateTime
                    .now()
                    .minusMonths(12)
            }

            ALL_TIME -> {
                LocalDateTime.of(1970, 1, 1, 0, 0)
            }
        }
}

enum class QuickPicks {
    QUICK_PICKS,
    LAST_LISTEN,
}

enum class PreferredLyricsProvider {
    LRCLIB,
    KUGOU,
    BETTER_LYRICS,
    PAXSENIX,
    LYRICSPLUS,
    MUSIXMATCH,
    YOULYPLUS,
    UNISON,
    BINILYRICS,
    SIMPMUSIC,
    NETEASE,
    MEGALOBIZ
}

enum class PlayerButtonsStyle {
    DEFAULT,
    PRIMARY,
    TERTIARY,
}

enum class PlayerBackgroundStyle {
    DEFAULT,
    GRADIENT,
    BLUR,
    ANIMATED_MESH,
}

val TopSize = stringPreferencesKey("topSize")
val HistoryDuration = floatPreferencesKey("historyDuration")

val PlayerButtonsStyleKey = stringPreferencesKey("player_buttons_style")
val PlayerBackgroundStyleKey = stringPreferencesKey("playerBackgroundStyle")
val ShowLyricsKey = booleanPreferencesKey("showLyrics")
val LyricsTextPositionKey = stringPreferencesKey("lyricsTextPosition")
val LyricsClickKey = booleanPreferencesKey("lyricsClick")
val LyricsScrollKey = booleanPreferencesKey("lyricsScrollKey")
val HideStatusBarOnFullscreenKey = booleanPreferencesKey("hideStatusBarOnFullscreen")
val LyricsRomanizeAsMainKey = booleanPreferencesKey("lyricsRomanizeAsMain")
val LyricsRomanizeCyrillicByLineKey = booleanPreferencesKey("lyricsRomanizeCyrillicByLine")
val OpenRouterApiKey = stringPreferencesKey("openRouterApiKey")
val AiProviderKey = stringPreferencesKey("aiProvider")
val OpenRouterBaseUrlKey = stringPreferencesKey("openRouterBaseUrl")
val OpenRouterModelKey = stringPreferencesKey("openRouterModel")

const val OpenRouterDefaultBaseUrl = "https://openrouter.ai/api/v1/chat/completions"
const val OpenRouterDefaultModel = "google/gemini-2.5-flash-lite"

val TranslateModeKey = stringPreferencesKey("translateMode")
val TranslateLanguageKey = stringPreferencesKey("translateLanguage")
val DeeplApiKey = stringPreferencesKey("deeplApiKey")
val DeeplFormalityKey = stringPreferencesKey("deeplFormality")
val AiSystemPromptKey = stringPreferencesKey("aiSystemPrompt")

const val DEFAULT_AI_SYSTEM_PROMPT = """You are a precise lyrics translation assistant. Your output must ALWAYS be a valid JSON array of strings.

CRITICAL RULES:
1. Output ONLY a JSON array: ["line1", "line2", "line3"]
2. NO explanations, NO questions, NO additional text
3. Each input line maps to exactly one output line
4. Preserve empty lines as empty strings ""
5. Return EXACTLY {lineCount} items in the array
6. If uncertain, provide best approximation but maintain line count"""
val LyricsGlowEffectKey = booleanPreferencesKey("lyricsGlowEffect")

val LyricsRomanizeList = stringPreferencesKey("lyricsRomanizeList")
val LyricsAnimationStyleKey = stringPreferencesKey("lyricsAnimationStyle")

enum class LyricsAnimationStyle {
    NONE,
    FADE,
    GLOW,
    SLIDE,
    KARAOKE,
    APPLE,
}

val LyricsTextSizeKey = floatPreferencesKey("lyricsTextSize")
val LyricsLineSpacingKey = floatPreferencesKey("lyricsLineSpacing")
val RespectAgentPositioningKey = booleanPreferencesKey("respectAgentPositioning")
val ShowIntervalIndicatorKey = booleanPreferencesKey("showIntervalIndicator")
val ExperimentalLyricsKey = booleanPreferencesKey("experimentalLyrics")

val PlayerVolumeKey = floatPreferencesKey("playerVolume")
val SleepTimerDefaultKey = floatPreferencesKey("sleepTimerDefault")
val SleepTimerStopAfterCurrentSongKey = booleanPreferencesKey("sleepTimerStopAfterCurrentSong")
val SleepTimerFadeOutKey = booleanPreferencesKey("sleepTimerFadeOut")
val RepeatModeKey = intPreferencesKey("repeatMode")

val SearchSourceKey = stringPreferencesKey("searchSource")
val SwipeThumbnailKey = booleanPreferencesKey("swipeThumbnail")
val SwipeSensitivityKey = floatPreferencesKey("swipeSensitivity")
val SleepTimerEnabledKey = booleanPreferencesKey("sleepTimerEnabled")
val SleepTimerRepeatKey = stringPreferencesKey("sleepTimerRepeat")
val SleepTimerStartTimeKey = stringPreferencesKey("sleepTimerStartTime")
val SleepTimerEndTimeKey = stringPreferencesKey("sleepTimerEndTime")
val SleepTimerCustomDaysKey = stringPreferencesKey("sleepTimerCustomDays")
val SleepTimerDayTimesKey = stringPreferencesKey("sleepTimerDayTimes")

enum class SearchSource {
    LOCAL,
    ONLINE,
    ;

    fun toggle() =
        when (this) {
            LOCAL -> ONLINE
            ONLINE -> LOCAL
        }
}

val VisitorDataKey = stringPreferencesKey("visitorData")
val DataSyncIdKey = stringPreferencesKey("dataSyncId")
val AndroidAutoYouTubePlaylistsKey = booleanPreferencesKey("androidAutoYoutubePlaylists")
val AndroidAutoSectionsOrderKey = stringPreferencesKey("androidAutoSectionsOrder")
val AndroidAutoTargetPlaylistKey = stringPreferencesKey("androidAutoTargetPlaylist")
val AndroidAutoSearchLocalLimitKey = intPreferencesKey("androidAutoSearchLocalLimit")
val InnerTubeCookieKey = stringPreferencesKey("innerTubeCookie")
val AccountNameKey = stringPreferencesKey("accountName")
val AccountEmailKey = stringPreferencesKey("accountEmail")
val AccountChannelHandleKey = stringPreferencesKey("accountChannelHandle")
val UseLoginForBrowse = booleanPreferencesKey("useLoginForBrowse")

val LanguageCodeToName =
    mapOf(
        "af" to "Afrikaans",
        "az" to "Azərbaycan",
        "id" to "Bahasa Indonesia",
        "ms" to "Bahasa Malaysia",
        "ca" to "Català",
        "cs" to "Čeština",
        "da" to "Dansk",
        "de" to "Deutsch",
        "et" to "Eesti",
        "en-GB" to "English (UK)",
        "en" to "English (US)",
        "es" to "Español (España)",
        "es-419" to "Español (Latinoamérica)",
        "eu" to "Euskara",
        "fil" to "Filipino",
        "fr" to "Français",
        "fr-CA" to "Français (Canada)",
        "gl" to "Galego",
        "hr" to "Hrvatski",
        "zu" to "IsiZulu",
        "is" to "Íslenska",
        "it" to "Italiano",
        "sw" to "Kiswahili",
        "lt" to "Lietuvių",
        "hu" to "Magyar",
        "nl" to "Nederlands",
        "no" to "Norsk",
        "or" to "Odia",
        "uz" to "O‘zbe",
        "pl" to "Polski",
        "pt-PT" to "Português",
        "pt" to "Português (Brasil)",
        "ro" to "Română",
        "sq" to "Shqip",
        "sk" to "Slovenčina",
        "sl" to "Slovenščina",
        "fi" to "Suomi",
        "sv" to "Svenska",
        "bo" to "Tibetan བོད་སྐད།",
        "vi" to "Tiếng Việt",
        "tr" to "Türkçe",
        "bg" to "Български",
        "ky" to "Кыргызча",
        "kk" to "Қазақ Тілі",
        "mk" to "Македонски",
        "mn" to "Монгол",
        "ru" to "Русский",
        "sr" to "Српски",
        "uk" to "Українська",
        "el" to "Ελληνικά",
        "hy" to "Հայերեն",
        "iw" to "עברית",
        "ur" to "اردو",
        "ar" to "العربية",
        "fa" to "فارسی",
        "ne" to "नेपाली",
        "mr" to "मराठी",
        "hi" to "हिन्दी",
        "bn" to "বাংলা",
        "pa" to "ਪੰਜਾਬੀ",
        "gu" to "ગુજરાતી",
        "ta" to "தமிழ்",
        "te" to "తెలుగు",
        "kn" to "ಕನ್ನಡ",
        "ml" to "മലയാളം",
        "si" to "සිංහල",
        "th" to "ภาษาไทย",
        "lo" to "ລາວ",
        "my" to "ဗမာ",
        "ka" to "ქართული",
        "am" to "አማርኛ",
        "km" to "ខ្មែរ",
        "zh-CN" to "中文 (简体)",
        "zh-TW" to "中文 (繁體)",
        "zh-HK" to "中文 (香港)",
        "ja" to "日本語",
        "ko" to "한국어",
    )

val CountryCodeToName =
    mapOf(
        "DZ" to "Algeria",
        "AR" to "Argentina",
        "AU" to "Australia",
        "AT" to "Austria",
        "AZ" to "Azerbaijan",
        "BH" to "Bahrain",
        "BD" to "Bangladesh",
        "BY" to "Belarus",
        "BE" to "Belgium",
        "BO" to "Bolivia",
        "BA" to "Bosnia and Herzegovina",
        "BR" to "Brazil",
        "BG" to "Bulgaria",
        "KH" to "Cambodia",
        "CA" to "Canada",
        "CL" to "Chile",
        "HK" to "Hong Kong",
        "CO" to "Colombia",
        "CR" to "Costa Rica",
        "HR" to "Croatia",
        "CY" to "Cyprus",
        "CZ" to "Czech Republic",
        "DK" to "Denmark",
        "DO" to "Dominican Republic",
        "EC" to "Ecuador",
        "EG" to "Egypt",
        "SV" to "El Salvador",
        "EE" to "Estonia",
        "FI" to "Finland",
        "FR" to "France",
        "GE" to "Georgia",
        "DE" to "Germany",
        "GH" to "Ghana",
        "GR" to "Greece",
        "GT" to "Guatemala",
        "HN" to "Honduras",
        "HU" to "Hungary",
        "IS" to "Iceland",
        "IN" to "India",
        "ID" to "Indonesia",
        "IQ" to "Iraq",
        "IE" to "Ireland",
        "IL" to "Israel",
        "IT" to "Italy",
        "JM" to "Jamaica",
        "JP" to "Japan",
        "JO" to "Jordan",
        "KZ" to "Kazakhstan",
        "KE" to "Kenya",
        "KR" to "South Korea",
        "KW" to "Kuwait",
        "LA" to "Lao",
        "LV" to "Latvia",
        "LB" to "Lebanon",
        "LY" to "Libya",
        "LI" to "Liechtenstein",
        "LT" to "Lithuania",
        "LU" to "Luxembourg",
        "MK" to "Macedonia",
        "MY" to "Malaysia",
        "MT" to "Malta",
        "MX" to "Mexico",
        "ME" to "Montenegro",
        "MA" to "Morocco",
        "NP" to "Nepal",
        "NL" to "Netherlands",
        "NZ" to "New Zealand",
        "NI" to "Nicaragua",
        "NG" to "Nigeria",
        "NO" to "Norway",
        "OM" to "Oman",
        "PK" to "Pakistan",
        "PA" to "Panama",
        "PG" to "Papua New Guinea",
        "PY" to "Paraguay",
        "PE" to "Peru",
        "PH" to "Philippines",
        "PL" to "Poland",
        "PT" to "Portugal",
        "PR" to "Puerto Rico",
        "QA" to "Qatar",
        "RO" to "Romania",
        "RU" to "Russian Federation",
        "SA" to "Saudi Arabia",
        "SN" to "Senegal",
        "RS" to "Serbia",
        "SG" to "Singapore",
        "SK" to "Slovakia",
        "SI" to "Slovenia",
        "ZA" to "South Africa",
        "ES" to "Spain",
        "LK" to "Sri Lanka",
        "SE" to "Sweden",
        "CH" to "Switzerland",
        "TW" to "Taiwan",
        "TZ" to "Tanzania",
        "TH" to "Thailand",
        "TN" to "Tunisia",
        "TR" to "Turkey",
        "UG" to "Uganda",
        "UA" to "Ukraine",
        "AE" to "United Arab Emirates",
        "GB" to "United Kingdom",
        "US" to "United States",
        "UY" to "Uruguay",
        "VE" to "Venezuela (Bolivarian Republic)",
        "VN" to "Vietnam",
        "YE" to "Yemen",
        "ZW" to "Zimbabwe",
    )

val QuickPickShapeKey = stringPreferencesKey("quickPickShape")

enum class QuickPickShape {
    DEFAULT,
    CIRCLE,
    SQUIRCLE,
    LEAF,
    INVERTED_LEAF,
    TEARDROP,
    MESSAGE_BUBBLE,
    TICKET,
    INVERTED_TICKET,
    CUT_CORNER,
    OCTAGON,
    DIAMOND,
    BOOKMARK,
    FOLDER,
    DYNAMIC
}

val QuickPicksStyleKey = stringPreferencesKey("quickPicksStyle")

enum class QuickPicksStyle {
    GRID,
    LIST,
    CAROUSEL
}

val ShowFeaturedCarouselKey = booleanPreferencesKey("showFeaturedCarousel")

val SelectedFontKey = stringPreferencesKey("selected_font")

enum class AppFont(val value: String, val displayName: String, val description: String) {
    SYSTEM("system", "System Default", "Use the default system typeface."),
    GOOGLE_SANS("google_sans", "Google Sans", "Google's signature geometric sans-serif."),
    GOOGLE_SANS_FLEX("google_sans_flex", "Google Sans Flex", "Google's highly adaptable flex font."),
    INTER("inter", "Inter", "A typeface carefully crafted & designed for computer screens."),
    MANROPE("manrope", "Manrope", "An open-source modern sans-serif font family."),
    OUTFIT("outfit", "Outfit", "A clean and modern geometric sans-serif."),
    PLUS_JAKARTA_SANS("plus_jakarta_sans", "Plus Jakarta Sans", "A premium warm geometric sans-serif typeface."),
    POPPINS("poppins", "Poppins", "A geometric sans-serif typeface with international appeal."),
    ROUNDEX("roundex", "Roundex", "A smooth, rounded modern typeface.");

    companion object {
        fun fromValue(value: String): AppFont = entries.find { it.value == value } ?: SYSTEM
    }
}
