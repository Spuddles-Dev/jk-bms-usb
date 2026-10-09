package com.horse.jk_bms.history

data class ChartSample(val timestamp: Long, val value: Float, val segment: Long = 0)

class ExtremaSampler(private val start: Long, private val end: Long, private val buckets: Int = 300) {
    private data class Bucket(var first: ChartSample, var last: ChartSample, var min: ChartSample, var max: ChartSample)
    private val values = linkedMapOf<Int, Bucket>()
    private var previous: Long? = null
    private var segment = 0L

    fun add(timestamp: Long, value: Float) {
        if (!value.isFinite()) {
            segment++
            previous = null
            return
        }
        if (previous?.let { timestamp - it > 2000 || timestamp < it } == true) segment++
        previous = timestamp
        val sample = ChartSample(timestamp, value, segment)
        val index = (((timestamp - start).coerceAtLeast(0) * buckets) / (end - start).coerceAtLeast(1))
            .coerceIn(0, (buckets - 1).toLong()).toInt()
        val bucket = values[index]
        if (bucket == null) values[index] = Bucket(sample, sample, sample, sample)
        else {
            bucket.last = sample
            if (value < bucket.min.value) bucket.min = sample
            if (value > bucket.max.value) bucket.max = sample
        }
    }

    fun samples(): List<ChartSample> = values.values.flatMap { listOf(it.first, it.min, it.max, it.last) }
        .distinct().sortedBy { it.timestamp }
}
