/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message;

import net.mm2d.log.Logger;
import net.mm2d.upnp.Http;
import net.mm2d.upnp.HttpMessage;
import net.mm2d.upnp.util.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * HttpMessageの共通実装。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class HttpMessageDelegate implements HttpMessage {
    private static final int BUFFER_SIZE = 1500;
    private static final int DEFAULT_CHUNK_SIZE = 1024;
    private static final int CR = 0x0d;
    private static final int LF = 0x0a;
    private static final byte[] CRLF = new byte[]{(byte) CR, (byte) LF};
    private static final String EOL = "\r\n";
    private static final String CHARSET = "utf-8";

    public interface StartLineDelegate {
        @Nonnull
        String getVersion();

        void setVersion(@Nonnull String version);

        @Nonnull
        String getStartLine();

        void setStartLine(@Nonnull String line);
    }

    @Nonnull
    private final HttpHeaders mHeaders;
    @Nullable
    private byte[] mBodyBinary;
    @Nullable
    private String mBody;
    @Nonnull
    private final StartLineDelegate mStartLineDelegate;

    public HttpMessageDelegate(@Nonnull final StartLineDelegate startLine) {
        mStartLineDelegate = startLine;
        mHeaders = new HttpHeaders();
    }

    public HttpMessageDelegate(
            @Nonnull final StartLineDelegate startLine,
            @Nonnull final HttpMessageDelegate original) {
        mStartLineDelegate = startLine;
        mHeaders = new HttpHeaders(original.mHeaders);
        mBodyBinary = original.mBodyBinary == null ? null :
                Arrays.copyOf(original.mBodyBinary, original.mBodyBinary.length);
        mBody = original.mBody;
    }

    @Nullable
    @Override
    public String getStartLine() {
        return mStartLineDelegate.getStartLine();
    }

    @Override
    public void setStartLine(@Nonnull final String line) {
        mStartLineDelegate.setStartLine(line);
    }

    @Nonnull
    @Override
    public String getVersion() {
        return mStartLineDelegate.getVersion();
    }

    @Override
    public void setVersion(@Nonnull final String version) {
        mStartLineDelegate.setVersion(version);
    }

    @Override
    public void setHeader(
            @Nonnull final String name,
            @Nonnull final String value) {
        mHeaders.put(name, value);
    }

    @Override
    public void setHeaderLine(@Nonnull final String line) {
        final int pos = line.indexOf(':');
        if (pos < 0) {
            return;
        }
        final String name = line.substring(0, pos).trim();
        final String value = line.substring(pos + 1).trim();
        setHeader(name, value);
    }

    @Nullable
    @Override
    public String getHeader(@Nonnull final String name) {
        return mHeaders.get(name);
    }

    @Override
    public boolean isChunked() {
        return mHeaders.containsValue(Http.TRANSFER_ENCODING, Http.CHUNKED);
    }

    @Override
    public boolean isKeepAlive() {
        if (getVersion().equals(Http.HTTP_1_0)) {
            return mHeaders.containsValue(Http.CONNECTION, Http.KEEP_ALIVE);
        }
        return !mHeaders.containsValue(Http.CONNECTION, Http.CLOSE);
    }

    @Override
    public int getContentLength() {
        final String len = mHeaders.get(Http.CONTENT_LENGTH);
        if (len != null) {
            try {
                return Integer.parseInt(len);
            } catch (final NumberFormatException e) {
                Logger.w(e);
            }
        }
        return 0;
    }

    @Override
    public void setBody(@Nullable final String body) {
        setBody(body, false);
    }

    @Override
    public void setBody(
            @Nullable final String body,
            final boolean withContentLength) {
        setBodyInner(body, null, withContentLength);
    }

    @Override
    public void setBodyBinary(@Nullable final byte[] body) {
        setBodyBinary(body, false);
    }

    @Override
    public void setBodyBinary(
            @Nullable final byte[] body,
            final boolean withContentLength) {
        setBodyInner(null, body, withContentLength);
    }

    private void setBodyInner(
            @Nullable final String string,
            @Nullable final byte[] binary,
            final boolean withContentLength) {
        mBody = string;
        if (string == null) {
            mBodyBinary = binary;
        } else if (string.length() == 0) {
            mBodyBinary = new byte[0];
        } else {
            try {
                mBodyBinary = getBytes(string);
            } catch (final UnsupportedEncodingException e) {
                Logger.w(e);
            }
        }
        if (withContentLength) {
            final int length = mBodyBinary == null ? 0 : mBodyBinary.length;
            setHeader(Http.CONTENT_LENGTH, String.valueOf(length));
        }
    }

    // VisibleForTesting
    @Nonnull
    byte[] getBytes(@Nonnull final String string) throws UnsupportedEncodingException {
        return string.getBytes(CHARSET);
    }

    @Nullable
    @Override
    public String getBody() {
        if (mBody == null && mBodyBinary != null) {
            mBody = decode(mBodyBinary);
        }
        return mBody;
    }

    @Nullable
    private String decode(@Nonnull final byte[] binary) {
        if (binary.length == 0) {
            return "";
        }
        try {
            return newString(binary);
        } catch (final Exception e) {
            // for bug in Android Sdk, ArrayIndexOutOfBoundsException may occur.
            Logger.w(e);
        }
        return null;
    }

    // VisibleForTesting
    @Nonnull
    String newString(@Nonnull final byte[] binary) throws UnsupportedEncodingException {
        return new String(binary, CHARSET);
    }

    @Nullable
    @Override
    public byte[] getBodyBinary() {
        return mBodyBinary;
    }

    @Nonnull
    private String getHeaderString() {
        return getHeaderStringBuilder().toString();
    }

    private StringBuilder getHeaderStringBuilder() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getStartLine());
        sb.append(EOL);
        for (final HttpHeaders.Entry entry : mHeaders.values()) {
            sb.append(entry.getName());
            sb.append(": ");
            sb.append(entry.getValue());
            sb.append(EOL);
        }
        sb.append(EOL);
        return sb;
    }

    // VisibleForTesting
    @Nonnull
    byte[] getHeaderBytes() {
        try {
            return getBytes(getHeaderString());
        } catch (final UnsupportedEncodingException e) {
            Logger.w(e);
        }
        return new byte[0];
    }

    @Nonnull
    @Override
    public String getMessageString() {
        final StringBuilder sb = getHeaderStringBuilder();
        final String body = getBody();
        if (!TextUtils.isEmpty(body)) {
            sb.append(body);
        }
        return sb.toString();
    }

    @Override
    public void writeData(@Nonnull final OutputStream outputStream) throws IOException {
        outputStream.write(getHeaderBytes());
        if (mBodyBinary != null) {
            if (isChunked()) {
                writeChunkedBody(outputStream, mBodyBinary);
            } else {
                outputStream.write(mBodyBinary);
            }
        }
        outputStream.flush();
    }

    private void writeChunkedBody(
            @Nonnull final OutputStream outputStream,
            @Nonnull final byte[] binary)
            throws IOException {
        int offset = 0;
        while (offset < binary.length) {
            final int size = Math.min(DEFAULT_CHUNK_SIZE, binary.length - offset);
            writeChunkSize(outputStream, size);
            outputStream.write(binary, offset, size);
            outputStream.write(CRLF);
            offset += size;
        }
        writeChunkSize(outputStream, 0);
        outputStream.write(CRLF);
    }

    private void writeChunkSize(
            @Nonnull final OutputStream outputStream,
            final int size) throws IOException {
        outputStream.write(getBytes(Integer.toHexString(size)));
        outputStream.write(CRLF);
    }

    @Override
    public void readData(@Nonnull final InputStream inputStream) throws IOException {
        readStartLine(inputStream);
        readHeaders(inputStream);
        if (isChunked()) {
            readChunkedBody(inputStream);
        } else {
            readBody(inputStream);
        }
    }

    private void readStartLine(@Nonnull final InputStream inputStream) throws IOException {
        final String startLine = readLine(inputStream);
        if (TextUtils.isEmpty(startLine)) {
            throw new IOException("Illegal start line:" + startLine);
        }
        try {
            setStartLine(startLine);
        } catch (final IllegalArgumentException e) {
            throw new IOException("Illegal start line:" + startLine);
        }
    }

    private void readHeaders(@Nonnull final InputStream inputStream) throws IOException {
        while (true) {
            final String line = readLine(inputStream);
            if (line.isEmpty()) {
                break;
            }
            setHeaderLine(line);
        }
    }

    private void readBody(@Nonnull final InputStream inputStream) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        readInputStream(inputStream, baos, getContentLength());
        setBodyBinary(baos.toByteArray());
    }

    private void readChunkedBody(@Nonnull final InputStream inputStream) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (true) {
            final int length = readChunkSize(inputStream);
            if (length == 0) {
                readLine(inputStream);
                break;
            }
            readInputStream(inputStream, baos, length);
            readLine(inputStream);
        }
        setBodyBinary(baos.toByteArray());
    }

    private void readInputStream(
            @Nonnull final InputStream inputStream,
            @Nonnull final OutputStream outputStream,
            final int length) throws IOException {
        final byte[] buffer = new byte[BUFFER_SIZE];
        int remain = length;
        while (remain > 0) {
            final int stroke = remain > buffer.length ? buffer.length : remain;
            final int size = inputStream.read(buffer, 0, stroke);
            if (size < 0) {
                throw new IOException("can't read from InputStream");
            }
            outputStream.write(buffer, 0, size);
            remain -= size;
        }
    }

    private int readChunkSize(@Nonnull final InputStream inputStream) throws IOException {
        final String line = readLine(inputStream);
        if (TextUtils.isEmpty(line)) {
            throw new IOException("Can not read chunk size!");
        }
        final String chunkSize = line.split(";", 2)[0];
        try {
            return Integer.parseInt(chunkSize, 16);
        } catch (final NumberFormatException e) {
            throw new IOException("Chunk format error!", e);
        }
    }

    @Nonnull
    private static String readLine(@Nonnull final InputStream inputStream) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (true) {
            final int b = inputStream.read();
            if (b < 0) {
                if (baos.size() == 0) {
                    throw new IOException("can't read from InputStream");
                }
                break;
            }
            if (b == LF) {
                break;
            }
            if (b == CR) {
                continue;
            }
            baos.write(b);
        }
        return baos.toString(CHARSET);
    }

    @Override
    @Nonnull
    public String toString() {
        return getMessageString();
    }
}
