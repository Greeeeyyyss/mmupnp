/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl;

import net.mm2d.upnp.Action;
import net.mm2d.upnp.Device;
import net.mm2d.upnp.Http;
import net.mm2d.upnp.HttpClient;
import net.mm2d.upnp.Icon;
import net.mm2d.upnp.IconFilter;
import net.mm2d.upnp.Service;
import net.mm2d.upnp.SsdpMessage;
import net.mm2d.upnp.internal.manager.SubscribeManager;
import net.mm2d.upnp.internal.message.FakeSsdpMessage;
import net.mm2d.upnp.internal.message.SsdpRequest;
import net.mm2d.upnp.internal.parser.DeviceParser;
import net.mm2d.upnp.util.TestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(Enclosed.class)
public class DeviceTest {

    @RunWith(JUnit4.class)
    public static class DeviceParserによる生成からのテスト {
        private HttpClient mHttpClient;
        private SsdpMessage mSsdpMessage;
        private ControlPointImpl mControlPoint;
        private SubscribeManager mSubscribeManager;
        private DeviceImpl.Builder mBuilder;

        @Before
        public void setUp() throws Exception {
            mHttpClient = mock(HttpClient.class);
            doReturn(TestUtils.getResourceAsString("device.xml"))
                    .when(mHttpClient).downloadString(new URL("http://192.0.2.2:12345/device.xml"));
            doReturn(TestUtils.getResourceAsString("cds.xml"))
                    .when(mHttpClient).downloadString(new URL("http://192.0.2.2:12345/cds.xml"));
            doReturn(TestUtils.getResourceAsString("cms.xml"))
                    .when(mHttpClient).downloadString(new URL("http://192.0.2.2:12345/cms.xml"));
            doReturn(TestUtils.getResourceAsString("mmupnp.xml"))
                    .when(mHttpClient).downloadString(new URL("http://192.0.2.2:12345/mmupnp.xml"));
            doReturn(TestUtils.getResourceAsByteArray("icon/icon120.jpg"))
                    .when(mHttpClient).downloadBinary(new URL("http://192.0.2.2:12345/icon/icon120.jpg"));
            doReturn(TestUtils.getResourceAsByteArray("icon/icon48.jpg"))
                    .when(mHttpClient).downloadBinary(new URL("http://192.0.2.2:12345/icon/icon48.jpg"));
            doReturn(TestUtils.getResourceAsByteArray("icon/icon120.png"))
                    .when(mHttpClient).downloadBinary(new URL("http://192.0.2.2:12345/icon/icon120.png"));
            doReturn(TestUtils.getResourceAsByteArray("icon/icon48.png"))
                    .when(mHttpClient).downloadBinary(new URL("http://192.0.2.2:12345/icon/icon48.png"));
            final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");
            mSsdpMessage = SsdpRequest.create(mock(InetAddress.class), data, data.length);
            mControlPoint = mock(ControlPointImpl.class);
            mSubscribeManager = mock(SubscribeManager.class);
            mBuilder = new DeviceImpl.Builder(mControlPoint, mSubscribeManager, mSsdpMessage);
            DeviceParser.loadDescription(mHttpClient, mBuilder);
        }

        @Test
        public void loadIconBinary_NONE() {
            final Device device = mBuilder.build();
            device.loadIconBinary(mHttpClient, IconFilter.NONE);
            final List<Icon> list = device.getIconList();
            for (final Icon icon : list) {
                assertThat(icon.getBinary(), is(nullValue()));
            }
        }

        @Test
        public void loadIconBinary_ALL() {
            final Device device = mBuilder.build();
            device.loadIconBinary(mHttpClient, IconFilter.ALL);
            final List<Icon> list = device.getIconList();
            for (final Icon icon : list) {
                assertThat(icon.getBinary(), is(notNullValue()));
            }
        }

