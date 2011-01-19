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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.appwork.utils.AwReg;

/**
 * @author thomas
 * 
 */

public class Regex extends AwReg {

    private Matcher matcher;

    public Regex(final Matcher matcher) {
        super(matcher);
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
        super(data, pattern);
    }

    public Regex(final String data, final String pattern) {
        super(data, pattern);
    }

    public Regex(final String data, final String pattern, final int flags) {
        super(data, pattern, flags);
    }

}