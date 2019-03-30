/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(JUnit4.class)
public class TextUtilsTest {
    @Test
    public void isEmpty_nullもしくは空文字でtrueそれ意外false() {
        assertThat(TextUtils.isEmpty(null), is(true));
        assertThat(TextUtils.isEmpty(""), is(true));
        assertThat(TextUtils.isEmpty(" "), is(false));
    }

    @Test
    public void equals_両者の内容が等しい場合にtrueそれ以外false() {
        assertThat(TextUtils.equals(null, null), is(true));
        assertThat(TextUtils.equals(null, ""), is(false));
        assertThat(TextUtils.equals("", null), is(false));
        assertThat(TextUtils.equals("a", "b"), is(false));
        assertThat(TextUtils.equals("1", String.valueOf(1)), is(true));
    }

    @Test
    public void toLowerCase_nullでも問題ない() {
        assertThat(TextUtils.toLowerCase(null), is(nullValue()));
        assertThat(TextUtils.toLowerCase("ABC"), is("abc"));
    }

    @Test
    public void toUpperCase_nullでも問題ない() {
        assertThat(TextUtils.toUpperCase(null), is(nullValue()));
        assertThat(TextUtils.toUpperCase("abc"), is("ABC"));
    }
}
