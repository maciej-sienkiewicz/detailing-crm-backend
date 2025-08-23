package com.carslab.crm.production.modules.stats.infrastructure.mapper

import com.carslab.crm.production.modules.stats.application.dto.TimeGranularity
import com.carslab.crm.production.modules.stats.domain.model.TimeSeriesData
import com.carslab.crm.production.modules.stats.infrastructure.dto.TimeSeriesProjection
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object TimeSeriesMapper {

    fun getDateTruncExpression(granularity: TimeGranularity): String {
        return when (granularity) {
            TimeGranularity.DAILY -> "DATE_TRUNC('day', v.start_date)"
            TimeGranularity.WEEKLY -> "DATE_TRUNC('week', v.start_date)"
            TimeGranularity.MONTHLY -> "DATE_TRUNC('month', v.start_date)"
            TimeGranularity.QUARTERLY -> "DATE_TRUNC('quarter', v.start_date)"
            TimeGranularity.YEARLY -> "DATE_TRUNC('year', v.start_date)"
        }
    }

    fun getDateFormatString(granularity: TimeGranularity): String {
        return when (granularity) {
            TimeGranularity.DAILY -> "YYYY-MM-DD"
            TimeGranularity.WEEKLY -> "IYYY-\"W\"IW"
            TimeGranularity.MONTHLY -> "YYYY-MM"
            TimeGranularity.QUARTERLY -> "YYYY-\"Q\"Q"
            TimeGranularity.YEARLY -> "YYYY"
        }
    }

    fun mapToTimeSeriesData(projection: TimeSeriesProjection, granularity: TimeGranularity): TimeSeriesData {
        val periodStr = projection.getPeriod()
        val (periodStart, periodEnd) = parsePeriod(periodStr, granularity)

        return TimeSeriesData(
            period = periodStr,
            periodStart = periodStart,
            periodEnd = periodEnd,
            orders = projection.getOrders(),
            revenue = projection.getRevenue()
        )
    }

    private fun parsePeriod(period: String, granularity: TimeGranularity): Pair<LocalDate, LocalDate> {
        return when (granularity) {
            TimeGranularity.DAILY -> {
                val date = LocalDate.parse(period)
                date to date
            }
            TimeGranularity.WEEKLY -> {
                val parts = period.split("-W")
                val year = parts[0].toInt()
                val week = parts[1].toInt()
                val startDate = LocalDate.ofYearDay(year, 1).plusWeeks((week - 1).toLong())
                startDate to startDate.plusDays(6)
            }
            TimeGranularity.MONTHLY -> {
                val date = LocalDate.parse("$period-01")
                date to date.withDayOfMonth(date.lengthOfMonth())
            }
            TimeGranularity.QUARTERLY -> {
                val parts = period.split("-Q")
                val year = parts[0].toInt()
                val quarter = parts[1].toInt()
                val startMonth = (quarter - 1) * 3 + 1
                val startDate = LocalDate.of(year, startMonth, 1)
                val endDate = startDate.plusMonths(2).withDayOfMonth(startDate.plusMonths(2).lengthOfMonth())
                startDate to endDate
            }
            TimeGranularity.YEARLY -> {
                val year = period.toInt()
                val startDate = LocalDate.of(year, 1, 1)
                val endDate = LocalDate.of(year, 12, 31)
                startDate to endDate
            }
        }
    }
}