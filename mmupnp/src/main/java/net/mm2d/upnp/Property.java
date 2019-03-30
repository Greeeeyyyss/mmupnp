/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * ライブラリのプロパティ情報を管理するクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
@SuppressWarnings("WeakerAccess")
public final class Property {
    /**
     * ライブラリバージョン
     */
    @Nonnull
    public static final String LIB_VERSION = "mmupnp/2.0.0";
    /**
     * OSバージョン
     */
    @Nonnull
    public static final String OS_VERSION;
    /**
     * UPnPバージョン
     */
    @Nonnull
    public static final String UPNP_VERSION = "UPnP/1.0";
    /**
     * ライブラリで使用するUserAgentの値
     */
    @Nonnull
    public static final String USER_AGENT_VALUE;
    /**
     * ライブラリで使用するServer名
     */
    @Nonnull
    public static final String SERVER_VALUE;
    /**
     * デフォルトタイムアウト値[ms]（30秒）
     *
     * <p>{@link java.net.Socket#setSoTimeout(int)}に渡すためint値で定義
     */
    public static final int DEFAULT_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(30);

    // SystemプロパティからOS名とバージョン番号を取得する
    static {
        final String os = System.getProperty("os.name").split(" ")[0];
        final String ver = System.getProperty("os.version");
        OS_VERSION = os + "/" + ver;
        USER_AGENT_VALUE = OS_VERSION + " " + UPNP_VERSION + " " + LIB_VERSION;
        SERVER_VALUE = OS_VERSION + " " + UPNP_VERSION + " " + LIB_VERSION;
    }

    // インスタンス化禁止
    private Property() {
    }
}
