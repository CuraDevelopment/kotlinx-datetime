/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:JvmMultifileClass
@file:JvmName("TimeZoneKt")

package kotlinx.datetime

import kotlinx.datetime.serializers.*
import kotlinx.serialization.Serializable
import java.time.DateTimeException
import java.time.ZoneId
import java.time.ZoneOffset as jtZoneOffset

@Serializable(with = TimeZoneSerializer::class)
public actual open class TimeZone internal constructor(internal val zoneId: ZoneId) {
    public actual val id: String get() = zoneId.id


    // experimental member-extensions
    public actual fun Instant.toLocalDateTime(): LocalDateTime = toLocalDateTime(this@TimeZone)
    public actual fun LocalDateTime.toInstant(): Instant = toInstant(this@TimeZone)

    override fun equals(other: Any?): Boolean =
            (this === other) || (other is TimeZone && this.zoneId == other.zoneId)

    override fun hashCode(): Int = zoneId.hashCode()

    override fun toString(): String = zoneId.toString()

    public actual companion object {
        public actual fun currentSystemDefault(): TimeZone = ZoneId.systemDefault().let(::TimeZone)
        public actual val UTC: TimeZone = jtZoneOffset.UTC.let(::TimeZone)

        public actual fun of(zoneId: String): TimeZone = try {
            val zone = ZoneId.of(zoneId)
            if (zone is jtZoneOffset) {
                FixedOffsetTimeZone(UtcOffset(zone))
            } else {
                TimeZone(zone)
            }
        } catch (e: Exception) {
            if (e is DateTimeException) throw IllegalTimeZoneException(e)
            throw e
        }

        public actual val availableZoneIds: Set<String> get() = ZoneId.getAvailableZoneIds()
    }
}

@Serializable(with = FixedOffsetTimeZoneSerializer::class)
public actual class FixedOffsetTimeZone
public actual constructor(public actual val utcOffset: UtcOffset): TimeZone(utcOffset.zoneOffset) {
    @Deprecated("Use utcOffset.totalSeconds", ReplaceWith("utcOffset.totalSeconds"))
    public actual val totalSeconds: Int get() = utcOffset.totalSeconds
}

public actual fun TimeZone.offsetAt(instant: Instant): UtcOffset =
        zoneId.rules.getOffset(instant.value).let(::UtcOffset)

public actual fun Instant.toLocalDateTime(timeZone: TimeZone): LocalDateTime = try {
    java.time.LocalDateTime.ofInstant(this.value, timeZone.zoneId).let(::LocalDateTime)
} catch (e: DateTimeException) {
    throw DateTimeArithmeticException(e)
}

public actual fun Instant.toLocalDateTime(utcOffset: UtcOffset): LocalDateTime = try {
    java.time.LocalDateTime.ofInstant(this.value, utcOffset.zoneOffset).let(::LocalDateTime)
} catch (e: DateTimeException) {
    throw DateTimeArithmeticException(e)
}


public actual fun LocalDateTime.toInstant(timeZone: TimeZone): Instant =
        this.value.atZone(timeZone.zoneId).toInstant().let(::Instant)

public actual fun LocalDateTime.toInstant(utcOffset: UtcOffset): Instant =
        this.value.toInstant(utcOffset.zoneOffset).let(::Instant)

public actual fun LocalDate.atStartOfDayIn(timeZone: TimeZone): Instant =
        this.value.atStartOfDay(timeZone.zoneId).toInstant().let(::Instant)