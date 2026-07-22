package com.example.nava.playback

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

@UnstableApi
class PlaybackFftAnalyzer(
    private val onBandsAvailable: (FloatArray) -> Unit,
) : TeeAudioProcessor.AudioBufferSink {
    private val executor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "nava-fft").apply { isDaemon = true }
    }
    private val computationPending = AtomicBoolean(false)
    private val sampleWindow = FloatArray(FFT_SIZE)
    private val smoothedBands = FloatArray(BAND_COUNT)
    private var sampleWindowPosition = 0
    private var sampleRateHz = DEFAULT_SAMPLE_RATE
    private var channelCount = 2
    private var encoding = C.ENCODING_PCM_16BIT
    private var lastAnalysisTimeNs = 0L

    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        this.sampleRateHz = sampleRateHz
        this.channelCount = channelCount.coerceAtLeast(1)
        this.encoding = encoding
        sampleWindowPosition = 0
        sampleWindow.fill(0f)
    }

    override fun handleBuffer(buffer: ByteBuffer) {
        val bytesPerSample = bytesPerSample(encoding) ?: return
        val bytesPerFrame = bytesPerSample * channelCount
        val samples = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        while (samples.remaining() >= bytesPerFrame) {
            var monoSample = 0f
            repeat(channelCount) {
                monoSample += readSample(samples, encoding)
            }
            sampleWindow[sampleWindowPosition++] = monoSample / channelCount
            if (sampleWindowPosition == FFT_SIZE) {
                submitWindowIfReady()
                sampleWindowPosition = 0
            }
        }
    }

    fun release() {
        executor.shutdownNow()
    }

    private fun submitWindowIfReady() {
        val now = System.nanoTime()
        if (now - lastAnalysisTimeNs < ANALYSIS_INTERVAL_NS || !computationPending.compareAndSet(false, true)) return
        lastAnalysisTimeNs = now
        val samples = sampleWindow.copyOf()
        val analysisSampleRate = sampleRateHz
        executor.execute {
            try {
                onBandsAvailable(computeBands(samples, analysisSampleRate))
            } finally {
                computationPending.set(false)
            }
        }
    }

    private fun computeBands(samples: FloatArray, sampleRateHz: Int): FloatArray {
        val real = DoubleArray(FFT_SIZE)
        val imaginary = DoubleArray(FFT_SIZE)
        samples.indices.forEach { index ->
            val hann = .5 - .5 * cos(2.0 * PI * index / (FFT_SIZE - 1))
            real[index] = samples[index] * hann
        }
        fft(real, imaginary)
        val nyquist = sampleRateHz / 2.0
        val upperFrequency = min(MAX_FREQUENCY_HZ, nyquist)
        val frequencyRange = upperFrequency / MIN_FREQUENCY_HZ
        return FloatArray(BAND_COUNT) { band ->
            val lowFrequency = MIN_FREQUENCY_HZ * frequencyRange.pow(band.toDouble() / BAND_COUNT)
            val highFrequency = MIN_FREQUENCY_HZ * frequencyRange.pow((band + 1.0) / BAND_COUNT)
            val lowBin = max(1, (lowFrequency * FFT_SIZE / sampleRateHz).toInt())
            val highBin = min(FFT_SIZE / 2 - 1, max(lowBin, (highFrequency * FFT_SIZE / sampleRateHz).toInt()))
            var energy = 0.0
            var binCount = 0
            for (bin in lowBin..highBin) {
                val magnitude = hypot(real[bin], imaginary[bin]) / (FFT_SIZE / 2.0)
                energy += magnitude * magnitude
                binCount++
            }
            val rms = sqrt(energy / binCount.coerceAtLeast(1))
            val decibels = 20.0 * log10(rms.coerceAtLeast(MIN_MAGNITUDE))
            val target = ((decibels - MIN_DECIBELS) / -MIN_DECIBELS).coerceIn(0.0, 1.0).toFloat()
            val smoothing = if (target > smoothedBands[band]) ATTACK_SMOOTHING else RELEASE_SMOOTHING
            smoothedBands[band] += (target - smoothedBands[band]) * smoothing
            smoothedBands[band]
        }
    }

    private fun fft(real: DoubleArray, imaginary: DoubleArray) {
        var reversedIndex = 0
        for (index in 1 until FFT_SIZE) {
            var bit = FFT_SIZE shr 1
            while (reversedIndex and bit != 0) {
                reversedIndex = reversedIndex xor bit
                bit = bit shr 1
            }
            reversedIndex = reversedIndex xor bit
            if (index < reversedIndex) {
                val realValue = real[index]
                real[index] = real[reversedIndex]
                real[reversedIndex] = realValue
                val imaginaryValue = imaginary[index]
                imaginary[index] = imaginary[reversedIndex]
                imaginary[reversedIndex] = imaginaryValue
            }
        }
        var length = 2
        while (length <= FFT_SIZE) {
            val angle = -2.0 * PI / length
            val baseReal = cos(angle)
            val baseImaginary = kotlin.math.sin(angle)
            var start = 0
            while (start < FFT_SIZE) {
                var twiddleReal = 1.0
                var twiddleImaginary = 0.0
                for (offset in 0 until length / 2) {
                    val evenIndex = start + offset
                    val oddIndex = evenIndex + length / 2
                    val oddReal = real[oddIndex] * twiddleReal - imaginary[oddIndex] * twiddleImaginary
                    val oddImaginary = real[oddIndex] * twiddleImaginary + imaginary[oddIndex] * twiddleReal
                    real[oddIndex] = real[evenIndex] - oddReal
                    imaginary[oddIndex] = imaginary[evenIndex] - oddImaginary
                    real[evenIndex] += oddReal
                    imaginary[evenIndex] += oddImaginary
                    val nextTwiddleReal = twiddleReal * baseReal - twiddleImaginary * baseImaginary
                    twiddleImaginary = twiddleReal * baseImaginary + twiddleImaginary * baseReal
                    twiddleReal = nextTwiddleReal
                }
                start += length
            }
            length = length shl 1
        }
    }

    private fun readSample(buffer: ByteBuffer, encoding: Int): Float = when (encoding) {
        C.ENCODING_PCM_8BIT -> ((buffer.get().toInt() and 0xFF) - 128) / 128f
        C.ENCODING_PCM_16BIT -> buffer.short / 32768f
        C.ENCODING_PCM_24BIT -> {
            var value = (buffer.get().toInt() and 0xFF) or
                ((buffer.get().toInt() and 0xFF) shl 8) or
                ((buffer.get().toInt() and 0xFF) shl 16)
            if (value and 0x800000 != 0) value = value or -0x1000000
            value / 8388608f
        }
        C.ENCODING_PCM_32BIT -> (buffer.int / 2147483648.0).toFloat()
        C.ENCODING_PCM_FLOAT -> buffer.float.coerceIn(-1f, 1f)
        else -> 0f
    }

    private fun bytesPerSample(encoding: Int): Int? = when (encoding) {
        C.ENCODING_PCM_8BIT -> 1
        C.ENCODING_PCM_16BIT -> 2
        C.ENCODING_PCM_24BIT -> 3
        C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 4
        else -> null
    }

    private companion object {
        const val FFT_SIZE = 1024
        const val BAND_COUNT = 28
        const val DEFAULT_SAMPLE_RATE = 44_100
        const val MIN_FREQUENCY_HZ = 45.0
        const val MAX_FREQUENCY_HZ = 16_000.0
        const val MIN_DECIBELS = -72.0
        const val MIN_MAGNITUDE = 0.000001
        const val ATTACK_SMOOTHING = .56f
        const val RELEASE_SMOOTHING = .2f
        const val ANALYSIS_INTERVAL_NS = 45_000_000L
    }
}
