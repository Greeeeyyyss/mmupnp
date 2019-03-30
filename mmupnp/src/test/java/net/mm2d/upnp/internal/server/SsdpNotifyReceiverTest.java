/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server;

import net.mm2d.upnp.Http;
import net.mm2d.upnp.SsdpMessage;
import net.mm2d.upnp.internal.message.SsdpRequest;
import net.mm2d.upnp.internal.server.SsdpNotifyReceiver.NotifyListener;
import net.mm2d.upnp.internal.server.SsdpServerDelegate.Receiver;
import net.mm2d.upnp.internal.thread.TaskExecutors;
import net.mm2d.upnp.util.NetworkUtils;
import net.mm2d.upnp.util.TestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"NonAsciiCharacters", "ResultOfMethodCallIgnored"})
@RunWith(JUnit4.class)
public class SsdpNotifyReceiverTest {
    private TaskExecutors mTaskExecutors;

    @Before
    public void setUp() {
        mTaskExecutors = new TaskExecutors();
    }

    @After
    public void terminate() {
        mTaskExecutors.terminate();
    }

    @Test
    public void setNotifyListener_受信メッセージが通知されること() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServerDelegate delegate = spy(new SsdpServerDelegate(mTaskExecutors, mock(Receiver.class), Address.IP_V4, networkInterface));
        final InterfaceAddress interfaceAddress = TestUtils.createInterfaceAddress("192.0.2.2", "255.255.255.0", 16);
        doReturn(interfaceAddress).when(delegate).getInterfaceAddress();
        final SsdpNotifyReceiver receiver = spy(new SsdpNotifyReceiver(delegate));

        final ArgumentCaptor<SsdpRequest> captor = ArgumentCaptor.forClass(SsdpRequest.class);
        final NotifyListener listener = mock(NotifyListener.class);
        doNothing().when(listener).onReceiveNotify(captor.capture());
        receiver.setNotifyListener(listener);

        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");
        final InetAddress address = InetAddress.getByName("192.0.2.2");
        receiver.onReceive(address, data, data.length);

