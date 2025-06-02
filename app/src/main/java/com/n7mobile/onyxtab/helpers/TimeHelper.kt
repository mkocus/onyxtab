package com.n7mobile.onyxtab.helpers

import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class TimeHelper {
    companion object {
        fun isNightTime(): Boolean {
            val nightStart = LocalTime.of(23, 0)
            val nightEnd = LocalTime.of(5, 0)

            // Get the current time in the local timezone
            val currentTime = ZonedDateTime.now(ZoneId.systemDefault()).toLocalTime()

            // Check if current time is within the night period
            return if (nightStart.isAfter(nightEnd)) {
                // When night starts in the evening and goes past midnight
                currentTime.isAfter(nightStart) || currentTime.isBefore(nightEnd)
            } else {
                // When night is within the same day (this case doesn't apply to night, but good to handle)
                currentTime.isAfter(nightStart) && currentTime.isBefore(nightEnd)
            }
        }

        fun isEveningTime(): Boolean {
            val eveningStart = LocalTime.of(20, 0)
            val eveningEnd = LocalTime.of(23, 0)

            // Get the current time in the local timezone
            val currentTime = ZonedDateTime.now(ZoneId.systemDefault()).toLocalTime()

            // Check if current time is within the night period
            return if (eveningStart.isAfter(eveningEnd)) {
                // When night starts in the evening and goes past midnight
                currentTime.isAfter(eveningStart) || currentTime.isBefore(eveningEnd)
            } else {
                // When night is within the same day (this case doesn't apply to night, but good to handle)
                currentTime.isAfter(eveningStart) && currentTime.isBefore(eveningEnd)
            }
        }

        fun isMorningTime(): Boolean {
            val morningStart = LocalTime.of(5, 0)
            val morningEnd = LocalTime.of(7, 0)

            // Get the current time in the local timezone
            val currentTime = ZonedDateTime.now(ZoneId.systemDefault()).toLocalTime()

            // Check if current time is within the night period
            return if (morningStart.isAfter(morningEnd)) {
                // When night starts in the evening and goes past midnight
                currentTime.isAfter(morningStart) || currentTime.isBefore(morningEnd)
            } else {
                // When night is within the same day (this case doesn't apply to night, but good to handle)
                currentTime.isAfter(morningStart) && currentTime.isBefore(morningEnd)
            }
        }
    }


}