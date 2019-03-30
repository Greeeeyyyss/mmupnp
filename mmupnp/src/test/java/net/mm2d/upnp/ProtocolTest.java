/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 */

package net.mm2d.upnp;

import net.mm2d.upnp.util.NetworkUtils;

import org.junit.Test;

import java.net.NetworkInterface;

import static org.junit.Assert.*;

public class ProtocolTest {
    @Test
    public void getAvailableInterfaces() {
        for (final NetworkInterface nif : Protocol.IP_V4_ONLY.getAvailableInterfaces()) {
            if (!NetworkUtils.isAvailableInet4Interface(nif)) {
                fail();
            }
        }
        for (final NetworkInterface nif : Protocol.IP_V6_ONLY.getAvailableInterfaces()) {
            if (!NetworkUtils.isAvailableInet6Interface(nif)) {
                fail();
            }
        }
        for (final NetworkInterface nif : Protocol.DUAL_STACK.getAvailableInterfaces()) {
            if (!NetworkUtils.isAvailableInet4Interface(nif) && !NetworkUtils.isAvailableInet6Interface(nif)) {
                fail();
            }
        }
    }
}