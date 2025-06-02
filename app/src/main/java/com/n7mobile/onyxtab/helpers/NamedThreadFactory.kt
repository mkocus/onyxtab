package com.n7mobile.onyxtab.helpers

import java.util.concurrent.ThreadFactory

/** Names every thread as [name]. */
class NamedThreadFactory(val name: String, delegate: ThreadFactory = GroupingThreadFactory(name))
    : ThreadFactoryDecorator(delegate) {

    override fun decorate(thread: Thread): Thread =
        thread.also { it.name = name }
}