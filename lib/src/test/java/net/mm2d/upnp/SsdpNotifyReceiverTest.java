/*
 * Copyright(C)  2017 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.SsdpNotifyReceiver.NotifyListener;
import net.mm2d.upnp.SsdpServerDelegate.Receiver;
import net.mm2d.util.NetworkUtils;
import net.mm2d.util.TestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;

import javax.annotation.Nonnull;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class SsdpNotifyReceiverTest {
    @Test
    public void setNotifyListener_受信メッセージが通知されること() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServerDelegate delegate = spy(new SsdpServerDelegate(mock(Receiver.class), networkInterface));
        final InterfaceAddress interfaceAddress = TestUtils.createInterfaceAddress("192.0.2.2", "255.255.255.0", (short) 16);
        doReturn(interfaceAddress).when(delegate).getInterfaceAddress();
        final SsdpNotifyReceiver receiver = spy(new SsdpNotifyReceiver(delegate));
        final SsdpRequest result[] = new SsdpRequest[1];
        receiver.setNotifyListener(new SsdpNotifyReceiver.NotifyListener() {
            @Override
            public void onReceiveNotify(@Nonnull final SsdpRequest message) {
                result[0] = message;
            }
        });

        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");
        final InetAddress address = InetAddress.getByName("192.0.2.2");
        receiver.onReceive(address, data, data.length);

        assertThat(result[0].getUuid(), is("uuid:01234567-89ab-cdef-0123-456789abcdef"));
    }

    @Test
    public void onReceive_同一セグメントからのメッセージは通知する() throws Exception {
        final SsdpNotifyReceiver receiver = spy(new SsdpNotifyReceiver(NetworkUtils.getAvailableInet4Interfaces().get(0)));
        final InterfaceAddress address = TestUtils.createInterfaceAddress("192.0.2.1", "255.255.0.0", (short) 24);
        doReturn(address).when(receiver).getInterfaceAddress();
        final NotifyListener listener = mock(NotifyListener.class);
        receiver.setNotifyListener(listener);
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");

        receiver.onReceive(InetAddress.getByName("192.0.2.2"), data, data.length);

        verify(listener).onReceiveNotify(ArgumentMatchers.any(SsdpRequest.class));
    }

    @Test
    public void onReceive_Listenerがnullでもクラッシュしない() throws Exception {
        final SsdpNotifyReceiver receiver = spy(new SsdpNotifyReceiver(NetworkUtils.getAvailableInet4Interfaces().get(0)));
        final InterfaceAddress address = TestUtils.createInterfaceAddress("192.0.2.1", "255.255.0.0", (short) 24);
        doReturn(address).when(receiver).getInterfaceAddress();
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");

        receiver.onReceive(InetAddress.getByName("192.0.2.2"), data, data.length);
    }

    @Test
    public void onReceive_異なるセグメントからのメッセージは無視する() throws Exception {
        final SsdpNotifyReceiver receiver = spy(new SsdpNotifyReceiver(NetworkUtils.getAvailableInet4Interfaces().get(0)));
        final InterfaceAddress address = TestUtils.createInterfaceAddress("192.0.2.1", "255.255.0.0", (short) 24);
        doReturn(address).when(receiver).getInterfaceAddress();
        final NotifyListener listener = mock(NotifyListener.class);
        receiver.setNotifyListener(listener);
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");

        receiver.onReceive(InetAddress.getByName("192.1.2.2"), data, data.length);

        verify(listener, never()).onReceiveNotify(ArgumentMatchers.any(SsdpRequest.class));
    }

    @Test
    public void onReceive_M_SEARCHパケットは無視する() throws Exception {
        final SsdpNotifyReceiver receiver = spy(new SsdpNotifyReceiver(NetworkUtils.getAvailableInet4Interfaces().get(0)));
        final InterfaceAddress address = TestUtils.createInterfaceAddress("192.0.2.1", "255.255.0.0", (short) 24);
        doReturn(address).when(receiver).getInterfaceAddress();
        final NotifyListener listener = mock(NotifyListener.class);
        receiver.setNotifyListener(listener);

        final SsdpRequest message = new SsdpRequest();
        message.setMethod(SsdpMessage.M_SEARCH);
        message.setUri("*");
        message.setHeader(Http.HOST, SsdpServer.SSDP_ADDR + ":" + String.valueOf(SsdpServer.SSDP_PORT));
        message.setHeader(Http.MAN, SsdpMessage.SSDP_DISCOVER);
        message.setHeader(Http.MX, "1");
        message.setHeader(Http.ST, SsdpSearchServer.ST_ALL);
        final byte[] data = message.getMessage().getMessageString().getBytes();

        receiver.onReceive(InetAddress.getByName("192.0.2.2"), data, data.length);

        verify(listener, never()).onReceiveNotify(ArgumentMatchers.any(SsdpRequest.class));
    }

    @Test
    public void onReceive_ByeByeパケットは通知する() throws Exception {
        final SsdpNotifyReceiver receiver = spy(new SsdpNotifyReceiver(NetworkUtils.getAvailableInet4Interfaces().get(0)));
        final InterfaceAddress address = TestUtils.createInterfaceAddress("192.0.2.1", "255.255.0.0", (short) 24);
        doReturn(address).when(receiver).getInterfaceAddress();
        final NotifyListener listener = mock(NotifyListener.class);
        receiver.setNotifyListener(listener);

        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-byebye0.bin");

        receiver.onReceive(InetAddress.getByName("192.0.2.2"), data, data.length);

        verify(listener).onReceiveNotify(ArgumentMatchers.any(SsdpRequest.class));
    }

    @Test
    public void onReceive_LocationとSourceが不一致のメッセージは無視する() throws Exception {
        final SsdpNotifyReceiver receiver = spy(new SsdpNotifyReceiver(NetworkUtils.getAvailableInet4Interfaces().get(0)));
        final InterfaceAddress address = TestUtils.createInterfaceAddress("192.0.2.1", "255.255.0.0", (short) 24);
        doReturn(address).when(receiver).getInterfaceAddress();
        final NotifyListener listener = mock(NotifyListener.class);
        receiver.setNotifyListener(listener);
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");

        receiver.onReceive(InetAddress.getByName("192.0.2.3"), data, data.length);

        verify(listener, never()).onReceiveNotify(ArgumentMatchers.any(SsdpRequest.class));
    }

    @Test
    public void onReceive_IOExceptionが発生してもクラッシュしない() throws Exception {
        final SsdpNotifyReceiver receiver = spy(new SsdpNotifyReceiver(NetworkUtils.getAvailableInet4Interfaces().get(0)));
        final InterfaceAddress address = TestUtils.createInterfaceAddress("192.0.2.1", "255.255.0.0", (short) 24);
        doReturn(address).when(receiver).getInterfaceAddress();
        doThrow(new IOException()).when(receiver).createSsdpRequestMessage(ArgumentMatchers.any(byte[].class), anyInt());
        final NotifyListener listener = mock(NotifyListener.class);
        receiver.setNotifyListener(listener);
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");

        receiver.onReceive(InetAddress.getByName("192.0.2.2"), data, data.length);

        verify(listener, never()).onReceiveNotify(ArgumentMatchers.any(SsdpRequest.class));
    }

    @Test
    public void isSameSegment() throws Exception {
        assertThat(
                SsdpNotifyReceiver.isSameSegment(
                        TestUtils.createInterfaceAddress("192.168.0.1", "255.255.255.0", (short) 24),
                        InetAddress.getByName("192.168.0.255")),
                is(true));
        assertThat(
                SsdpNotifyReceiver.isSameSegment(
                        TestUtils.createInterfaceAddress("192.168.0.1", "255.255.255.128", (short) 25),
                        InetAddress.getByName("192.168.0.255")),
                is(false));
        assertThat(
                SsdpNotifyReceiver.isSameSegment(
                        TestUtils.createInterfaceAddress("192.168.0.1", "255.255.255.0", (short) 24),
                        InetAddress.getByName("192.168.1.255")),
                is(false));
        assertThat(
                SsdpNotifyReceiver.isSameSegment(
                        TestUtils.createInterfaceAddress("192.168.0.1", "255.255.254.0", (short) 23),
                        InetAddress.getByName("192.168.1.255")),
                is(true));
    }
}