        assertThat(captor.getValue().getUuid(), is("uuid:01234567-89ab-cdef-0123-456789abcdef"));
    }

    @Test
    public void onReceive_同一セグメントからのメッセージは通知する() throws Exception {
        final SsdpNotifyReceiver receiver = spy(new SsdpNotifyReceiver(mTaskExecutors, Address.IP_V4, NetworkUtils.getAvailableInet4Interfaces().get(0)));
        final InterfaceAddress address = TestUtils.createInterfaceAddress("192.0.2.1", "255.255.0.0", 24);
        doReturn(address).when(receiver).getInterfaceAddress();
        final NotifyListener listener = mock(NotifyListener.class);
        receiver.setNotifyListener(listener);
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");

        receiver.onReceive(InetAddress.getByName("192.0.2.2"), data, data.length);

        verify(listener).onReceiveNotify(ArgumentMatchers.any(SsdpRequest.class));
    }

    @Test
    public void onReceive_Listenerがnullでもクラッシュしない() throws Exception {
        final SsdpNotifyReceiver receiver = spy(new SsdpNotifyReceiver(mTaskExecutors, Address.IP_V4, NetworkUtils.getAvailableInet4Interfaces().get(0)));
        final InterfaceAddress address = TestUtils.createInterfaceAddress("192.0.2.1", "255.255.0.0", 24);
        doReturn(address).when(receiver).getInterfaceAddress();
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");

        receiver.onReceive(InetAddress.getByName("192.0.2.2"), data, data.length);
    }

    @Test
    public void onReceive_異なるセグメントからのメッセージは無視する() throws Exception {
        final SsdpNotifyReceiver receiver = spy(new SsdpNotifyReceiver(mTaskExecutors, Address.IP_V4, NetworkUtils.getAvailableInet4Interfaces().get(0)));
        final InterfaceAddress address = TestUtils.createInterfaceAddress("192.0.2.1", "255.255.0.0", 24);
        doReturn(address).when(receiver).getInterfaceAddress();
        final NotifyListener listener = mock(NotifyListener.class);
        receiver.setNotifyListener(listener);
        receiver.setSegmentCheckEnabled(true);
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");

        receiver.onReceive(InetAddress.getByName("192.1.2.2"), data, data.length);

        verify(listener, never()).onReceiveNotify(ArgumentMatchers.any(SsdpRequest.class));
    }

    @Test
    public void onReceive_M_SEARCHパケットは無視する() throws Exception {
        final SsdpNotifyReceiver receiver = spy(new SsdpNotifyReceiver(mTaskExecutors, Address.IP_V4, NetworkUtils.getAvailableInet4Interfaces().get(0)));
        final InterfaceAddress address = TestUtils.createInterfaceAddress("192.0.2.1", "255.255.0.0", 24);
        doReturn(address).when(receiver).getInterfaceAddress();
        final NotifyListener listener = mock(NotifyListener.class);
        receiver.setNotifyListener(listener);

        final SsdpRequest message = SsdpRequest.create();
        message.setMethod(SsdpMessage.M_SEARCH);
        message.setUri("*");
        message.setHeader(Http.HOST, Address.IP_V4.getAddressString());
        message.setHeader(Http.MAN, SsdpMessage.SSDP_DISCOVER);
        message.setHeader(Http.MX, "1");
        message.setHeader(Http.ST, SsdpSearchServer.ST_ALL);
        final byte[] data = message.getMessage().getMessageString().getBytes();

        receiver.onReceive(InetAddress.getByName("192.0.2.2"), data, data.length);

        verify(listener, never()).onReceiveNotify(ArgumentMatchers.any(SsdpRequest.class));
    }

    @Test
    public void onReceive_ByeByeパケットは通知する() throws Exception {
        final SsdpNotifyReceiver receiver = spy(new SsdpNotifyReceiver(mTaskExecutors, Address.IP_V4, NetworkUtils.getAvailableInet4Interfaces().get(0)));
        final InterfaceAddress address = TestUtils.createInterfaceAddress("192.0.2.1", "255.255.0.0", 24);
        doReturn(address).when(receiver).getInterfaceAddress();
        final NotifyListener listener = mock(NotifyListener.class);
        receiver.setNotifyListener(listener);

        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-byebye0.bin");

        receiver.onReceive(InetAddress.getByName("192.0.2.2"), data, data.length);

        verify(listener).onReceiveNotify(ArgumentMatchers.any(SsdpRequest.class));
    }

    @Test
    public void onReceive_LocationとSourceが不一致のメッセージは無視する() throws Exception {
        final SsdpNotifyReceiver receiver = spy(new SsdpNotifyReceiver(mTaskExecutors, Address.IP_V4, NetworkUtils.getAvailableInet4Interfaces().get(0)));
        final InterfaceAddress address = TestUtils.createInterfaceAddress("192.0.2.1", "255.255.0.0", 24);
        doReturn(address).when(receiver).getInterfaceAddress();
        final NotifyListener listener = mock(NotifyListener.class);
        receiver.setNotifyListener(listener);
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");

        receiver.onReceive(InetAddress.getByName("192.0.2.3"), data, data.length);

        verify(listener, never()).onReceiveNotify(ArgumentMatchers.any(SsdpRequest.class));
    }

    @Test
    public void onReceive_IOExceptionが発生してもクラッシュしない() throws Exception {
        final SsdpNotifyReceiver receiver = spy(new SsdpNotifyReceiver(mTaskExecutors, Address.IP_V4, NetworkUtils.getAvailableInet4Interfaces().get(0)));
        final InterfaceAddress address = TestUtils.createInterfaceAddress("192.0.2.1", "255.255.0.0", 24);
        doReturn(address).when(receiver).getInterfaceAddress();
        doThrow(new IOException()).when(receiver).createSsdpRequestMessage(ArgumentMatchers.any(byte[].class), anyInt());
        final NotifyListener listener = mock(NotifyListener.class);
        receiver.setNotifyListener(listener);
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");

        receiver.onReceive(InetAddress.getByName("192.0.2.2"), data, data.length);

        verify(listener, never()).onReceiveNotify(ArgumentMatchers.any(SsdpRequest.class));
    }

    @Test
    public void send_delegateがコールされる() {
        final SsdpServerDelegate delegate = mock(SsdpServerDelegate.class);
        final SsdpNotifyReceiver receiver = new SsdpNotifyReceiver(delegate);
        final SsdpMessage message = mock(SsdpMessage.class);

        receiver.send(message);

        verify(delegate, times(1)).send(message);
    }

    @Test
    public void invalidAddress_IPv4() throws Exception {
        final SsdpServerDelegate delegate = mock(SsdpServerDelegate.class);
        doReturn(Address.IP_V4).when(delegate).getAddress();
        final SsdpNotifyReceiver receiver = spy(new SsdpNotifyReceiver(delegate));
        receiver.setSegmentCheckEnabled(true);

        InterfaceAddress interfaceAddress;

        interfaceAddress = TestUtils.createInterfaceAddress("192.168.0.1", "255.255.255.0", 24);
        doReturn(interfaceAddress).when(delegate).getInterfaceAddress();

        receiver.setSegmentCheckEnabled(true);
        assertThat(receiver.invalidAddress(InetAddress.getByName("192.168.0.255")), is(false));
        receiver.setSegmentCheckEnabled(false);
        assertThat(receiver.invalidAddress(InetAddress.getByName("192.168.0.255")), is(false));

        interfaceAddress = TestUtils.createInterfaceAddress("192.168.0.1", "255.255.255.128", 25);
        doReturn(interfaceAddress).when(delegate).getInterfaceAddress();

        receiver.setSegmentCheckEnabled(true);
        assertThat(receiver.invalidAddress(InetAddress.getByName("192.168.0.255")), is(true));
        receiver.setSegmentCheckEnabled(false);
        assertThat(receiver.invalidAddress(InetAddress.getByName("192.168.0.255")), is(false));

        interfaceAddress = TestUtils.createInterfaceAddress("192.168.0.1", "255.255.255.0", 24);
        doReturn(interfaceAddress).when(delegate).getInterfaceAddress();

        receiver.setSegmentCheckEnabled(true);
        assertThat(receiver.invalidAddress(InetAddress.getByName("192.168.1.255")), is(true));

        interfaceAddress = TestUtils.createInterfaceAddress("192.168.0.1", "255.255.254.0", 23);
        doReturn(interfaceAddress).when(delegate).getInterfaceAddress();

        receiver.setSegmentCheckEnabled(true);
        assertThat(receiver.invalidAddress(InetAddress.getByName("192.168.1.255")), is(false));

        receiver.setSegmentCheckEnabled(true);
        assertThat(receiver.invalidAddress(InetAddress.getByName("fe80::a831:801b:8dc6:421f")), is(true));
        receiver.setSegmentCheckEnabled(false);
        assertThat(receiver.invalidAddress(InetAddress.getByName("fe80::a831:801b:8dc6:421f")), is(true));
    }

    @Test
    public void invalidAddress_IPv6() throws Exception {
        final SsdpServerDelegate delegate = mock(SsdpServerDelegate.class);
        doReturn(Address.IP_V6_LINK_LOCAL).when(delegate).getAddress();
        final SsdpNotifyReceiver receiver = spy(new SsdpNotifyReceiver(delegate));
        receiver.setSegmentCheckEnabled(true);

        InterfaceAddress interfaceAddress;

        interfaceAddress = TestUtils.createInterfaceAddress("fe80::a831:801b:8dc6:421f", "255.255.0.0", 16);
        doReturn(interfaceAddress).when(delegate).getInterfaceAddress();

        receiver.setSegmentCheckEnabled(true);
        assertThat(receiver.invalidAddress(InetAddress.getByName("2001:db8::1")), is(false));
        receiver.setSegmentCheckEnabled(false);
        assertThat(receiver.invalidAddress(InetAddress.getByName("2001:db8::1")), is(false));

        receiver.setSegmentCheckEnabled(true);
        assertThat(receiver.invalidAddress(InetAddress.getByName("192.168.0.255")), is(true));
        receiver.setSegmentCheckEnabled(false);
        assertThat(receiver.invalidAddress(InetAddress.getByName("192.168.0.255")), is(true));
    }
}