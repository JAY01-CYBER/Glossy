from pathlib import Path

p = Path("app/src/main/kotlin/com/jay/glossy/ui/player/MiniPlayer.kt")
s = p.read_text(encoding="utf-8").replace("\r\n", "\n")

# ---------------------------------------------------------------- Studio BARS
old_studio_title = """                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mediaMetadata?.title.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )"""
new_studio_title = """                Spacer(Modifier.width(10.dp))
                val studioPlayingAnimation by rememberEnumPreference(
                    MiniPlayerPlayingAnimationKey,
                    defaultValue = MiniPlayerPlayingAnimation.BARS,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (studioPlayingAnimation == MiniPlayerPlayingAnimation.BARS) {
                            NowPlayingAnimationIndicator(
                                animation = studioPlayingAnimation,
                                isPlaying = isPlaying,
                                color = primaryTextColor,
                                modifier = Modifier.padding(end = 6.dp),
                            )
                        }
                        Text(
                            text = mediaMetadata?.title.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }"""
assert s.count(old_studio_title) == 1, "studio title=%d" % s.count(old_studio_title)
s = s.replace(old_studio_title, new_studio_title)

# Rename the artwork overlay pref read so the two reads do not share a name.
old_overlay = "val studioNotesAnimation by rememberEnumPreference("
assert s.count(old_overlay) == 1
s = s.replace(old_overlay, "val studioArtworkNotes by rememberEnumPreference(")
s = s.replace("if (studioNotesAnimation == MiniPlayerPlayingAnimation.NOTES) {",
              "if (studioArtworkNotes == MiniPlayerPlayingAnimation.NOTES) {")

p.write_text(s, encoding="utf-8")
print("fix2 studio bars ok")
