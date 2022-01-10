package us.ajg0702.leaderboards;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import us.ajg0702.leaderboards.boards.StatEntry;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.leaderboards.boards.TopManager;
import us.ajg0702.leaderboards.cache.Cache;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Placeholders extends PlaceholderExpansion {

    LeaderboardPlugin plugin;
    public Placeholders(LeaderboardPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * This method should always return true unless we
     * have a dependency we need to make sure is on the server
     * for our placeholders to work!
     *
     * @return always true since we do not have any dependencies.
     */
    @Override
    public boolean canRegister(){
        return true;
    }

    /**
     * The name of the person who created this expansion should go here.
     *
     * @return The name of the author as a String.
     */
    @Override
    public String getAuthor(){
        return "ajgeiss0702";
    }

    /**
     * Because this is an internal class,
     * you must override this method to let PlaceholderAPI know to not unregister your expansion class when
     * PlaceholderAPI is reloaded
     *
     * @return true to persist through reloads
     */
    @Override
    public boolean persist(){
        return true;
    }

    /**
     * The placeholder identifier should go here.
     * <br>This is what tells PlaceholderAPI to call our onRequest
     * method to obtain a value if a placeholder starts with our
     * identifier.
     * <br>The identifier has to be lowercase and can't contain _ or %
     *
     * @return The identifier in {@code %<identifier>_<value>%} as String.
     */
    @Override
    public String getIdentifier(){
        return "ajlb";
    }

    /**
     * This is the version of this expansion.
     * <br>You don't have to use numbers, since it is set as a String.
     *
     * @return The version as a String.
     */
    @Override
    public String getVersion(){
        return plugin.getDescription().getVersion();
    }

    Pattern highNamePattern = Pattern.compile("lb_(.*)_([1-9][0-9]*)_(.*)_name");
    Pattern highValuePattern = Pattern.compile("lb_(.*)_([1-9][0-9]*)_(.*)_value");
    Pattern highValueFormattedPattern = Pattern.compile("lb_(.*)_([1-9][0-9]*)_(.*)_value_formatted");
    Pattern highValueRawPattern = Pattern.compile("lb_(.*)_([1-9][0-9]*)_(.*)_rawvalue");
    Pattern highSuffixPattern = Pattern.compile("lb_(.*)_([1-9][0-9]*)_(.*)_suffix");
    Pattern highPrefixPattern = Pattern.compile("lb_(.*)_([1-9][0-9]*)_(.*)_prefix");
    Pattern highColorPattern = Pattern.compile("lb_(.*)_([1-9][0-9]*)_(.*)_color");
    Pattern positionPattern = Pattern.compile("position_(.*)_(.*)");
    Pattern valuePattern = Pattern.compile("value_(.*)_(.*)");
    Pattern valueFormattedPattern = Pattern.compile("value_(.*)_(.*)_formatted");
    /**
     * This is the method called when a placeholder with our identifier
     * is found and needs a value.
     * <br>We specify the value identifier in this method.
     * <br>Since version 2.9.1 can you use OfflinePlayers in your requests.
     *
     * @param  player
     *         A {@link org.bukkit.OfflinePlayer OfflinePlayer}.
     * @param  identifier
     *         A String containing the identifier/value.
     *
     * @return Possibly-null String of the requested identifier.
     */
    @Override
    public String onRequest(OfflinePlayer player, String identifier) {


        Matcher highNameMatcher = highNamePattern.matcher(identifier);
        if(highNameMatcher.find()) {
            String board = highNameMatcher.group(1);
            String typeRaw = highNameMatcher.group(3).toUpperCase();
            StatEntry r = plugin.getTopManager().getStat(Integer.parseInt(highNameMatcher.group(2)), board, TimedType.valueOf(typeRaw));
            return r.getPlayer();
        }
        Matcher highPrefixMatcher = highPrefixPattern.matcher(identifier);
        if(highPrefixMatcher.find()) {
            String board = highPrefixMatcher.group(1);
            String typeRaw = highPrefixMatcher.group(3).toUpperCase();
            StatEntry r = plugin.getTopManager().getStat(Integer.parseInt(highPrefixMatcher.group(2)), board, TimedType.valueOf(typeRaw));
            return r.getPrefix();
        }
        Matcher highSuffixMatcher = highSuffixPattern.matcher(identifier);
        if(highSuffixMatcher.find()) {
            String board = highSuffixMatcher.group(1);
            String typeRaw = highSuffixMatcher.group(3).toUpperCase();
            StatEntry r = plugin.getTopManager().getStat(Integer.parseInt(highSuffixMatcher.group(2)), board, TimedType.valueOf(typeRaw));
            return r.getSuffix();
        }
        Matcher highColorMatcher = highColorPattern.matcher(identifier);
        if(highColorMatcher.find()) {
            String board = highColorMatcher.group(1);
            String typeRaw = highColorMatcher.group(3).toUpperCase();
            StatEntry r = plugin.getTopManager().getStat(Integer.parseInt(highColorMatcher.group(2)), board, TimedType.valueOf(typeRaw));
            if(r.getPrefix().isEmpty()) return "";
            String prefix = r.getPrefix();
            StringBuilder colors = new StringBuilder();
            int i = 0;
            for(char c : prefix.toCharArray()) {
                if(i == prefix.length()-1) break;
                if(c == '&' || c == '\u00A7') {
                    colors.append(c);
                    colors.append(prefix.charAt(i+1));
                }
                i++;
            }
            return colors.toString();
        }

        Matcher highValueRawMatcher = highValueRawPattern.matcher(identifier);
        if(highValueRawMatcher.find()) {
            String board = highValueRawMatcher.group(1);
            String typeRaw = highValueRawMatcher.group(3).toUpperCase();
            StatEntry r = plugin.getTopManager().getStat(Integer.parseInt(highValueRawMatcher.group(2)), board, TimedType.valueOf(typeRaw));
            DecimalFormat df = new DecimalFormat("#.##");
            return df.format(r.getScore());
        }
        Matcher highValueFormattedMatcher = highValueFormattedPattern.matcher(identifier);
        if(highValueFormattedMatcher.find()) {
            String board = highValueFormattedMatcher.group(1);
            String typeRaw = highValueFormattedMatcher.group(3).toUpperCase();
            StatEntry r = plugin.getTopManager().getStat(Integer.parseInt(highValueFormattedMatcher.group(2)), board, TimedType.valueOf(typeRaw));
            return r.getScoreFormatted();
        }
        Matcher highValueMatcher = highValuePattern.matcher(identifier);
        if(highValueMatcher.find()) {
            String board = highValueMatcher.group(1);
            String typeRaw = highValueMatcher.group(3).toUpperCase();
            StatEntry r = plugin.getTopManager().getStat(Integer.parseInt(highValueMatcher.group(2)), board, TimedType.valueOf(typeRaw));
            return r.getScorePretty();
        }


        Matcher positionMatcher = positionPattern.matcher(identifier);
        if(positionMatcher.find()) {
            String board = positionMatcher.group(1);
            String typeRaw = positionMatcher.group(2).toUpperCase();
            return plugin.getTopManager().getStatEntry(player, board, TimedType.valueOf(typeRaw)).getPosition()+"";
        }

        Matcher valueFormattedMatcher = valueFormattedPattern.matcher(identifier);
        if(valueFormattedMatcher.find()) {
            String board = valueFormattedMatcher.group(1);
            String typeRaw = valueFormattedMatcher.group(2).toUpperCase();
            return plugin.getTopManager().getStatEntry(player, board, TimedType.valueOf(typeRaw)).getScoreFormatted();
        }
        Matcher valueMatcher = valuePattern.matcher(identifier);
        if(valueMatcher.find()) {
            String board = valueMatcher.group(1);
            String typeRaw = valueMatcher.group(2).toUpperCase();
            return plugin.getTopManager().getStatEntry(player, board, TimedType.valueOf(typeRaw)).getScorePretty();
        }


        return null;
    }
}
