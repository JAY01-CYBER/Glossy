from pathlib import Path

p = Path("app/src/main/kotlin/com/jay/glossy/ui/player/MiniPlayer.kt")
s = p.read_text(encoding="utf-8").replace("\r\n", "\n")

# ------------------------------------------------- Special BARS beside title
old_special_col = """                    ) { metadata ->
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = Brush.horizontalGradient(
                                            0f to Color.Transparent,
                                            0.05f to Color.Black,
                                            0.95f to Color.Black,
                                            1f to Color.Transparent
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                        ) {
                            Text(
                                text = metadata?.title ?: "Unknown",
                                color = textColor,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee()
                            )"""
new_special_col = """                    ) { metadata ->
                        val specialPlayingAnimation by rememberEnumPreference(
                            MiniPlayerPlayingAnimationKey,
                            defaultValue = MiniPlayerPlayingAnimation.BARS,
                        )
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = Brush.horizontalGradient(
                                            0f to Color.Transparent,
                                            0.05f to Color.Black,
                                            0.95f to Color.Black,
                                            1f to Color.Transparent
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (specialPlayingAnimation == MiniPlayerPlayingAnimation.BARS) {
                                    NowPlayingAnimationIndicator(
                                        animation = specialPlayingAnimation,
                                        isPlaying = effectiveIsPlaying,
                                        color = textColor,
                                        modifier = Modifier.padding(end = 6.dp),
                                    )
                                }
                                Text(
                                    text = metadata?.title ?: "Unknown",
                                    color = textColor,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f).basicMarquee()
                                )
                            }"""
assert s.count(old_special_col) == 1, "special col=%d" % s.count(old_special_col)
s = s.replace(old_special_col, new_special_col)

# Rename its overlay read to avoid clashing with the new title read.
old_ov = "val specialNotesAnimation by rememberEnumPreference("
assert s.count(old_ov) == 1
s = s.replace(old_ov, "val specialArtworkNotes by rememberEnumPreference(")
s = s.replace("if (specialNotesAnimation == MiniPlayerPlayingAnimation.NOTES) {",
              "if (specialArtworkNotes == MiniPlayerPlayingAnimation.NOTES) {")

p.write_text(s, encoding="utf-8")
print("fix3 special bars ok")