        @Test
        public void loadIconBinary_取得に問題があっても他のファイルは取得可能() throws Exception {
            doThrow(new IOException()).when(mHttpClient).downloadBinary(new URL("http://192.0.2.2:12345/icon/icon120.jpg"));
            final Device device = mBuilder.build();
            device.loadIconBinary(mHttpClient, IconFilter.ALL);
            final List<Icon> list = device.getIconList();
            for (final Icon icon : list) {
                if (icon.getUrl().equals("/icon/icon120.jpg")) {
                    assertThat(icon.getBinary(), is(nullValue()));
                } else {
                    assertThat(icon.getBinary(), is(notNullValue()));
                }
            }
        }

        @Test
        public void getControlPoint() {
            final Device device = mBuilder.build();
            assertThat(device.getControlPoint(), is(mControlPoint));
        }

        @Test
        public void updateSsdpMessage() throws Exception {
            final Device device = mBuilder.build();
            final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive1.bin");
            final SsdpMessage message = SsdpRequest.create(mock(InetAddress.class), data, data.length);
            device.updateSsdpMessage(message);

            assertThat(device.getSsdpMessage(), is(message));
        }

        @Test
        public void updateSsdpMessage_pinned() throws Exception {
            final SsdpMessage originalMessage = mock(FakeSsdpMessage.class);
            doReturn("location").when(originalMessage).getLocation();
            doReturn(true).when(originalMessage).isPinned();
            mBuilder.updateSsdpMessage(originalMessage);
            final Device device = mBuilder.build();
            final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive1.bin");
            final SsdpMessage message = spy(SsdpRequest.create(mock(InetAddress.class), data, data.length));
            doReturn("uuid:").when(message).getUuid();
            device.updateSsdpMessage(message);

            assertThat(device.getSsdpMessage(), is(originalMessage));
        }

        @Test(expected = IllegalArgumentException.class)
        public void updateSsdpMessage_uuid不一致() throws Exception {
            final Device device = mBuilder.build();
            final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive1.bin");
            final SsdpMessage message = spy(SsdpRequest.create(mock(InetAddress.class), data, data.length));
            doReturn("uuid:").when(message).getUuid();
            device.updateSsdpMessage(message);
        }

        @Test(expected = IllegalArgumentException.class)
        public void updateSsdpMessage_location不正() throws Exception {
            final Device device = mBuilder.build();
            final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive1.bin");
            final SsdpMessage message = spy(SsdpRequest.create(mock(InetAddress.class), data, data.length));
            doReturn(null).when(message).getLocation();
            device.updateSsdpMessage(message);
        }

        @Test
        public void getSsdpMessage() {
            final Device device = mBuilder.build();
            assertThat(device.getSsdpMessage(), is(mSsdpMessage));
        }

        @Test
        public void getExpireTime() {
            final Device device = mBuilder.build();
            assertThat(device.getExpireTime(), is(mSsdpMessage.getExpireTime()));
        }

        @Test
        public void getDescription() throws Exception {
            final Device device = mBuilder.build();
            assertThat(device.getDescription(), is(TestUtils.getResourceAsString("device.xml")));
        }

        @Test
        public void getBaseUrl_URLBaseがなければLocationの値() throws Exception {
            final String location = "http://10.0.0.1:1000/";
            final Device device = mBuilder.build();
            final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive2.bin");
            final SsdpRequest message = SsdpRequest.create(mock(InetAddress.class), data, data.length);
            message.setHeader(Http.LOCATION, location);
            message.updateLocation();
            device.updateSsdpMessage(message);

            assertThat(device.getBaseUrl(), is(location));
        }

        @Test
        public void getBaseUrl_URLBaseがあればURLBaseの値() throws Exception {
            final String location = "http://10.0.0.1:1000/";
            final String urlBase = "http://10.0.0.1:1001/";
            mBuilder.setUrlBase(urlBase);
            final Device device = mBuilder.build();
            final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive2.bin");
            final SsdpRequest message = SsdpRequest.create(mock(InetAddress.class), data, data.length);
            message.setHeader(Http.LOCATION, location);
            message.updateLocation();
            device.updateSsdpMessage(message);

            assertThat(device.getBaseUrl(), is(urlBase));
        }

