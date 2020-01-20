package dubbo.mini.common.utils;

import dubbo.mini.common.UnsafeStringWriter;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {


    private static final Pattern KVP_PATTERN = Pattern.compile("([_.a-zA-Z0-9][-_.a-zA-Z0-9]*)[=](.*)"); //key value pair pattern.
    private static final Pattern INT_PATTERN = Pattern.compile("^\\d+$");

    public static final String EMPTY = "";

    public static String trimToEmpty(String str) {
        return str == null ? EMPTY : str.trim();
    }


    public static int parseInteger(String str) {
        return isInteger(str) ? Integer.parseInt(str) : 0;
    }

    public static boolean isInteger(String str) {
        return !isEmpty(str) && INT_PATTERN.matcher(str).matches();
    }

    public static String toString(Throwable e) {
        UnsafeStringWriter w = new UnsafeStringWriter();
        PrintWriter p = new PrintWriter(w);
        p.print(e.getClass().getName());
        if (e.getMessage() != null) {
            p.print(": " + e.getMessage());
        }
        p.println();
        try {
            e.printStackTrace(p);
            return w.toString();
        } finally {
            p.close();
        }
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }


    /**
     * parse query string to Parameters.
     *
     * @param qs query string.
     * @return Parameters instance.
     */
    public static Map<String, String> parseQueryString(String qs) {
        if (isEmpty(qs)) {
            return new HashMap<>();
        }
        return parseKeyValuePair(qs, "\\&");
    }


    private static Map<String, String> parseKeyValuePair(String str, String itemSeparator) {
        String[] tmp = str.split(itemSeparator);
        Map<String, String> map = new HashMap<String, String>(tmp.length);
        for (int i = 0; i < tmp.length; i++) {
            Matcher matcher = KVP_PATTERN.matcher(tmp[i]);
            if (!matcher.matches()) {
                continue;
            }
            map.put(matcher.group(1), matcher.group(2));
        }
        return map;
    }
}
