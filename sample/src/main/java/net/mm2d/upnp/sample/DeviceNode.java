/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample;

import net.mm2d.upnp.Device;
import net.mm2d.upnp.Service;

import java.awt.Component;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class DeviceNode extends UpnpNode {
    public DeviceNode(final Device device) {
        super(device);
        for (final Service service : device.getServiceList()) {
            add(new ServiceNode(service));
        }
        for (final Device embeddedDevice : device.getDeviceList()) {
            add(new DeviceNode(embeddedDevice));
        }
    }

    @Override
    public Device getUserObject() {
        return (Device) super.getUserObject();
    }

    @Override
    public String toString() {
        return getUserObject().getFriendlyName() + " [" + getUserObject().getIpAddress() + "]";
    }

    @Override
    public String formatDescription() {
        return Formatter.format(getUserObject());
    }

    @Override
    public String getDetailXml() {
        return formatXml(getUserObject().getDescription());
    }

    @Override
    public void showContextMenu(
            final JFrame frame,
            final Component invoker,
            final int x,
            final int y) {
        final JPopupMenu menu = new JPopupMenu();
        final JMenuItem open = new JMenuItem("Open Device Description");
        open.addActionListener(e -> {
            final Device device = getUserObject();
            try {
                Desktop.getDesktop().browse(new URI(device.getLocation()));
            } catch (final IOException | URISyntaxException e1) {
                e1.printStackTrace();
            }
        });
        menu.add(open);
        menu.show(invoker, x, y);
    }
}
