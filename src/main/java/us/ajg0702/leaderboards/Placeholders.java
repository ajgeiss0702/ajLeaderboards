package us.ajg0702.leaderboards;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.OfflinePlayer;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import us.ajg0702.leaderboards.boards.StatEntry;

/**
 * This class will automatically register as a placeholder expansion
 * when a jar including this class is added to the directory
 * {@code /plugins/PlaceholderAPI/expansions} on your server.
 * <br>
 * <br>If you create such a class inside your own plugin, you have to
 * register it manually in your plugins {@code onEnable()} by using
 * {@code new YourExpansionClass().register();}
 */
public class Placeholders extends PlaceholderExpansion {

    Main pl;
    public Placeholders(Main pl) {
        this.pl = pl;
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
        return pl.getDescription().getVersion();
    }

    Pattern highNamePattern = Pattern.compile("lb_(.*)_([1-9][0-9]*)_name");
    Pattern highValuePattern = Pattern.compile("lb_(.*)_([1-9][0-9]*)_value");
    Pattern highValueRawPattern = Pattern.compile("lb_(.*)_([1-9][0-9]*)_rawvalue");
    Pattern highSuffixPattern = Pattern.compile("lb_(.*)_([1-9][0-9]*)_suffix");
    Pattern highPrefixPattern = Pattern.compile("lb_(.*)_([1-9][0-9]*)_prefix");
    Pattern highColorPattern = Pattern.compile("lb_(.*)_([1-9][0-9]*)_color");
    Pattern positionPattern = Pattern.compile("position_(.*)");
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
            StatEntry r = Cache.getInstance().getStat(Integer.parseInt(highNameMatcher.group(2)), board);
            return r.getPlayer();
        }
        Matcher highPrefixMatcher = highPrefixPattern.matcher(identifier);
        if(highPrefixMatcher.find()) {
            String board = highPrefixMatcher.group(1);
            StatEntry r = Cache.getInstance().getStat(Integer.parseInt(highPrefixMatcher.group(2)), board);
            return r.getPrefix();
        }
        Matcher highSuffixMatcher = highSuffixPattern.matcher(identifier);
        if(highSuffixMatcher.find()) {
            String board = highSuffixMatcher.group(1);
            StatEntry r = Cache.getInstance().getStat(Integer.parseInt(highSuffixMatcher.group(2)), board);
            return r.getSuffix();
        }
        Matcher highColorMatcher = highColorPattern.matcher(identifier);
        if(highColorMatcher.find()) {
            String board = highColorMatcher.group(1);
            StatEntry r = Cache.getInstance().getStat(Integer.parseInt(highColorMatcher.group(2)), board);
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


        Matcher highValueMatcher = highValuePattern.matcher(identifier);
        if(highValueMatcher.find()) {
            String board = highValueMatcher.group(1);
            StatEntry r = Cache.getInstance().getStat(Integer.parseInt(highValueMatcher.group(2)), board);
            return r.getScorePretty();
        }
        Matcher highValueRawMatcher = highValueRawPattern.matcher(identifier);
        if(highValueRawMatcher.find()) {
            String board = highValueRawMatcher.group(1);
            StatEntry r = Cache.getInstance().getStat(Integer.parseInt(highValueRawMatcher.group(2)), board);
            return r.getScore()+"";
        }

        Matcher positionMatcher = positionPattern.matcher(identifier);
        if(positionMatcher.find()) {
            String board = positionMatcher.group(1);
            return Cache.getInstance().getPlace(player, board)+"";
        }

        return null;
    }
}
