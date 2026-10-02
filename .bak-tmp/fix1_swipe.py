from pathlib import Path

p = Path("app/src/main/kotlin/com/jay/glossy/ui/player/MiniPlayer.kt")
s = p.read_text(encoding="utf-8").replace("\r\n", "\n")

# ---------------------------------------------------------------- Studio swipe
# Studio never applied its drag offset to anything, so swipes did nothing.
# Give it the Modern bar's swipe engine: replace its static card Box with
# SwipeableMiniPlayerBox and move the drag offset onto the card Box, exactly
# like Modern does (Modern offsets the inner card; see NewMiniPlayer).
old_studio_box = """    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable(onClick = onClick),
    ) {"""
new_studio_box = """    // Swipe engine shared with the Modern bar: drag offset + skip cues +
    // velocity/distance song change. Studio previously never applied its
    // offset anywhere, so swipes did nothing.
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeThumbnailPref by rememberPreference(SwipeThumbnailKey, true)
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val swipeThumbnail = swipeThumbnailPref && !isListenTogetherGuest

    SwipeableMiniPlayerBox(
        modifier = modifier.padding(horizontal = 8.dp),
        swipeSensitivity = swipeSensitivity,
        swipeThumbnail = swipeThumbnail,
        playerConnection = playerConnection,
        layoutDirection = layoutDirection,
        coroutineScope = coroutineScope,
        pureBlack = pureBlack,
        useLegacyBackground = false,
    ) { offsetX ->
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable(onClick = onClick),
    ) {"""
assert s.count(old_studio_box) == 1, "studio box=%d" % s.count(old_studio_box)
s = s.replace(old_studio_box, new_studio_box)

# Close the extra SwipeableMiniPlayerBox lambda opened above.
old_studio_close = """                GlossyIconAction(
                    icon = R.drawable.skip_next,
                    contentDescription = null,
                    tint = primaryTextColor,
                    buttonSize = 36.dp,
                    iconSize = 20.dp,
                    onClick = { playerConnection.seekToNext() },
                )
            }
        }
    }
}
"""
new_studio_close = """                GlossyIconAction(
                    icon = R.drawable.skip_next,
                    contentDescription = null,
                    tint = primaryTextColor,
                    buttonSize = 36.dp,
                    iconSize = 20.dp,
                    onClick = { playerConnection.seekToNext() },
                )
            }
        }
    }
    }
}
"""
assert s.count(old_studio_close) == 1, "studio close=%d" % s.count(old_studio_close)
s = s.replace(old_studio_close, new_studio_close)

p.write_text(s, encoding="utf-8")
print("fix1 swipe ok")
