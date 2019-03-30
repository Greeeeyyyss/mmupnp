/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.thread;

import net.mm2d.upnp.TaskExecutor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(JUnit4.class)
public class DefaultTaskExecutorTest {
    @Test
    public void execute_executeが実行される() {
        final ExecutorService executorService = mock(ExecutorService.class);
        final TaskExecutor taskExecutor = new DefaultTaskExecutor(executorService);
        final Runnable command = mock(Runnable.class);

        assertThat(taskExecutor.execute(command), is(true));

        verify(executorService, times(1)).execute(command);
    }

    @Test
    public void execute_shutdown済みなら何もしないでfalse() {
        final ExecutorService executorService = mock(ExecutorService.class);
        final TaskExecutor taskExecutor = new DefaultTaskExecutor(executorService);
        final Runnable command = mock(Runnable.class);
        doReturn(true).when(executorService).isShutdown();

        assertThat(taskExecutor.execute(command), is(false));

        verify(executorService, never()).execute(command);
    }

    @Test
    public void execute_exceptionが発生すればfalse() {
        final ExecutorService executorService = mock(ExecutorService.class);
        final TaskExecutor taskExecutor = new DefaultTaskExecutor(executorService);
        final Runnable command = mock(Runnable.class);
        doThrow(new RejectedExecutionException()).when(executorService).execute(command);

        assertThat(taskExecutor.execute(command), is(false));
    }

    @Test
    public void execute_terminate後はfalse() {
        final ExecutorService executorService = mock(ExecutorService.class);
        final TaskExecutor taskExecutor = new DefaultTaskExecutor(executorService);
        final Runnable command = mock(Runnable.class);

        taskExecutor.terminate();
        assertThat(taskExecutor.execute(command), is(false));
    }

    @Test
    public void terminate_shutdownNowが実行される() {
        final ExecutorService executorService = mock(ExecutorService.class);
        final TaskExecutor taskExecutor = new DefaultTaskExecutor(executorService);

        taskExecutor.terminate();

        verify(executorService, times(1)).shutdownNow();
    }

    @Test
    public void terminate_2回コールできる() {
        final ExecutorService executorService = mock(ExecutorService.class);
        final TaskExecutor taskExecutor = new DefaultTaskExecutor(executorService);

        taskExecutor.terminate();
        taskExecutor.terminate();

        verify(executorService, times(1)).shutdownNow();
    }

    @Test
    public void terminate_terminate済みなら何もしない() {
        final ExecutorService executorService = mock(ExecutorService.class);
        final TaskExecutor taskExecutor = new DefaultTaskExecutor(executorService);
        doReturn(true).when(executorService).isShutdown();

        taskExecutor.terminate();

        verify(executorService, never()).shutdownNow();
    }

    @Test
    public void execute_io_executeが実行される() {
        final ExecutorService executorService = mock(ExecutorService.class);
        final TaskExecutor taskExecutor = new DefaultTaskExecutor(executorService, true);
        final Runnable command = mock(Runnable.class);

        assertThat(taskExecutor.execute(command), is(true));

        verify(executorService, times(1)).execute(command);
    }

    @Test
    public void execute_io_shutdown済みなら何もしないでfalse() {
        final ExecutorService executorService = mock(ExecutorService.class);
        final TaskExecutor taskExecutor = new DefaultTaskExecutor(executorService, true);
        final Runnable command = mock(Runnable.class);
        doReturn(true).when(executorService).isShutdown();

        assertThat(taskExecutor.execute(command), is(false));

        verify(executorService, never()).execute(command);
    }

    @Test
    public void execute_io_exceptionが発生すればfalse() {
        final ExecutorService executorService = mock(ExecutorService.class);
        final TaskExecutor taskExecutor = new DefaultTaskExecutor(executorService, true);
        final Runnable command = mock(Runnable.class);
        doThrow(new RejectedExecutionException()).when(executorService).execute(command);

        assertThat(taskExecutor.execute(command), is(false));
    }

    @Test
    public void execute_io_terminate後はfalse() {
        final ExecutorService executorService = mock(ExecutorService.class);
        final TaskExecutor taskExecutor = new DefaultTaskExecutor(executorService, true);
        final Runnable command = mock(Runnable.class);

        taskExecutor.terminate();
        assertThat(taskExecutor.execute(command), is(false));
    }

    @Test
    public void terminate_io_shutdownNowが実行される() throws Exception {
        final ExecutorService executorService = mock(ExecutorService.class);
        final TaskExecutor taskExecutor = new DefaultTaskExecutor(executorService, true);
        doReturn(true).when(executorService)
                .awaitTermination(1, TimeUnit.SECONDS);

        taskExecutor.terminate();

        verify(executorService, times(1)).shutdown();
    }

    @Test
    public void terminate_io_2回コールできる() throws Exception {
        final ExecutorService executorService = mock(ExecutorService.class);
        final TaskExecutor taskExecutor = new DefaultTaskExecutor(executorService, true);
        doReturn(true).when(executorService)
                .awaitTermination(1, TimeUnit.SECONDS);

        taskExecutor.terminate();
        taskExecutor.terminate();

        verify(executorService, times(1)).shutdown();
    }

    @Test
    public void terminate_io_awaitTerminationが割り込まれてもshutdownNowがコールされる() throws Exception {
        final ExecutorService executorService = mock(ExecutorService.class);
        final TaskExecutor taskExecutor = new DefaultTaskExecutor(executorService, true);
        doThrow(new InterruptedException()).when(executorService)
                .awaitTermination(1, TimeUnit.SECONDS);

        taskExecutor.terminate();

        verify(executorService, times(1)).shutdownNow();
    }

    @Test
    public void terminate_io_terminate済みなら何もしない() throws Exception {
        final ExecutorService executorService = mock(ExecutorService.class);
        final TaskExecutor taskExecutor = new DefaultTaskExecutor(executorService, true);
        doReturn(true).when(executorService)
                .awaitTermination(1, TimeUnit.SECONDS);
        doReturn(true).when(executorService).isShutdown();

        taskExecutor.terminate();

        verify(executorService, never()).shutdownNow();
    }

    @Test
    public void terminate_io_タイムアウトしたらshutdownNow() throws Exception {
        final ExecutorService executorService = mock(ExecutorService.class);
        final TaskExecutor taskExecutor = new DefaultTaskExecutor(executorService, true);
        doReturn(false).when(executorService)
                .awaitTermination(1, TimeUnit.SECONDS);

        taskExecutor.terminate();

        verify(executorService, times(1)).shutdown();
        verify(executorService, times(1)).shutdownNow();
    }
}