import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexTest {
    @Test
    public void regex() {
        String prefix = "&cOwner &4";
        StringBuilder colors = new StringBuilder();

        int i = 0;
        for(char c : prefix.toCharArray()) {
            if(i == prefix.length()-1) break;
            if(c == '&') {
                colors.append(c);
                colors.append(prefix.charAt(i+1));
            }
            i++;
        }
       /* Matcher colorMatcher = Pattern.compile("(&[0-9a-fk-or]).*", Pattern.MULTILINE).matcher(prefix);

        System.out.println(colorMatcher.group());
        System.out.println(colorMatcher.groupCount());
        System.out.println(colorMatcher.matches());
        System.out.println(prefix.matches("(&[0-9a-fk-or]).*"));

        for(int i = 1; i <= colorMatcher.groupCount(); i++) {
            colors.append(colorMatcher.group(i));
        }*/
        System.out.println("Colors: " + colors);
    }
}
