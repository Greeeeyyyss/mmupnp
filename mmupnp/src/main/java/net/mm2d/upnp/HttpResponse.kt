/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import net.mm2d.upnp.Http.Status
import net.mm2d.upnp.internal.message.HttpMessageDelegate
import net.mm2d.upnp.internal.message.HttpMessageDelegate.StartLineDelegate

/**
 * HTTPレスポンスメッセージを表現するクラス。
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
class HttpResponse
internal constructor(
    private val startLineDelegate: StartLine,
    private val delegate: HttpMessageDelegate
) : HttpMessage by delegate {

    internal data class StartLine(
        private var _status: Status = Status.HTTP_INVALID,
        private var _statusCode: Int = 0,
        var reasonPhrase: String = "",
        override var version: String = Http.DEFAULT_HTTP_VERSION
    ) : StartLineDelegate {
        var statusCode: Int
            get() = _statusCode
            set(code) {
                val status = Status.valueOf(code)
                if (status == Status.HTTP_INVALID) {
                    throw IllegalArgumentException("unexpected status code:$code")
                }
                _status = status
                _statusCode = code
                reasonPhrase = status.phrase
            }

        var status: Status
            get() = _status
            set(status) {
                _status = status
                _statusCode = status.code
                reasonPhrase = status.phrase
            }

        override var startLine: String
            get() = "$version $_statusCode $reasonPhrase"
            set(line) {
                val params = line.split(" ", limit = 3)
                if (params.size < 3) {
                    throw IllegalArgumentException()
                }
                version = params[0]
                statusCode = Integer.parseInt(params[1])
                reasonPhrase = params[2]
            }
    }

    /**
     * ステータスコードを返す
     *
     * @return ステータスコード
     * @see status
     */
    val statusCode: Int
        get() = startLineDelegate.statusCode

    /**
     * レスポンスフレーズを取得する
     *
     * @return レスポンスフレーズ
     * @see status
     */
    val reasonPhrase: String
        get() = startLineDelegate.reasonPhrase

    /**
     * ステータスを取得する。
     *
     * @return ステータス
     */
    val status: Status
        get() = startLineDelegate.status

    /**
     * ステータスコードを設定する。
     *
     * @param code ステータスコード
     * @see setStatus
     */
    fun setStatusCode(code: Int) {
        startLineDelegate.statusCode = code
    }

    /**
     * レスポンスフレーズを設定する。
     *
     * @param reasonPhrase レスポンスフレーズ
     * @see setStatus
     */
    fun setReasonPhrase(reasonPhrase: String) {
        startLineDelegate.reasonPhrase = reasonPhrase
    }

    /**
     * ステータスを設定する。
     *
     * @param status ステータス
     * @return HttpResponse
     */
    fun setStatus(status: Status) {
        startLineDelegate.status = status
    }

    override fun toString(): String {
        return delegate.toString()
    }

    companion object {
        @JvmStatic
        fun create(): HttpResponse {
            return StartLine().let {
                HttpResponse(it, HttpMessageDelegate(it))
            }
        }

        @JvmStatic
        fun copy(original: HttpResponse): HttpResponse {
            return original.startLineDelegate.copy().let {
                HttpResponse(it, HttpMessageDelegate(it, original.delegate))
            }
        }
    }
}