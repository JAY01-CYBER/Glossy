from pathlib import Path

p = Path("app/src/main/kotlin/com/jay/glossy/ui/player/MiniPlayer.kt")
s = p.read_text(encoding="utf-8").replace("\r\n", "\n")

# Legacy NOTES overlay already exists; add the BARS marker beside the title.
old_legacy_col = """        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
        ) {
            Text(
                text = mediaMetadata.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(),
            )"""
new_legacy_col = """        val legacyPlayingAnimation by rememberEnumPreference(
            MiniPlayerPlayingAnimationKey,
            defaultValue = MiniPlayerPlayingAnimation.BARS,
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (legacyPlayingAnimation == MiniPlayerPlayingAnimation.BARS) {
                    NowPlayingAnimationIndicator(
                        animation = legacyPlayingAnimation,
                        isPlaying = legacyIsPlaying,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
                Text(
                    text = mediaMetadata.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).basicMarquee(),
                )
            }"""
assert s.count(old_legacy_col) == 1, "legacy col=%d" % s.count(old_legacy_col)
s = s.replace(old_legacy_col, new_legacy_col)
p.write_text(s, encoding="utf-8")
print("fix4 legacy bars ok")
