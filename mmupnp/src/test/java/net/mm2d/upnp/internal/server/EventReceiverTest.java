/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server;

import net.mm2d.upnp.Http;
import net.mm2d.upnp.HttpRequest;
import net.mm2d.upnp.HttpResponse;
import net.mm2d.upnp.internal.server.EventReceiver.ClientTask;
import net.mm2d.upnp.internal.server.EventReceiver.EventMessageListener;
import net.mm2d.upnp.internal.thread.TaskExecutors;
import net.mm2d.upnp.internal.util.StringPair;
import net.mm2d.upnp.util.TestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentMatchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import javax.annotation.Nonnull;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(JUnit4.class)
public class EventReceiverTest {
    private byte[] mBadRequest;
    private byte[] mFailRequest;
    private byte[] mNotifyRequest;
    private static final String SID = "uuid:s1234567-89ab-cdef-0123-456789abcdef";
    private TaskExecutors mTaskExecutors;

    @Before
    public void setUp() throws Exception {
        final HttpRequest notify = HttpRequest.create();
        notify.setMethod(Http.NOTIFY);
        notify.setUri("/");
        notify.setHeader(Http.CONNECTION, Http.CLOSE);
        notify.setHeader(Http.SEQ, "0");
        notify.setBody(TestUtils.getResourceAsString("propchange.xml"), true);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        notify.writeData(baos);
        mBadRequest = baos.toByteArray();
        baos.reset();

        notify.setHeader(Http.NT, Http.UPNP_EVENT);
        notify.setHeader(Http.NTS, Http.UPNP_PROPCHANGE);
        notify.writeData(baos);
        mFailRequest = baos.toByteArray();
        baos.reset();

        notify.setHeader(Http.SID, SID);
        notify.writeData(baos);
        mNotifyRequest = baos.toByteArray();
        mTaskExecutors = new TaskExecutors();
    }

    @After
    public void terminate() {
        mTaskExecutors.terminate();
    }

    @Test(timeout = 10000L)
    public void open_close_デッドロックしない() {
        final EventReceiver receiver = new EventReceiver(mTaskExecutors, null);
        receiver.start();
        receiver.stop();
    }

    @Test(timeout = 1000L)
    public void close_open前なら即終了() {
        final EventReceiver receiver = new EventReceiver(mTaskExecutors, null);
        receiver.stop();
    }

    @Test
    public void getLocalPort() throws Exception {
        final int port = 12345;
        final ServerSocket serverSocket = mock(ServerSocket.class);
        doReturn(port).when(serverSocket).getLocalPort();
        doThrow(new IOException()).when(serverSocket).accept();
        final EventReceiver receiver = spy(new EventReceiver(mTaskExecutors, null));
        doReturn(serverSocket).when(receiver).createServerSocket();

        receiver.start();
        assertThat(receiver.getLocalPort(), is(port));
        receiver.stop();
    }

    @Test
    public void getLocalPort_開始前は0() {
        final EventReceiver receiver = new EventReceiver(mTaskExecutors, null);
        assertThat(receiver.getLocalPort(), is(0));
    }

    private class Result {
        String sid;
        long seq;
        List<StringPair> properties;
    }

