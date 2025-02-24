package me.neznamy.tab.shared;

import java.util.*;

import me.neznamy.tab.shared.TabConstants.Placeholder;
import me.neznamy.tab.shared.features.layout.LayoutManagerImpl;
import me.neznamy.tab.shared.features.sorting.types.SortingType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Class for detecting misconfiguration in config files and fix mistakes
 * to avoid headaches when making a configuration mistake.
 */
public class MisconfigurationHelper {

    /** Amount of logged warns on startup */
    private int warnCount;

    // ------------------------
    // Startup Errors
    // ------------------------

    /**
     * Checks if configured refresh intervals are non-negative, non-zero and
     * divisible by {@link Placeholder#MINIMUM_REFRESH_INTERVAL}. If not,
     * value is fixed in the map and console warn is sent.
     *
     * @param   refreshIntervals
     *          Configured refresh intervals
     */
    public void fixRefreshIntervals(@NotNull Map<String, Integer> refreshIntervals) {
        LinkedHashMap<String, Integer> valuesToFix = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : refreshIntervals.entrySet()) {
            if (entry.getValue() == null) {
                startupWarn("Refresh interval of " + entry.getKey() +
                        " is set to null. Define a valid value or remove it if you don't want to override default value.");
                valuesToFix.put(entry.getKey(), Placeholder.MINIMUM_REFRESH_INTERVAL);
                continue;
            }
            if (!(entry.getValue() instanceof Integer)) {
                startupWarn("Refresh interval configured for \"" + entry.getKey() +
                        "\" is not a valid number.");
                valuesToFix.put(entry.getKey(), 500);
                continue;
            }
            int interval = (int) entry.getValue();
            if (interval < 0) {
                startupWarn("Invalid refresh interval configured for " + entry.getKey() +
                        " (" + interval + "). Value cannot be negative.");
            } else if (interval == 0) {
                startupWarn("Invalid refresh interval configured for " + entry.getKey() +
                        " (0). Value cannot be zero.");
            } else if (interval % Placeholder.MINIMUM_REFRESH_INTERVAL != 0) {
                startupWarn("Invalid refresh interval configured for " + entry.getKey() +
                        " (" + interval + "). Value must be divisible by " + Placeholder.MINIMUM_REFRESH_INTERVAL + ".");
            } else continue;
            valuesToFix.put(entry.getKey(), Placeholder.MINIMUM_REFRESH_INTERVAL);
        }
        refreshIntervals.putAll(valuesToFix);
    }

    /**
     * Makes interval divisible by {@link Placeholder#MINIMUM_REFRESH_INTERVAL}
     * and sends error message if it was not already or was 0 or less
     *
     * @param   name
     *          name of animation used in error message
     * @param   interval
     *          configured change interval
     * @return  fixed change interval
     */
    public int fixAnimationInterval(@NotNull String name, int interval) {
        if (interval == 0) {
            startupWarn(String.format("Animation \"&e%s&c\" has refresh interval of 0 milliseconds! Did you forget to configure it? &bUsing 1000.", name));
            return 1000;
        }
        if (interval < 0) {
            startupWarn(String.format("Animation \"&e%s&c\" has refresh interval of %s. Refresh cannot be negative! &bUsing 1000.", name, interval));
            return 1000;
        }
        if (interval % TabConstants.Placeholder.MINIMUM_REFRESH_INTERVAL != 0) {
            int newInterval = interval - interval % TabConstants.Placeholder.MINIMUM_REFRESH_INTERVAL;
            if (newInterval == 0) newInterval = TabConstants.Placeholder.MINIMUM_REFRESH_INTERVAL;
            startupWarn(String.format("Animation \"&e%s&c\" has refresh interval of %s, which is not divisible by " +
                    TabConstants.Placeholder.MINIMUM_REFRESH_INTERVAL + "! &bUsing %s.", name, interval, newInterval));
            return newInterval;
        }
        return interval;
    }

    /**
     *
     * Returns the list if not null, empty list and error message if null
     *
     * @param   name
     *          name of animation used in error message
     * @param   list
     *          list of configured animation frames
     * @return  the list if it's valid, singleton list with {@code "<Invalid Animation>"} otherwise
     */
    public List<String> fixAnimationFrames(@NotNull String name, @Nullable List<String> list) {
        if (list == null) {
            startupWarn("Animation \"&e" + name + "&c\" does not have any texts defined!");
            return Collections.singletonList("<Animation does not have any texts>");
        }
        return list;
    }

    /**
     * Checks if belowname text contains suspicious placeholders, which
     * will not work as users may expect, since the text must be
     * the same for all players.
     *
     * @param   text
     *          Configured belowname text
     */
    public void checkBelowNameText(@NotNull String text) {
        if (!text.contains("%")) return;
        if (text.contains("%animation") || text.contains("%condition")) return;
        startupWarn("Belowname text is set to " + text + ", however, the feature cannot display different text on different players " +
                "due to a minecraft limitation. Placeholders will be parsed for viewing player.");
    }

    /**
     * Sends a console warn that entered skin definition does not match
     * any of the supported patterns.
     *
     * @param   definition
     *          Configured skin definition
     */
    public void invalidLayoutSkinDefinition(@NotNull String definition) {
        startupWarn("Invalid skin definition: \"" + definition + "\". Supported patterns are:",
                "#1 - \"player:<name>\" for skin of player with specified name",
                "#2 - \"mineskin:<id>\" for UUID of chosen skin from mineskin.org",
                "#3 - \"texture:<texture>\" for raw texture string");
    }

    /**
     * Sends a console warn about a fixed line in layout being invalid.
     *
     * @param   layout
     *          Layout name where fixed slot is defined
     * @param   line
     *          Line definition from configuration
     */
    public void invalidFixedSlotDefinition(@NotNull String layout, @NotNull String line) {
        startupWarn("Layout " + layout + " has invalid fixed slot defined as \"" + line + "\". Supported values are " +
                "\"SLOT|TEXT\" and \"SLOT|TEXT|SKIN\", where SLOT is a number from 1 to 80, TEXT is displayed text and SKIN is skin used for the slot");
    }

    /**
     * Sends a console warn that specified layout direction is not a valid
     * enum value.
     *
     * @param   direction
     *          Configured direction
     */
    public void invalidLayoutDirection(@NotNull String direction) {
        startupWarn("\"&e" + direction + "&c\" is not a valid type of layout direction. Valid options are: &e" + Arrays.deepToString(LayoutManagerImpl.Direction.values()) + ". &bUsing COLUMNS");
    }

    public void invalidSortingTypeElement(@NotNull String element, @NotNull Set<String> validTypes) {
        startupWarn("\"&e" + element + "&c\" is not a valid sorting type element. Valid options are: &e" + validTypes + ".");
    }

    public void invalidSortingPlaceholder(@NotNull String placeholder, @NotNull SortingType type) {
        startupWarn("\"" + placeholder + "\" is not a valid placeholder for " + type.getClass().getSimpleName() + " sorting type");
    }

    public void conditionHasNoConditions(@NotNull String conditionName) {
        startupWarn("Condition \"" + conditionName + "\" is missing \"conditions\" section.");
    }

    public void invalidConditionPattern(@NotNull String conditionName, @NotNull String line) {
        startupWarn("Line \"" + line + "\" in condition " + conditionName + " is not a valid condition pattern.");
    }

    public void invisibleAndUnlimitedNameTagsAreMutuallyExclusive() {
        startupWarn("Unlimited name tag mode is enabled as well as invisible name tags. These 2 options are mutually exclusive.",
                "If you want name tags to be invisible, you don't need unlimited name tag mode at all.",
                "If you want enhanced name tags without limits, making them invisible would defeat the purpose.");
    }

    public void invalidDateFormat(@NotNull String format) {
        startupWarn("Format \"" + format + "\" is not a valid date/time format. Did you try to use color codes?");
    }

    public void bothGlobalPlayerListAndLayoutEnabled() {
        startupWarn("Both global playerlist and layout features are enabled, but layout makes global playerlist redundant.",
                "Layout automatically works with all connected players on the proxy and replaces real player entries with" +
                        " fake players, making global playerlist completely useless.",
                "Disable global playerlist for the same result, but with better performance.");
    }

    public void bothPerWorldPlayerListAndLayoutEnabled() {
        startupWarn("Both per world playerlist and layout features are enabled, but layout makes per world playerlist redundant.",
                "Layout automatically works with all connected players and replaces real player entries with" +
                        " fake players, making per world playerlist completely useless as real players are pushed out of the playerlist.",
                "Disable per world playerlist for the same result, but with better performance.");
    }

    public void checkLayoutMap(@NotNull String layoutName, @NotNull Map<String, Object> map) {
        List<String> expectedKeys = Arrays.asList("condition", "fixed-slots", "groups");
        for (String mapKey : map.keySet()) {
            if (!expectedKeys.contains(mapKey)) {
                startupWarn("Unknown property \"" + mapKey + "\" in layout \"" + layoutName + "\". Valid properties: " + expectedKeys);
            }
        }
    }

    public void checkLayoutGroupMap(@NotNull String layoutName, @NotNull String groupName, @NotNull Map<String, Object> map) {
        List<String> expectedKeys = Arrays.asList("condition", "slots");
        for (String mapKey : map.keySet()) {
            if (!expectedKeys.contains(mapKey)) {
                startupWarn("Unknown property \"" + mapKey + "\" in layout \"" + layoutName + "\"'s group \"" + groupName + "\". Valid properties: " + expectedKeys);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public @NotNull <T> T fromMapOrElse(@NotNull Map<String, Object> map, @NotNull String key, @NotNull T defaultValue, @NotNull String warnMessage) {
        if (map.containsKey(key)) {
            return (T) map.get(key);
        } else {
            startupWarn(warnMessage);
            return defaultValue;
        }
    }

    /**
     * Sends a startup warn message into console
     *
     * @param   messages
     *          messages to print into console
     */
    private void startupWarn(@NotNull String... messages) {
        warnCount++;
        for (String message : messages) {
            TAB.getInstance().sendConsoleMessage("&c[WARN] " + message, true);
        }
    }

    /**
     * Prints amount of warns logged into console previously, if more than 0.
     */
    public void printWarnCount() {
        if (warnCount == 0) return;
        TAB.getInstance().sendConsoleMessage("&eFound a total of " + warnCount + " issues.", true);
        // Reset after printing to prevent count going up on each reload
        warnCount = 0;
    }

    // ------------------------
    // Runtime Errors
    // ------------------------

    public void invalidNumberForBossBarProgress(@NotNull String bossBar, @NotNull String input, @NotNull String configuredValue) {
        if (configuredValue.contains("%")) {
            TAB.getInstance().sendConsoleMessage("&c[WARN] Placeholder \"" + configuredValue +
                    "\" used in BossBar progress of \"" + bossBar + "\" returned value, which cannot be evaluated to a number between 0 and 100 (\"" + input + "\")", true);

        } else {
            TAB.getInstance().sendConsoleMessage("&c[WARN] BossBar \"" + bossBar +
                    "\" has invalid input configured for progress (\"" + configuredValue + "\"). Expecting a number between 0 and 100 or a placeholder returning one.", true);
        }
    }
}
