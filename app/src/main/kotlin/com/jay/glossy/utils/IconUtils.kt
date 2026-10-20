package com.jay.glossy.utils

import com.jay.glossy.R

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Every launcher icon the app ships.
 *
 * Each entry is one `<activity-alias>` in the manifest, all pointing at
 * `MainActivity`; exactly one alias is enabled at a time and that alias is what
 * the launcher draws. [CLASSIC] is the icon the app installs with, so it stays
 * out of [picker] — it is the fallback, not a choice.
 */
enum class AppIcon(
    /** Fully-qualified component name of the alias that carries this icon. */
    val component: String,
    /** What the picker calls it. */
    val label: String,
    /** Mipmap the settings dialog draws in its row. */
    val previewRes: Int,
) {
    CLASSIC("com.jay.glossy.MainActivityAlias", "Glossy", R.mipmap.ic_launcher),
    EMBER("com.jay.glossy.MainActivityEmber", "Ember", R.mipmap.ic_launcher_ember),
    LAVENDER("com.jay.glossy.MainActivityLavender", "Lavender", R.mipmap.ic_launcher_lavender),
    AURORA("com.jay.glossy.MainActivityAurora", "Aurora", R.mipmap.ic_launcher_aurora),
    MINT("com.jay.glossy.MainActivityMint", "Mint", R.mipmap.ic_launcher_mint),
    NOIR("com.jay.glossy.MainActivityNoir", "Noir", R.mipmap.ic_launcher_noir),
    BLOOM("com.jay.glossy.MainActivityBloom", "Bloom", R.mipmap.ic_launcher_bloom),
    MIDNIGHT("com.jay.glossy.MainActivityMidnight", "Midnight", R.mipmap.ic_launcher_midnight),
    SAND("com.jay.glossy.MainActivitySand", "Sand", R.mipmap.ic_launcher_sand),
    ;

    companion object {
        /** The eight alternatives offered in Appearance settings. */
        val picker: List<AppIcon> = entries.filter { it != CLASSIC }

        fun fromName(name: String?): AppIcon =
            entries.firstOrNull { it.name == name } ?: CLASSIC
    }
}

object IconUtils {
    /**
     * Enables [icon]'s launcher alias and disables every other one.
     *
     * The four launcher-visible aliases that pre-date the picker
     * (`MainActivityAlias`, `MainActivityStatic`) are switched here too, so
     * choosing an icon never leaves two home-screen entries behind.
     */
    fun setIcon(context: Context, icon: AppIcon) {
        val pm = context.packageManager
        AppIcon.entries.forEach { candidate ->
            setEnabled(context, pm, candidate.component, candidate == icon)
        }
        setEnabled(context, pm, "com.jay.glossy.MainActivityStatic", false)
    }

    private fun setEnabled(
        context: Context,
        pm: PackageManager,
        component: String,
        enabled: Boolean,
    ) {
        pm.setComponentEnabledSetting(
            ComponentName(context, component),
            if (enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            },
            PackageManager.DONT_KILL_APP,
        )
    }
}