    @Test(timeout = 10000L)
    public void onEventReceived_イベントの値が取得できること() throws Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ByteArrayInputStream bais = new ByteArrayInputStream(mNotifyRequest);
        final Result result = new Result();
        final EventReceiver receiver = new EventReceiver(mTaskExecutors, (sid, seq, properties) -> {
            result.sid = sid;
            result.seq = seq;
            result.properties = properties;
            return true;
        }) {
            @Nonnull
            @Override
            ServerSocket createServerSocket() throws IOException {
                final Socket socket = mock(Socket.class);
                doReturn(bais).when(socket).getInputStream();
                doReturn(baos).when(socket).getOutputStream();
                final ServerSocket serverSocket = mock(ServerSocket.class);
                doAnswer(new Answer<Socket>() {
                    private int count;

                    @Override
                    public Socket answer(final InvocationOnMock invocation) throws Throwable {
                        if (count++ == 0) {
                            return socket;
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (final InterruptedException ignored) {
                        }
                        throw new IOException();
                    }
                }).when(serverSocket).accept();
                return serverSocket;
            }
        };
        receiver.start();
        Thread.sleep(100);
        receiver.stop();
        Thread.sleep(10);

        assertThat(result.sid, is(SID));
        assertThat(result.seq, is(0L));
        assertThat(result.properties, hasItem(new StringPair("SystemUpdateID", "0")));
        assertThat(result.properties, hasItem(new StringPair("ContainerUpdateIDs", "")));

        final HttpResponse response = HttpResponse.create();
        response.readData(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(response.getStatus(), is(Http.Status.HTTP_OK));
    }

    @Test(timeout = 10000L)
    public void onEventReceived_Failedが返る1() throws Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ByteArrayInputStream bais = new ByteArrayInputStream(mNotifyRequest);
        final EventReceiver receiver = new EventReceiver(mTaskExecutors, (sid, seq, properties) -> false) {
            @Nonnull
            @Override
            ServerSocket createServerSocket() throws IOException {
                final Socket socket = mock(Socket.class);
                doReturn(bais).when(socket).getInputStream();
                doReturn(baos).when(socket).getOutputStream();
                final ServerSocket serverSocket = mock(ServerSocket.class);
                doAnswer(new Answer<Socket>() {
                    private int count;

                    @Override
                    public Socket answer(final InvocationOnMock invocation) throws Throwable {
                        if (count++ == 0) {
                            return socket;
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (final InterruptedException ignored) {
                        }
                        throw new IOException();
                    }
                }).when(serverSocket).accept();
                return serverSocket;
            }
        };
        receiver.start();
        Thread.sleep(500);
        receiver.stop();
        Thread.sleep(100);

        final HttpResponse response = HttpResponse.create();
        response.readData(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(response.getStatus(), is(Http.Status.HTTP_PRECON_FAILED));
    }

    @Test(timeout = 10000L)
    public void onEventReceived_Failedが返る2() throws Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ByteArrayInputStream bais = new ByteArrayInputStream(mFailRequest);
        final EventReceiver receiver = new EventReceiver(mTaskExecutors, null) {
            @Nonnull
            @Override
            ServerSocket createServerSocket() throws IOException {
                final Socket socket = mock(Socket.class);
                doReturn(bais).when(socket).getInputStream();
                doReturn(baos).when(socket).getOutputStream();
                final ServerSocket serverSocket = mock(ServerSocket.class);
                doAnswer(new Answer<Socket>() {
                    private int count;

                    @Override
                    public Socket answer(final InvocationOnMock invocation) throws Throwable {
                        if (count++ == 0) {
                            return socket;
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (final InterruptedException ignored) {
                        }
                        throw new IOException();
                    }
                }).when(serverSocket).accept();
                return serverSocket;
            }
        };
        receiver.start();
        Thread.sleep(100);
        receiver.stop();
        Thread.sleep(10);

        final HttpResponse response = HttpResponse.create();
        response.readData(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(response.getStatus(), is(Http.Status.HTTP_PRECON_FAILED));
    }

    @Test(timeout = 10000L)
    public void onEventReceived_BadRequestが返る() throws Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ByteArrayInputStream bais = new ByteArrayInputStream(mBadRequest);
        final EventReceiver receiver = new EventReceiver(mTaskExecutors, null) {
            @Nonnull
            @Override
            ServerSocket createServerSocket() throws IOException {
                final Socket socket = mock(Socket.class);
                doReturn(bais).when(socket).getInputStream();
                doReturn(baos).when(socket).getOutputStream();
                final ServerSocket serverSocket = mock(ServerSocket.class);
                doAnswer(new Answer<Socket>() {
                    private int count;

                    @Override
                    public Socket answer(final InvocationOnMock invocation) throws Throwable {
                        if (count++ == 0) {
                            return socket;
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (final InterruptedException ignored) {
                        }
                        throw new IOException();
                    }
                }).when(serverSocket).accept();
                return serverSocket;
            }
        };
        receiver.start();
        Thread.sleep(100);
        receiver.stop();
        Thread.sleep(10);

        final HttpResponse response = HttpResponse.create();
        response.readData(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(response.getStatus(), is(Http.Status.HTTP_BAD_REQUEST));
    }

    @Test(timeout = 10000L)
    public void stop() throws Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ByteArrayInputStream bais = new ByteArrayInputStream(mNotifyRequest);
        final EventReceiver receiver = new EventReceiver(mTaskExecutors, (sid, seq, properties) -> {
            try {
                Thread.sleep(1000);
            } catch (final InterruptedException ignored) {
            }
            return false;
        }) {
            @Nonnull
            @Override
            ServerSocket createServerSocket() throws IOException {
                final Socket socket = mock(Socket.class);
                doReturn(bais).when(socket).getInputStream();
                doReturn(baos).when(socket).getOutputStream();
                final ServerSocket serverSocket = mock(ServerSocket.class);
                doAnswer(new Answer<Socket>() {
                    private int count;

                    @Override
                    public Socket answer(final InvocationOnMock invocation) throws Throwable {
                        if (count++ == 0) {
                            return socket;
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (final InterruptedException ignored) {
                        }
                        throw new IOException();
                    }
                }).when(serverSocket).accept();
                return serverSocket;
            }
        };
        receiver.start();
        Thread.sleep(100);
        receiver.stop();
        Thread.sleep(10);
    }

    @Test
    public void parsePropertyPairs_中身が空なら空のリスト() {
        final HttpRequest request = HttpRequest.create();

        assertThat(EventReceiver.parsePropertyPairs(request), empty());
    }

    @Test
    public void parsePropertyPairs_rootがpropertysetでない場合リスト() {
        final HttpRequest request = HttpRequest.create();
        request.setBody("<e:property xmlns:e=\"urn:schemas-upnp-org:event-1-0\">\n" +
                "<e:property>\n" +
                "<SystemUpdateID>0</SystemUpdateID>\n" +
                "</e:property>\n" +
                "<e:property>\n" +
                "<ContainerUpdateIDs></ContainerUpdateIDs>\n" +
                "</e:property>\n" +
                "</e:property>", true);

        assertThat(EventReceiver.parsePropertyPairs(request), empty());
    }

    @Test
    public void parsePropertyPairs_property以外の要素は無視() {
        final HttpRequest request = HttpRequest.create();
        request.setBody("<e:propertyset xmlns:e=\"urn:schemas-upnp-org:event-1-0\">\n" +
                "<e:property>\n" +
                "<SystemUpdateID>0</SystemUpdateID>\n" +
                "</e:property>\n" +
                "<e:proper>\n" +
                "<ContainerUpdateIDs></ContainerUpdateIDs>\n" +
                "</e:proper>\n" +
                "</e:propertyset>", true);

        assertThat(EventReceiver.parsePropertyPairs(request), hasSize(1));
    }

    @Test
    public void parsePropertyPairs_xml異常() {
        final HttpRequest request = HttpRequest.create();
        request.setBody("<e:propertyset xmlns:e=\"urn:schemas-upnp-org:event-1-0\">\n" +
                "<e:property>\n" +
                "<>0</>\n" +
                "</e:property>\n" +
                "<e:property>\n" +
                "<ContainerUpdateIDs></ContainerUpdateIDs>\n" +
                "</e:property>\n" +
                "</e:propertyset>", true);

        assertThat(EventReceiver.parsePropertyPairs(request), empty());
    }

    @Test
    public void ServerTask_notifyEvent_空ならfalse() {
        final String sid = "sid";
        final EventMessageListener listener = mock(EventMessageListener.class);
        final EventReceiver receiver = new EventReceiver(mTaskExecutors, listener);

        final HttpRequest request = HttpRequest.create();
        doReturn(true).when(listener).onEventReceived(anyString(), anyLong(), ArgumentMatchers.anyList());

        assertThat(receiver.notifyEvent(sid, request), is(false));
        verify(listener, never()).onEventReceived(anyString(), anyLong(), ArgumentMatchers.anyList());
    }

    @Test
    public void ServerTask_notifyEvent_listenerの戻り値と等しい() throws Exception {
        final String sid = "sid";
        final EventMessageListener listener = mock(EventMessageListener.class);
        final EventReceiver receiver = new EventReceiver(mTaskExecutors, listener);

        final HttpRequest request = HttpRequest.create();
        request.setHeader(Http.SEQ, "0");
        request.setBody(TestUtils.getResourceAsString("propchange.xml"), true);

        doReturn(true).when(listener).onEventReceived(anyString(), anyLong(), ArgumentMatchers.anyList());

        assertThat(receiver.notifyEvent(sid, request), is(true));
        verify(listener, times(1)).onEventReceived(anyString(), anyLong(), ArgumentMatchers.anyList());

        doReturn(false).when(listener).onEventReceived(anyString(), anyLong(), ArgumentMatchers.anyList());

        assertThat(receiver.notifyEvent(sid, request), is(false));
        verify(listener, times(2)).onEventReceived(anyString(), anyLong(), ArgumentMatchers.anyList());
    }

    @Test
    public void ServerTask_notifyEvent_listenerがなければfalse() throws Exception {
        final String sid = "sid";
        final EventMessageListener listener = mock(EventMessageListener.class);
        final EventReceiver receiver = new EventReceiver(mTaskExecutors, listener);

        final HttpRequest request = HttpRequest.create();
        request.setHeader(Http.SEQ, "0");
        request.setBody(TestUtils.getResourceAsString("propchange.xml"), true);

        assertThat(receiver.notifyEvent(sid, request), is(false));
    }

    @Test
    public void ClientTask_run() throws Exception {
        final Socket socket = mock(Socket.class);
        doReturn(mock(InputStream.class)).when(socket).getInputStream();
        doReturn(mock(OutputStream.class)).when(socket).getOutputStream();
        final EventReceiver eventReceiver = mock(EventReceiver.class);
        final ClientTask clientTask = spy(new ClientTask(eventReceiver, socket));
        doThrow(new IOException()).when(clientTask).receiveAndReply(ArgumentMatchers.any(InputStream.class), ArgumentMatchers.any(OutputStream.class));

        clientTask.run();

        verify(eventReceiver, times(1)).notifyClientFinished(clientTask);
    }
}
