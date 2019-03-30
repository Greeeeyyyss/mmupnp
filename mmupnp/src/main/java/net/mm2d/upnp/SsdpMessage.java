/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * SSDP(Simple Service Discovery Protocol)メッセージを表現するインターフェース。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public interface SsdpMessage {
    /**
     * M-SEARCHのリスエストメソッド
     */
    String M_SEARCH = "M-SEARCH";
    /**
     * NOTIFYのリクエストメソッド
     */
    String NOTIFY = "NOTIFY";
    /**
     * NTSの値：ssdp:alive
     */
    String SSDP_ALIVE = "ssdp:alive";
    /**
     * NTSの値：ssdp:byebye
     */
    String SSDP_BYEBYE = "ssdp:byebye";
    /**
     * NTSの値：ssdp:update
     */
    @SuppressWarnings("unused")
    String SSDP_UPDATE = "ssdp:update";
    /**
     * MANの値：ssdp:discover
     */
    String SSDP_DISCOVER = "\"ssdp:discover\"";

    /**
     * 固定用メッセージかを返す。
     *
     * @return 固定用メッセージの時true
     */
    boolean isPinned();

    /**
     * 受信したインターフェースのScopeIDを返す。
     *
     * @return ScopeID、設定されていない場合(IPv4含む)は0
     */
    int getScopeId();

    /**
     * このパケットを受信したインターフェースのアドレスを返す。
     *
     * @return このパケットを受信したインターフェースのアドレス
     */
    @Nullable
    InetAddress getLocalAddress();

    /**
     * ヘッダの値を返す。
     *
     * @param name ヘッダ名
     * @return 値
     */
    @Nullable
    String getHeader(@Nonnull String name);

    /**
     * ヘッダの値を設定する。
     *
     * @param name  ヘッダ名
     * @param value 値
     */
    void setHeader(
            @Nonnull String name,
            @Nonnull String value);

    /**
     * USNに記述されたUUIDを返す。
     *
     * @return UUID
     */
    @Nonnull
    String getUuid();

    /**
     * USNに記述されたTypeを返す。
     *
     * @return Type
     */
    @Nonnull
    String getType();

    /**
     * NTSフィールドの値を返す。
     *
     * @return NTSフィールドの値
     */
    @Nullable
    String getNts();

    /**
     * max-ageの値を返す。
     *
     * @return max-ageの値
     */
    int getMaxAge();

    /**
     * 有効期限が切れる時刻を返す。
     *
     * <p>受信時刻からmax-ageを加算した時刻
     *
     * @return 有効期限が切れる時刻
     */
    long getExpireTime();

    /**
     * Locationの値を返す。
     *
     * @return Locationの値
     */
    @Nullable
    String getLocation();

    /**
     * 指定されたOutputStreamにメッセージの内容を書き出す。
     *
     * @param os 出力先
     * @throws IOException 入出力エラー
     */
    void writeData(@Nonnull OutputStream os) throws IOException;
}
