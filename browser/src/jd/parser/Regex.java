/**
 * Copyright (c) 2009 - 2010 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package jd.parser;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author thomas
 * 
 */

public class Regex {

    private Matcher matcher;

    public Regex(final Matcher matcher) {
        if (matcher != null) {
            this.matcher = matcher;
        }
    }

    public Regex(final Object data, final Pattern pattern) {
        this(data.toString(), pattern);
    }

    public Regex(final Object data, final String pattern) {
        this(data.toString(), pattern);
    }

    public Regex(final Object data, final String pattern, final int flags) {
        this(data.toString(), pattern, flags);
    }

    public Regex(final String data, final Pattern pattern) {
        if (data != null && pattern != null) {
            matcher = pattern.matcher(data);
        }
    }

    public Regex(final String data, final String pattern) {
        if (data != null && pattern != null) {
            matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(data);
        }
    }

    public Regex(final String data, final String pattern, final int flags) {
        if (data != null && pattern != null) {
            matcher = Pattern.compile(pattern, flags).matcher(data);
        }
    }

    /**
     * Gibt die Anzahl der Treffer zurück
     * 
     * @return
     */
    public int count() {
        if (matcher == null) {
            return 0;
        } else {
            matcher.reset();
            int c = 0;
            final Matcher matchertmp = matcher;
            while (matchertmp.find()) {
                c++;
            }
            return c;
        }
    }

    public String getMatch(final int group) {
        if (matcher != null) {
            final Matcher matcher = this.matcher;
            matcher.reset();
            if (matcher.find()) { return matcher.group(group + 1); }
        }
        return null;
    }

    public Matcher getMatcher() {
        if (matcher != null) {
            matcher.reset();
        }
        return matcher;
    }

    /**
     * Gibt alle Treffer eines Matches in einem 2D array aus
     * 
     * @return
     */
    public String[][] getMatches() {
        if (matcher == null) {
            return null;
        } else {
            final Matcher matcher = this.matcher;
            matcher.reset();
            final ArrayList<String[]> ar = new ArrayList<String[]>();
            while (matcher.find()) {
                int c = matcher.groupCount();
                int d = 1;
                String[] group;
                if (c == 0) {
                    group = new String[c + 1];
                    d = 0;
                } else {
                    group = new String[c];
                }

                for (int i = d; i <= c; i++) {
                    group[i - d] = matcher.group(i);
                }
                ar.add(group);
            }
            return (ar.size() == 0) ? new String[][] {} : ar.toArray(new String[][] {});
        }
    }

    public String[] getColumn(int x) {
        if (matcher == null) {
            return null;
        } else {
            x++;
            final Matcher matcher = this.matcher;
            matcher.reset();

            final ArrayList<String> ar = new ArrayList<String>();
            while (matcher.find()) {
                ar.add(matcher.group(x));
            }
            return ar.toArray(new String[ar.size()]);
        }
    }

    public boolean matches() {
        final Matcher matcher = this.matcher;
        if (matcher == null) {
            return false;
        } else {
            matcher.reset();
            return matcher.find();
        }
    }

    /**
     * Setzt den Matcher
     * 
     * @param matcher
     */
    public void setMatcher(final Matcher matcher) {
        this.matcher = matcher;
    }

    @Override
    public String toString() {
        final StringBuilder ret = new StringBuilder();
        final String[][] matches = getMatches();
        final int matchesLength = matches.length;
        String[] match;
        int matchLength;
        for (int i = 0; i < matchesLength; i++) {
            match = matches[i];
            matchLength = match.length;
            for (int j = 0; j < matchLength; j++) {
                ret.append("match[");
                ret.append(i);
                ret.append("][");
                ret.append(j);
                ret.append("] = ");
                ret.append(match[j]);
                ret.append(System.getProperty("line.separator"));
            }
        }
        matcher.reset();
        return ret.toString();
    }

    public String getMatch(int entry, int group) {
        if (matcher != null) {
            final Matcher matcher = this.matcher;
            matcher.reset();
            // group++;
            entry++;
            int groupCount = 0;
            while (matcher.find()) {
                if (groupCount == group) { return matcher.group(entry); }
                groupCount++;
            }
        }
        return null;
    }

    public String[] getRow(final int y) {
        if (matcher != null) {
            final Matcher matcher = this.matcher;
            matcher.reset();
            int groupCount = 0;
            while (matcher.find()) {
                if (groupCount == y) {
                    final int c = matcher.groupCount();

                    final String[] group = new String[c];

                    for (int i = 1; i <= c; i++) {
                        group[i - 1] = matcher.group(i);
                    }
                    return group;
                }
                groupCount++;
            }
        }
        return null;
    }

    public static String escape(final String pattern) {
        final char[] specials = new char[] { '(', '[', '{', '\\', '^', '-', '$', '|', ']', '}', ')', '?', '*', '+', '.' };
        final int patternLength = pattern.length();
        final StringBuilder sb = new StringBuilder();
        sb.setLength(patternLength);
        char act;
        for (int i = 0; i < patternLength; i++) {
            act = pattern.charAt(i);
            for (char s : specials) {
                if (act == s) {
                    sb.append('\\');
                    break;
                }
            }
            sb.append(act);
        }
        return sb.toString().trim();
    }

    public static String[] getLines(final String arg) {
        if (arg == null) {
            return new String[] {};
        } else {
            final String[] temp = arg.split("[\r\n]{1,2}");
            final int tempLength = temp.length;
            final String[] output = new String[tempLength];
            for (int i = 0; i < tempLength; i++) {
                output[i] = temp[i].trim();
            }
            return output;
        }
    }

    public static boolean matches(final Object str, final Pattern pat) {
        return new Regex(str, pat).matches();
    }

    public static boolean matches(final Object page, final String string) {
        return new Regex(page, string).matches();
    }

}