/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * StateVariableを表現するクラス
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public interface StateVariable {
    /**
     * SendEventsの状態を返す。
     *
     * @return SendEventsがyesの場合true
     */
    boolean isSendEvents();

    /**
     * Multicastの状態を返す。
     *
     * @return Multicastがyesの場合true
     */
    boolean isMulticast();

    /**
     * StateVariable名を返す。
     *
     * @return StateVariable名
     */
    @Nonnull
    String getName();

    /**
     * DataTypeを返す。
     *
     * <p>UPnPでは以下のデータタイプが定義されている。
     * これらのどれにも当てはまらない場合もチェックは行われない。
     *
     * <p>UPnP
     * <table><caption>DataType一覧</caption>
     * <tr><th>ui1</th>
     * <td>Unsigned 1 Byte int. Same format as int without leading sign.</td></tr>
     * <tr><th>ui2</th>
     * <td>Unsigned 2 Byte int. Same format as int without leading sign.</td></tr>
     * <tr><th>ui4</th>
     * <td>Unsigned 4 Byte int. Same format as int without leading sign.</td></tr>
     * <tr><th>ui8</th>
     * <td>Unsigned 8 Byte int. Same format as int without leading sign.</td></tr>
     * <tr><th>i1</th>
     * <td>1 Byte int. Same format as int.</td></tr>
     * <tr><th>i2</th>
     * <td>2 Byte int. Same format as int.</td></tr>
     * <tr><th>i4</th>
     * <td>4 Byte int. Same format as int. shall be between -2147483648 and 2147483647.</td></tr>
     * <tr><th>i8</th>
     * <td>8 Byte int. Same format as int. shall be between -9,223,372,036,854,775,808 and
     * 9,223,372,036,854,775,807, from .(263) to 263 - 1.</td></tr>
     * <tr><th>int</th>
     * <td>Fixed point, integer number. Is allowed to have leading sign. Is allowed to have leading
     * zeros, which should be ignored by the recipient. (No currency symbol.)
     * (No grouping of digits to the left of the decimal, e.g., no commas.)</td></tr>
     * <tr><th>r4</th>
     * <td>4 Byte float. Same format as float. shall be between 3.40282347E+38 to 1.17549435E-38.</td></tr>
     * <tr><th>r8</th>
     * <td>8 Byte float. Same format as float. shall be between -1.79769313486232E308 and
     * -4.94065645841247E-324 for negative values, and between 4.94065645841247E-324 and
     * 1.79769313486232E308 for positive values, i.e., IEEE 64-bit (8-Byte) double.</td></tr>
     * <tr><th>number</th>
     * <td>Same as r8.</td></tr>
     * <tr><th>fixed.14.4</th>
     * <td>Same as r8 but no more than 14 digits to the left of the decimal point and no more than
     * 4 to the right.</td></tr>
     * <tr><th>float</th>
     * <td>Floating point number. Mantissa (left of the decimal) and/or exponent is allowed to have
     * a leading sign. Mantissa and/or exponent Is allowed to have leading zeros, which should be
     * ignored by the recipient. Decimal character in mantissa is a period, i.e., whole digits in
     * mantissa separated from fractional digits by period ("."). Mantissa separated from exponent
     * by "E". (No currency symbol.) (No grouping of digits in the mantissa, e.g., no commas.)</td></tr>
     * <tr><th>char</th>
     * <td>Unicode string. One character long.</td></tr>
     * <tr><th>string</th>
     * <td>Unicode string. No limit on length.</td></tr>
     * <tr><th>date</th>
     * <td>Date in a subset of ISO 8601 format without time data.</td></tr>
     * <tr><th>dateTime</th>
     * <td>Date in ISO 8601 format with allowed time but no time zone.</td></tr>
     * <tr><th>dateTime.tz</th>
     * <td>Date in ISO 8601 format with allowed time and allowed time zone.</td></tr>
     * <tr><th>time</th>
     * <td>Time in a subset of ISO 8601 format with no date and no time zone.</td></tr>
     * <tr><th>time.tz</th>
     * <td>Time in a subset of ISO 8601 format with allowed time zone but no date.</td></tr>
     * <tr><th>boolean</th>
     * <td>"0" for false or "1" for true. The values "true", "yes", "false", or "no" are deprecated
     * and shall not be sent but shall be accepted when received. When received, the values "true"
     * and "yes" shall be interpreted as true and the values "false" and "no" shall be interpreted as false.</td></tr>
     * <tr><th>bin.base64</th>
     * <td>MIME-style Base64 encoded binary BLOB. Takes 3 Bytes, splits them into 4 parts, and maps
     * each 6 bit piece to an octet. (3 octets are encoded as 4.) No limit on size.</td></tr>
     * <tr><th>bin.hex</th>
     * <td>Hexadecimal digits representing octets. Treats each nibble as a hex digit and encodes as
     * a separate Byte. (1 octet is encoded as 2.) No limit on size.</td></tr>
     * <tr><th>uri</th>
     * <td>Universal Resource Identifier.</td></tr>
     * <tr><th>uuid</th>
     * <td>Universally Unique ID. See clause 1.1.4, "UUID format and recommended generation
     * algorithms" for the MANDATORY UUID format.</td></tr>
     * </table>
     *
     * @return DataType
     */
    @Nonnull
    String getDataType();

    /**
     * AllowedValueListを返す。
     *
     * <p>リストは変更不可。
     *
     * @return AllowedValueList
     */
    @Nonnull
    List<String> getAllowedValueList();

    /**
     * DefaultValueの値を返す。
     *
     * @return DefaultValue
     */
    @Nullable
    String getDefaultValue();

    /**
     * Minimumの値を返す。
     *
     * @return Minimum
     */
    @Nullable
    String getMinimum();

    /**
     * Maximumの値を返す。
     *
     * @return Maximum
     */
    @Nullable
    String getMaximum();

    /**
     * Stepの値を返す。
     *
     * @return Stepの値
     */
    @Nullable
    String getStep();
}
