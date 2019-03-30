/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.manager;

import net.mm2d.upnp.ControlPoint.NotifyEventListener;
import net.mm2d.upnp.Service;
import net.mm2d.upnp.internal.impl.DiFactory;
import net.mm2d.upnp.internal.server.EventReceiver;
import net.mm2d.upnp.internal.server.EventReceiver.EventMessageListener;
import net.mm2d.upnp.internal.thread.TaskExecutors;
import net.mm2d.upnp.internal.util.StringPair;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentMatchers;

import java.util.Collections;

import javax.annotation.Nonnull;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class SubscribeManagerTest {
    @Test
    public void onEventReceived_has_no_service() {
        final SubscribeHolder holder = mock(SubscribeHolder.class);
        final TaskExecutors executors = mock(TaskExecutors.class);
        final NotifyEventListener listener = mock(NotifyEventListener.class);
        final DiFactory factory = new DiFactory() {
            @Nonnull
            @Override
            public SubscribeHolder createSubscribeHolder(
                    @Nonnull final TaskExecutors taskExecutors) {
                return holder;
            }
        };
        final SubscribeManager manager = new SubscribeManager(executors, listener, factory);
        assertThat(manager.onEventReceived("", 0, Collections.emptyList()), is(false));
    }

    @Test
    public void onEventReceived() {
        final SubscribeHolder holder = mock(SubscribeHolder.class);
        final TaskExecutors executors = new TaskExecutors();
        final NotifyEventListener listener = mock(NotifyEventListener.class);
        final DiFactory factory = new DiFactory() {
            @Nonnull
            @Override
            public SubscribeHolder createSubscribeHolder(
                    @Nonnull final TaskExecutors taskExecutors) {
                return holder;
            }
        };
        final SubscribeManager manager = new SubscribeManager(executors, listener, factory);
        final String sid = "sid";
        final Service service = mock(Service.class);
        doReturn(service).when(holder).getService(sid);

        assertThat(manager.onEventReceived(sid, 0, Collections.singletonList(new StringPair("", ""))), is(true));

        executors.terminate();
    }

    @Test
    public void initialize() {
        final SubscribeHolder holder = mock(SubscribeHolder.class);
        final TaskExecutors executors = mock(TaskExecutors.class);
        final NotifyEventListener listener = mock(NotifyEventListener.class);
        final DiFactory factory = new DiFactory() {
            @Nonnull
            @Override
            public SubscribeHolder createSubscribeHolder(
                    @Nonnull final TaskExecutors taskExecutors) {
                return holder;
            }
        };
        final SubscribeManager manager = new SubscribeManager(executors, listener, factory);
        manager.initialize();
        verify(holder).start();
    }

    @Test
    public void start() {
        final EventReceiver receiver = mock(EventReceiver.class);
        final TaskExecutors executors = mock(TaskExecutors.class);
        final NotifyEventListener listener = mock(NotifyEventListener.class);
        final DiFactory factory = new DiFactory() {
            @Nonnull
            @Override
            public EventReceiver createEventReceiver(
                    @Nonnull final TaskExecutors taskExecutors,
                    @Nonnull final EventMessageListener listener) {
                return receiver;
            }
        };
        final SubscribeManager manager = new SubscribeManager(executors, listener, factory);
        manager.start();
        verify(receiver).start();
    }

    @Test
    public void stop() {
        final SubscribeHolder holder = mock(SubscribeHolder.class);
        final EventReceiver receiver = mock(EventReceiver.class);
        final TaskExecutors executors = mock(TaskExecutors.class);
        final NotifyEventListener listener = mock(NotifyEventListener.class);
        final DiFactory factory = new DiFactory() {
            @Nonnull
            @Override
            public SubscribeHolder createSubscribeHolder(
                    @Nonnull final TaskExecutors taskExecutors) {
                return holder;
            }

            @Nonnull
            @Override
            public EventReceiver createEventReceiver(
                    @Nonnull final TaskExecutors taskExecutors,
                    @Nonnull final EventMessageListener listener) {
                return receiver;
            }
        };
        final SubscribeManager manager = new SubscribeManager(executors, listener, factory);
        doReturn(Collections.singletonList(mock(Service.class))).when(holder).getServiceList();
        manager.stop();

        verify(executors).io(ArgumentMatchers.any(Runnable.class));
        verify(holder).clear();
        verify(receiver).stop();
    }

    @Test
    public void terminate() {
        final SubscribeHolder holder = mock(SubscribeHolder.class);
        final TaskExecutors executors = mock(TaskExecutors.class);
        final NotifyEventListener listener = mock(NotifyEventListener.class);
        final DiFactory factory = new DiFactory() {
            @Nonnull
            @Override
            public SubscribeHolder createSubscribeHolder(
                    @Nonnull final TaskExecutors taskExecutors) {
                return holder;
            }
        };
        final SubscribeManager manager = new SubscribeManager(executors, listener, factory);
        manager.terminate();
        verify(holder).stop();
    }

    @Test
    public void getEventPort() {
        final EventReceiver receiver = mock(EventReceiver.class);
        final TaskExecutors executors = mock(TaskExecutors.class);
        final NotifyEventListener listener = mock(NotifyEventListener.class);
        final DiFactory factory = new DiFactory() {
            @Nonnull
            @Override
            public EventReceiver createEventReceiver(
                    @Nonnull final TaskExecutors taskExecutors,
                    @Nonnull final EventMessageListener listener) {
                return receiver;
            }
        };
        final SubscribeManager manager = new SubscribeManager(executors, listener, factory);
        final int port = 80;
        doReturn(port).when(receiver).getLocalPort();

        assertThat(manager.getEventPort(), is(port));

        verify(receiver).getLocalPort();
    }

    @Test
    public void getSubscribeService() {
        final SubscribeHolder holder = mock(SubscribeHolder.class);
        final TaskExecutors executors = mock(TaskExecutors.class);
        final NotifyEventListener listener = mock(NotifyEventListener.class);
        final DiFactory factory = new DiFactory() {
            @Nonnull
            @Override
            public SubscribeHolder createSubscribeHolder(
                    @Nonnull final TaskExecutors taskExecutors) {
                return holder;
            }
        };
        final SubscribeManager manager = new SubscribeManager(executors, listener, factory);
        final String id = "id";
        final Service service = mock(Service.class);
        doReturn(service).when(holder).getService(id);

        assertThat(manager.getSubscribeService(id), is(service));

        verify(holder).getService(id);
    }

    @Test
    public void register() {
        final SubscribeHolder holder = mock(SubscribeHolder.class);
        final TaskExecutors executors = mock(TaskExecutors.class);
        final NotifyEventListener listener = mock(NotifyEventListener.class);
        final DiFactory factory = new DiFactory() {
            @Nonnull
            @Override
            public SubscribeHolder createSubscribeHolder(
                    @Nonnull final TaskExecutors taskExecutors) {
                return holder;
            }
        };
        final SubscribeManager manager = new SubscribeManager(executors, listener, factory);
        final Service service = mock(Service.class);
        final long timeout = 1000L;

        manager.register(service, timeout, true);

        verify(holder).add(service, timeout, true);
    }

    @Test
    public void renew() {
        final TaskExecutors executors = mock(TaskExecutors.class);
        final NotifyEventListener listener = mock(NotifyEventListener.class);
        final DiFactory factory = new DiFactory();
        final SubscribeManager manager = new SubscribeManager(executors, listener, factory);
        final Service service = mock(Service.class);
        final String id = "id";
        doReturn(id).when(service).getSubscriptionId();
        final long timeout = 1000L;

        manager.renew(service, timeout);
        manager.setKeepRenew(service, false);
        manager.register(service, timeout, true);
        manager.renew(service, timeout);
        manager.setKeepRenew(service, false);
    }

    @Test
    public void unregister() {
        final SubscribeHolder holder = mock(SubscribeHolder.class);
        final TaskExecutors executors = mock(TaskExecutors.class);
        final NotifyEventListener listener = mock(NotifyEventListener.class);
        final DiFactory factory = new DiFactory() {
            @Nonnull
            @Override
            public SubscribeHolder createSubscribeHolder(
                    @Nonnull final TaskExecutors taskExecutors) {
                return holder;
            }
        };
        final SubscribeManager manager = new SubscribeManager(executors, listener, factory);
        final Service service = mock(Service.class);

        manager.unregister(service);

        verify(holder).remove(service);
    }
}
