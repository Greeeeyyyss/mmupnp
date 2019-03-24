/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server;

import net.mm2d.upnp.Http;
import net.mm2d.upnp.MockMulticastSocket;
import net.mm2d.upnp.SsdpMessage;
import net.mm2d.upnp.internal.message.SsdpRequest;
import net.mm2d.upnp.internal.message.SsdpResponse;
import net.mm2d.upnp.internal.server.SsdpSearchServer.ResponseListener;
import net.mm2d.upnp.internal.server.SsdpServerDelegate.Receiver;
import net.mm2d.upnp.internal.thread.TaskExecutors;
import net.mm2d.upnp.util.NetworkUtils;
import net.mm2d.upnp.util.TestUtils;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(JUnit4.class)
public class SsdpSearchServerTest {
    private TaskExecutors mTaskExecutors;

    @Before
    public void setUp() {
        mTaskExecutors = new TaskExecutors();
    }

    @After
    public void terminate() {
        mTaskExecutors.terminate();
    }

    @Test(timeout = 5000)
    public void search_ST_ALLでのサーチ() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServerDelegate delegate = spy(new SsdpServerDelegate(mTaskExecutors, mock(Receiver.class), Address.IP_V4, networkInterface));
        final InterfaceAddress interfaceAddress = TestUtils.createInterfaceAddress("192.0.2.2", "255.255.255.0", 16);
        doReturn(interfaceAddress).when(delegate).getInterfaceAddress();
        final MockMulticastSocket socket = new MockMulticastSocket();
        doReturn(socket).when(delegate).createMulticastSocket(anyInt());
        final SsdpSearchServer server = Mockito.spy(new SsdpSearchServer(delegate));
        server.start();
        server.search();
        Thread.sleep(100);
        server.stop();

        final DatagramPacket packet = socket.getSendPacket();
        final SsdpRequest message = SsdpRequest.create(
                mock(InetAddress.class), packet.getData(), packet.getLength());
        assertThat(message.getMethod(), Matchers.is(SsdpMessage.M_SEARCH));
        assertThat(message.getHeader(Http.ST), is(SsdpSearchServer.ST_ALL));
        assertThat(message.getHeader(Http.MAN), is(SsdpMessage.SSDP_DISCOVER));
    }

    @Test(timeout = 5000)
    public void search_ST_ROOTDEVICEでのサーチ() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServerDelegate delegate = spy(new SsdpServerDelegate(mTaskExecutors, mock(Receiver.class), Address.IP_V4, networkInterface));
        final InterfaceAddress interfaceAddress = TestUtils.createInterfaceAddress("192.0.2.2", "255.255.255.0", 16);
        doReturn(interfaceAddress).when(delegate).getInterfaceAddress();
        final MockMulticastSocket socket = new MockMulticastSocket();
        doReturn(socket).when(delegate).createMulticastSocket(anyInt());
        final SsdpSearchServer server = Mockito.spy(new SsdpSearchServer(delegate));
        server.start();
        server.search(SsdpSearchServer.ST_ROOTDEVICE);
        Thread.sleep(100);
        server.stop();

        final DatagramPacket packet = socket.getSendPacket();
        final SsdpRequest message = SsdpRequest.create(
                mock(InetAddress.class), packet.getData(), packet.getLength());
        assertThat(message.getMethod(), is(SsdpMessage.M_SEARCH));
        assertThat(message.getHeader(Http.ST), is(SsdpSearchServer.ST_ROOTDEVICE));
        assertThat(message.getHeader(Http.MAN), is(SsdpMessage.SSDP_DISCOVER));
    }

    @Test
    public void setResponseListener_受信メッセージが通知されること() throws Exception {
        final SsdpServerDelegate delegate = mock(SsdpServerDelegate.class);
        final InterfaceAddress interfaceAddress = TestUtils.createInterfaceAddress("192.0.2.2", "255.255.255.0", 16);
        doReturn(interfaceAddress).when(delegate).getInterfaceAddress();
        doReturn(interfaceAddress.getAddress()).when(delegate).getLocalAddress();
        final SsdpSearchServer server = Mockito.spy(new SsdpSearchServer(delegate));
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-search-response0.bin");
        final InetAddress address = InetAddress.getByName("192.0.2.2");
        final ArgumentCaptor<SsdpResponse> captor = ArgumentCaptor.forClass(SsdpResponse.class);
        final ResponseListener listener = mock(ResponseListener.class);
        doNothing().when(listener).onReceiveResponse(captor.capture());
        server.setResponseListener(listener);
        server.onReceive(address, data, data.length);

        final SsdpResponse response = captor.getValue();
        assertThat(response.getStatus(), is(Http.Status.HTTP_OK));
        assertThat(response.getUuid(), is("uuid:01234567-89ab-cdef-0123-456789abcdef"));
    }

    @Test
    public void onReceive_listenerがコールされる() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpSearchServer server = new SsdpSearchServer(mTaskExecutors, Address.IP_V4, networkInterface);
        final ResponseListener listener = mock(ResponseListener.class);
        server.setResponseListener(listener);
        final InetAddress address = InetAddress.getByName("192.0.2.2");
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-search-response0.bin");

        server.onReceive(address, data, data.length);

        verify(listener, times(1)).onReceiveResponse(ArgumentMatchers.any(SsdpResponse.class));
    }

    @Test
    public void onReceive_アドレス不一致ならlistenerコールされない() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpSearchServer server = new SsdpSearchServer(mTaskExecutors, Address.IP_V4, networkInterface);
        final ResponseListener listener = mock(ResponseListener.class);
        server.setResponseListener(listener);
        final InetAddress address = InetAddress.getByName("192.0.2.3");
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-search-response0.bin");

        server.onReceive(address, data, data.length);

        verify(listener, never()).onReceiveResponse(ArgumentMatchers.any(SsdpResponse.class));
    }

    @Test
    public void onReceive_データに問題がある場合listenerコールされない() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpSearchServer server = new SsdpSearchServer(mTaskExecutors, Address.IP_V4, networkInterface);
        final ResponseListener listener = mock(ResponseListener.class);
        server.setResponseListener(listener);
        final InetAddress address = InetAddress.getByName("192.0.2.2");
        final byte[] data = new byte[0];

        server.onReceive(address, data, data.length);

        verify(listener, never()).onReceiveResponse(ArgumentMatchers.any(SsdpResponse.class));
    }

    @Test
    public void onReceive_listenerが登録されていなくてもクラッシュしない() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpSearchServer server = new SsdpSearchServer(mTaskExecutors, Address.IP_V4, networkInterface);

        final InetAddress address = InetAddress.getByName("192.0.2.2");
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-search-response0.bin");

        server.onReceive(address, data, data.length);
    }

}