        @Test
        public void getValue() {
            final Device device = mBuilder.build();
            assertThat(device.getValue("deviceType"),
                    is("urn:schemas-upnp-org:device:MediaServer:1"));
        }

        @Test
        public void getValue_存在しないタグはnull() {
            final Device device = mBuilder.build();
            assertThat(device.getValue("deviceType1"),
                    is(nullValue()));
        }

        @Test
        public void getValueWithNamespace() {
            final Device device = mBuilder.build();
            assertThat(device.getValueWithNamespace("urn:schemas-upnp-org:device-1-0", "deviceType"),
                    is("urn:schemas-upnp-org:device:MediaServer:1"));
        }

        @Test
        public void getValueWithNamespace_namespace間違ったらnull() {
            final Device device = mBuilder.build();
            assertThat(device.getValueWithNamespace("urn:schemas-upnp-org:device-1-1", "deviceType"),
                    is(nullValue()));
        }

        @Test
        public void getLocation() {
            final Device device = mBuilder.build();
            assertThat(device.getLocation(), is(mSsdpMessage.getLocation()));
        }

        @Test
        public void getIpAddress() {
            final Device device = mBuilder.build();
            assertThat(device.getIpAddress(), is("192.0.2.2"));
        }

        @Test
        public void getUdn() {
            final Device device = mBuilder.build();
            assertThat(device.getUdn(), is("uuid:01234567-89ab-cdef-0123-456789abcdef"));
        }

        @Test
        public void getDeviceType() {
            final Device device = mBuilder.build();
            assertThat(device.getDeviceType(), is("urn:schemas-upnp-org:device:MediaServer:1"));
        }

        @Test
        public void getFriendlyName() {
            final Device device = mBuilder.build();
            assertThat(device.getFriendlyName(), is("mmupnp"));
        }

        @Test
        public void getManufacture() {
            final Device device = mBuilder.build();
            assertThat(device.getManufacture(), is("mm2d.net"));
        }

        @Test
        public void getManufactureUrl() {
            final Device device = mBuilder.build();
            assertThat(device.getManufactureUrl(), is("http://www.mm2d.net/"));
        }

        @Test
        public void getModelName() {
            final Device device = mBuilder.build();
            assertThat(device.getModelName(), is("mmupnp"));
        }

        @Test
        public void getModelUrl() {
            final Device device = mBuilder.build();
            assertThat(device.getModelUrl(), is("http://www.mm2d.net/"));
        }

        @Test
        public void getModelDescription() {
            final Device device = mBuilder.build();
            assertThat(device.getModelDescription(), is("mmupnp test server"));
        }

        @Test
        public void getModelNumber() {
            final Device device = mBuilder.build();
            assertThat(device.getModelNumber(), is("ABCDEFG"));
        }

        @Test
        public void getSerialNumber() {
            final Device device = mBuilder.build();
            assertThat(device.getSerialNumber(), is("0123456789ABC"));
        }

        @Test
        public void getPresentationUrl() {
            final Device device = mBuilder.build();
            assertThat(device.getPresentationUrl(), is("http://192.0.2.2:12346/"));
        }

        @Test
        public void getIconList() {
            final Device device = mBuilder.build();
            final List<Icon> list = device.getIconList();
            for (final Icon icon : list) {
                assertThat(icon.getMimeType(), is(anyOf(is("image/jpeg"), is("image/png"))));
                assertThat(icon.getWidth(), is(anyOf(is(48), is(120))));
                assertThat(icon.getHeight(), is(anyOf(is(48), is(120))));
                assertThat(icon.getDepth(), is(24));
                assertThat(icon.getUrl(), is(anyOf(
                        is("/icon/icon120.jpg"),
                        is("/icon/icon48.jpg"),
                        is("/icon/icon120.png"),
                        is("/icon/icon48.png"))));
            }
        }

        @Test
        public void getServiceList() {
            final Device device = mBuilder.build();
            assertThat(device.getServiceList(), hasSize(3));
        }

