/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl;

import net.mm2d.upnp.Argument;
import net.mm2d.upnp.StateVariable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(JUnit4.class)
public class ArgumentTest {
    @Test(expected = IllegalStateException.class)
    public void build_Nameを設定していないとException() {
        new ArgumentImpl.Builder()
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void build_RelatedStateVariableを設定していないとException() {
        new ArgumentImpl.Builder()
                .setName("")
                .build();
    }

    @Test
    public void getRelatedStateVariableName_setした値が返る() {
        final String name = "name";
        final ArgumentImpl.Builder builder = new ArgumentImpl.Builder()
                .setRelatedStateVariableName(name);
        assertThat(builder.getRelatedStateVariableName(), is(name));
    }

    @Test
    public void build_Builderで指定した値が得られる() {
        final String name = "name";
        final StateVariable stateVariable = mock(StateVariable.class);
        final Argument argument = new ArgumentImpl.Builder()
                .setName(name)
                .setDirection("in")
                .setRelatedStateVariable(stateVariable)
                .build();
        assertThat(argument.getRelatedStateVariable(), is(stateVariable));
        assertThat(argument.getName(), is(name));
        assertThat(argument.isInputDirection(), is(true));
    }

    @Test
    public void isInputDirection_Builderでoutを指定した場合false() {
        final String name = "name";
        final StateVariable stateVariable = mock(StateVariable.class);
        final Argument argument = new ArgumentImpl.Builder()
                .setName(name)
                .setDirection("out")
                .setRelatedStateVariable(stateVariable)
                .build();
        assertThat(argument.isInputDirection(), is(false));
    }
}
