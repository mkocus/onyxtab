package com.n7mobile.onyxtab.helpers

import java.util.concurrent.ThreadFactory

/**
 * Base class for ThreadFactory decorators.
 *
 * Make sure to provide both a constructor taking a delegate parameter and one using the default delegate,
 * where possible.
 */
abstract class ThreadFactoryDecorator constructor(private val delegate: ThreadFactory = GroupingThreadFactory()) : ThreadFactory {

    /** Uses a GroupingThreadFactory with a new ThreadGroup as the delegate.  */
    constructor(threadGroupName: String?) : this(GroupingThreadFactory(threadGroupName))

    /** Uses a GroupingThreadFactory with a new ThreadGroup as the delegate.  */
    constructor(threadGroup: ThreadGroup?) : this(GroupingThreadFactory(threadGroup!!))

    protected abstract fun decorate(thread: Thread): Thread

    final override fun newThread(r: Runnable): Thread {
        return decorate(delegate.newThread(r))
    }

    protected fun Thread.buildUpon(runnable: () -> Unit): Thread =
        buildUpon(Runnable(runnable))

    protected fun Thread.buildUpon(runnable: Runnable?): Thread =
        Thread(threadGroup, runnable, name).also {
            it.uncaughtExceptionHandler = uncaughtExceptionHandler
            it.priority = priority
            it.contextClassLoader = contextClassLoader
            it.isDaemon = isDaemon
        }
}