        @Test
        public void findServiceById() {
            final Device device = mBuilder.build();
            final Service cds = device.findServiceById("urn:upnp-org:serviceId:ContentDirectory");

            assertThat(cds, is(notNullValue()));
            assertThat(cds.getDevice(), is(device));
        }

        @Test
        public void findServiceById_見つからなければnull() {
            final Device device = mBuilder.build();
            assertThat(device.findServiceById("urn:upnp-org:serviceId:ContentDirectory1"), is(nullValue()));
        }

        @Test
        public void findServiceByType() {
            final Device device = mBuilder.build();
            final Service cds = device.findServiceByType("urn:schemas-upnp-org:service:ContentDirectory:1");

            assertThat(cds, is(notNullValue()));
            assertThat(cds.getDevice(), is(device));
        }

        @Test
        public void findServiceByType_見つからなければnull() {
            final Device device = mBuilder.build();
            assertThat(device.findServiceByType("urn:schemas-upnp-org:service:ContentDirectory:11"), is(nullValue()));
        }

        @Test
        public void findAction() {
            final Device device = mBuilder.build();
            final Action browse = device.findAction("Browse");

            assertThat(browse, is(notNullValue()));
            assertThat(browse.getService().getDevice(), is(device));

            final Action hogehoge = device.findAction("hogehoge");

            assertThat(hogehoge, is(nullValue()));
        }

        @Test
        public void toString_Nullでない() {
            final Device device = mBuilder.build();
            assertThat(device.toString(), is(notNullValue()));
        }

        @Test
        public void hashCode_Exceptionが発生しない() {
            final Device device = mBuilder.build();
            device.hashCode();
        }

        @Test
        public void equals_null比較可能() {
            final Device device = mBuilder.build();
            assertThat(device.equals(null), is(false));
        }
    }

    @RunWith(JUnit4.class)
    public static class DeviceBuilderによる生成からのテスト {
        @Test
        public void getIpAddress_正常() {
            final SsdpMessage message = mock(SsdpMessage.class);
            doReturn("http://192.168.0.1/").when(message).getLocation();
            doReturn("uuid").when(message).getUuid();
            final Device device = new DeviceImpl.Builder(mock(ControlPointImpl.class), mock(SubscribeManager.class), message)
                    .setDescription("description")
                    .setUdn("uuid")
                    .setUpc("upc")
                    .setDeviceType("deviceType")
                    .setFriendlyName("friendlyName")
                    .setManufacture("manufacture")
                    .setModelName("modelName")
                    .build();
            assertThat(device.getIpAddress(), is("192.168.0.1"));
        }

        @Test
        public void getIpAddress_異常() {
            final SsdpMessage message = mock(SsdpMessage.class);
            doReturn("location").when(message).getLocation();
            doReturn("uuid").when(message).getUuid();
            final Device device = new DeviceImpl.Builder(mock(ControlPointImpl.class), mock(SubscribeManager.class), message)
                    .setDescription("description")
                    .setUdn("uuid")
                    .setUpc("upc")
                    .setDeviceType("deviceType")
                    .setFriendlyName("friendlyName")
                    .setManufacture("manufacture")
                    .setModelName("modelName")
                    .build();

            assertThat(device.getIpAddress(), is(""));
        }

        @Test
        public void loadIconBinary_何も起こらない() {
            final SsdpMessage message = mock(SsdpMessage.class);
            doReturn("http://192.168.0.1/").when(message).getLocation();
            doReturn("uuid").when(message).getUuid();
            final Device device = new DeviceImpl.Builder(mock(ControlPointImpl.class), mock(SubscribeManager.class), message)
                    .setDescription("description")
                    .setUdn("uuid")
                    .setUpc("upc")
                    .setDeviceType("deviceType")
                    .setFriendlyName("friendlyName")
                    .setManufacture("manufacture")
                    .setModelName("modelName")
                    .build();

            device.loadIconBinary(mock(HttpClient.class), IconFilter.ALL);
        }
    }
}
