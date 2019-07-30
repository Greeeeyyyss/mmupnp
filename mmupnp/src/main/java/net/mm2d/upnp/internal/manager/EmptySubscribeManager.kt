package net.mm2d.upnp.internal.manager

import net.mm2d.upnp.Service

class EmptySubscribeManager : SubscribeManager {
    override fun getEventPort(): Int = 0
    override fun initialize() = Unit
    override fun start() = Unit
    override fun stop() = Unit
    override fun terminate() = Unit
    override fun getSubscribeService(subscriptionId: String): Service? = null
    override fun register(service: Service, timeout: Long, keep: Boolean) = Unit
    override fun renew(service: Service, timeout: Long) = Unit
    override fun setKeepRenew(service: Service, keep: Boolean) = Unit
    override fun unregister(service: Service) = Unit
}