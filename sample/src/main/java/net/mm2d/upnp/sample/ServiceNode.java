/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample;

import net.mm2d.upnp.Action;
import net.mm2d.upnp.Device;
import net.mm2d.upnp.Http;
import net.mm2d.upnp.Service;

import java.awt.Component;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class ServiceNode extends UpnpNode {
    private boolean mSubscribing;

    public ServiceNode(final Service service) {
        super(service);
        final List<Action> actions = service.getActionList();
        for (final Action action : actions) {
            add(new ActionNode(action));
        }
        add(new StateVariableListNode(service.getStateVariableList()));
    }

    @Override
    public Service getUserObject() {
        return (Service) super.getUserObject();
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
    public String toString() {
        return getUserObject().getServiceType();
    }

    @Override
    public void showContextMenu(
            final JFrame frame,
            final Component invoker,
            final int x,
            final int y) {
        final JPopupMenu menu = new JPopupMenu();
        final JMenuItem open = new JMenuItem("Open Service Description");
        open.addActionListener(e -> {
            final Service service = getUserObject();
            final Device device = service.getDevice();
            final String baseUrl = device.getBaseUrl();
            final String scpdUrl = service.getScpdUrl();
            final int scopeId = device.getScopeId();
            try {
                final URI uri = Http.makeAbsoluteUrl(baseUrl, scpdUrl, scopeId).toURI();
                Desktop.getDesktop().browse(uri);
            } catch (final IOException | URISyntaxException e1) {
                e1.printStackTrace();
            }
        });
        menu.add(open);
        if (mSubscribing) {
            final JMenuItem unsubscribe = new JMenuItem("Unsubscribe");
            unsubscribe.addActionListener(e -> getUserObject().unsubscribe(result -> mSubscribing = !result));
            menu.add(unsubscribe);
        } else {
            final JMenuItem subscribe = new JMenuItem("Subscribe");
            subscribe.addActionListener(e -> {
                getUserObject().subscribe(true, result -> mSubscribing = result);
            });
            menu.add(subscribe);
        }
        menu.show(invoker, x, y);
    }

    public boolean isSubscribing() {
        return mSubscribing;
    }
}
