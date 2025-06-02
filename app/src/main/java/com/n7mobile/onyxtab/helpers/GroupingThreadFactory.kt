package com.n7mobile.onyxtab.helpers

import java.util.concurrent.ThreadFactory

/**
 * Creates threads that belong to a common thread group.
 */
class GroupingThreadFactory(private val group: ThreadGroup = Thread.currentThread().threadGroup!!) : ThreadFactory {

    constructor(groupName: String?) : this(ThreadGroup(groupName))

    override fun newThread(r: Runnable): Thread {
        return Thread(group, r)
    }
}