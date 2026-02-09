package io.aimei.wk.benchmark

import io.aimei.wk.parser.SvgParser
import io.aimei.wk.parser.SvgPathParser
import kotlin.test.Test
import kotlin.system.measureNanoTime
import java.io.File

/**
 * Performance benchmarks for Vekt SVG parsing
 *
 * ============================================================================
 * BENCHMARK ENVIRONMENT
 * ============================================================================
 * - JVM: OpenJDK 17+ (recommended), tested on OpenJDK 21
 * - OS: macOS 14+ / Linux / Windows (tested on macOS 14.5 ARM64)
 * - Hardware: Apple M2 Pro (results may vary on different hardware)
 * - Warmup: 100 iterations (allows JIT optimization)
 * - Benchmark: 1000 iterations per test
 *
 * ============================================================================
 * HOW TO RUN
 * ============================================================================
 * ./gradlew :vekt-core:jvmTest --tests "*.ParserBenchmark*"
 *
 * ============================================================================
 * TEST CASES - SYNTHETIC DATA
 * ============================================================================
 *
 * Path Parsing Tests:
 * - Simple path: "M10 20 L30 40 L50 20 Z" (4 commands: MoveTo, 2x LineTo, Close)
 * - Medium path: 2 subpaths with cubic curves, ~15 commands total
 * - Complex path: 10+ cubic curves with many decimal coordinates, ~20 commands
 *
 * SVG Document Parsing Tests:
 * - Simple SVG: 1 path element, basic viewBox (53 bytes)
 * - Medium SVG: Group with transform, rect, circle, ellipse, path (379 bytes)
 * - Complex SVG: Multiple paths with transforms, polygon, group (750 bytes)
 *
 * ============================================================================
 * TEST CASES - REAL WORLD SVG FILES
 * ============================================================================
 *
 * Real-world icon files from production use:
 * - boy.svg: 35KB, 23 paths, 23 transforms (character illustration)
 * - confetti-ball.svg: 43KB, 21 paths, 21 transforms (celebration icon)
 * - maps.svg: 56KB, 19 paths, 19 transforms (map marker icon)
 * - swimming-pool.svg: 55KB, 20 paths, 20 transforms (amenity icon)
 * - woman.svg: 36KB, 19 paths, 19 transforms (character illustration)
 *
 * ============================================================================
 */
class ParserBenchmark {

    companion object {
        private const val WARMUP_ITERATIONS = 100
        private const val BENCHMARK_ITERATIONS = 1000

        // ====================================================================
        // SYNTHETIC TEST DATA
        // ====================================================================

        /**
         * Simple path: 4 commands (M, L, L, Z)
         * Use case: Basic shapes, simple icons
         */
        private const val SIMPLE_PATH = "M10 20 L30 40 L50 20 Z"

        /**
         * Medium path: 2 subpaths with cubic bezier curves
         * Use case: iOS-style icons, material design icons
         * Commands: M, C (multiple), S, Z - approximately 15 commands total
         */
        private const val MEDIUM_PATH = """
            M256,48C141.31,48,48,141.31,48,256s93.31,208,208,208,208-93.31,208-208S370.69,48,256,48Z
            M256,144c-35.29,0-64,28.71-64,64s28.71,64,64,64,64-28.71,64-64S291.29,144,256,144Z
        """

        /**
         * Complex path: Many cubic bezier curves with high-precision coordinates
         * Use case: Detailed illustrations, complex graphics
         * Commands: M, C (10+), L, Z - approximately 20 commands with 8+ decimal places
         */
        private const val COMPLEX_PATH = """
            M0 0 C2.81878924 0.6052425 5.05333422 1.45230596 7.546875 2.890625
            C31.85505721 16.54304601 53.27275202 18.55094103 79.875 11.1875
            C97.7583287 6.33010829 115.10297265 2.40184451 133.75 2.625
            C134.99716797 2.63442627 136.24433594 2.64385254 137.52929688 2.65356445
            C160.15976339 3.0247254 180.89261367 10.10101442 197.25390625 26.1328125
            C211.29174786 40.789677 224.26080176 65.25924224 225.125 86
            C225.08375 86.66 225.0425 87.32 225 88
            C226.04414062 88.12568359 226.04414062 88.12568359 227.109375 88.25390625
            C237.91980135 91.04416494 248.01903723 101.6625929 253.7421875 110.7734375
            Z
        """

        /**
         * Simple SVG: Minimal document with 1 path
         * Size: ~53 bytes, 1 element
         */
        private val SIMPLE_SVG = """
            <svg width="100" height="100" viewBox="0 0 100 100">
                <path d="M0 0 L100 100" fill="#FF0000"/>
            </svg>
        """.trimIndent()

        /**
         * Medium SVG: Group with transform, mixed shapes
         * Size: ~379 bytes, 5 elements (1 group with 3 shapes + 1 path)
         */
        private val MEDIUM_SVG = """
            <svg width="512" height="512" viewBox="0 0 512 512">
                <g transform="translate(50, 50)">
                    <rect x="0" y="0" width="100" height="100" fill="#FF0000"/>
                    <circle cx="200" cy="50" r="40" fill="#00FF00"/>
                    <ellipse cx="350" cy="50" rx="50" ry="30" fill="#0000FF"/>
                </g>
                <path d="M256,200 C256,150 306,150 306,200 C306,250 256,300 256,250 Z" fill="#FF00FF"/>
            </svg>
        """.trimIndent()

        /**
         * Complex SVG: Multiple paths with transforms, polygon
         * Size: ~750 bytes, 6 elements (2 paths with transforms, 1 group, 2 more paths, 1 polygon)
         */
        private val COMPLEX_SVG = """
            <svg width="512" height="512">
                <path d="M0 0 C2.81878924 0.6052425 5.05333422 1.45230596 7.546875 2.890625 C31.85505721 16.54304601 53.27275202 18.55094103 79.875 11.1875 Z" fill="#FEE2E0" transform="translate(127,8)"/>
                <path d="M0 0 C1.45703125 1.49023438 1.45703125 1.49023438 2.8125 3.59375 C3.32425781 4.36847656 3.83601563 5.14320313 4.36328125 5.94140625 Z" fill="#B3E97F" transform="translate(201,360)"/>
                <g fill="red" transform="translate(100, 100)">
                    <rect x="0" y="0" width="50" height="50"/>
                    <circle cx="100" cy="25" r="20"/>
                </g>
                <path d="M0 0 C2.81878924 0.6052425 5.05333422 1.45230596 7.546875 2.890625 Z" fill="#756663" transform="translate(127,8)"/>
                <polygon points="100,10 40,198 190,78 10,78 160,198" fill="#FFFF00"/>
            </svg>
        """.trimIndent()
    }

    // ========================================================================
    // PATH PARSING BENCHMARKS (Synthetic Data)
    // ========================================================================

    @Test
    fun benchmarkSimplePath() {
        println("\n" + "=".repeat(60))
        println("Simple Path Parsing Benchmark")
        println("=".repeat(60))
        println("Input: \"$SIMPLE_PATH\"")
        println("Commands: 4 (M, L, L, Z)")
        println("Use case: Basic shapes, simple arrows")
        println("-".repeat(60))
        runBenchmark("Simple path") {
            SvgPathParser.parse(SIMPLE_PATH)
        }
    }

    @Test
    fun benchmarkMediumPath() {
        println("\n" + "=".repeat(60))
        println("Medium Path Parsing Benchmark")
        println("=".repeat(60))
        println("Input: 2 subpaths with cubic bezier curves")
        println("Commands: ~15 (M, C, S, Z)")
        println("Use case: iOS-style icons, material design")
        println("-".repeat(60))
        runBenchmark("Medium path") {
            SvgPathParser.parse(MEDIUM_PATH)
        }
    }

    @Test
    fun benchmarkComplexPath() {
        println("\n" + "=".repeat(60))
        println("Complex Path Parsing Benchmark")
        println("=".repeat(60))
        println("Input: 10+ cubic curves with 8-decimal precision")
        println("Commands: ~20 (M, C, L, Z)")
        println("Use case: Detailed illustrations, complex icons")
        println("-".repeat(60))
        runBenchmark("Complex path") {
            SvgPathParser.parse(COMPLEX_PATH)
        }
    }

    @Test
    fun benchmarkPathWithScale() {
        println("\n" + "=".repeat(60))
        println("Path Parsing with Scale/Offset Benchmark")
        println("=".repeat(60))
        println("Input: Complex path with scale=2x, offset=(10,10)")
        println("Tests: Coordinate transformation during parsing")
        println("-".repeat(60))
        runBenchmark("Path with scale") {
            SvgPathParser.parse(COMPLEX_PATH, scale = 2f, offsetX = 10f, offsetY = 10f)
        }
    }

    // ========================================================================
    // SVG DOCUMENT PARSING BENCHMARKS (Synthetic Data)
    // ========================================================================

    @Test
    fun benchmarkSimpleSvg() {
        println("\n" + "=".repeat(60))
        println("Simple SVG Document Parsing Benchmark")
        println("=".repeat(60))
        println("Input: ${SIMPLE_SVG.length} bytes")
        println("Elements: 1 path")
        println("Features: viewBox, fill color")
        println("-".repeat(60))
        runBenchmark("Simple SVG") {
            SvgParser.parse(SIMPLE_SVG)
        }
    }

    @Test
    fun benchmarkMediumSvg() {
        println("\n" + "=".repeat(60))
        println("Medium SVG Document Parsing Benchmark")
        println("=".repeat(60))
        println("Input: ${MEDIUM_SVG.length} bytes")
        println("Elements: 5 (1 group with rect/circle/ellipse + 1 path)")
        println("Features: viewBox, group transform, multiple shapes")
        println("-".repeat(60))
        runBenchmark("Medium SVG") {
            SvgParser.parse(MEDIUM_SVG)
        }
    }

    @Test
    fun benchmarkComplexSvg() {
        println("\n" + "=".repeat(60))
        println("Complex SVG Document Parsing Benchmark")
        println("=".repeat(60))
        println("Input: ${COMPLEX_SVG.length} bytes")
        println("Elements: 6 (paths with transforms, group, polygon)")
        println("Features: Multiple transforms, nested group, polygon")
        println("-".repeat(60))
        runBenchmark("Complex SVG") {
            SvgParser.parse(COMPLEX_SVG)
        }
    }

    // ========================================================================
    // REAL-WORLD SVG FILE BENCHMARKS
    // ========================================================================

    @Test
    fun benchmarkRealWorldBoySvg() {
        val content = loadResource("boy.svg")
        println("\n" + "=".repeat(60))
        println("Real-World SVG: boy.svg")
        println("=".repeat(60))
        println("File size: ${content.length} bytes (35KB)")
        println("Elements: 23 paths with 23 transforms")
        println("Type: Character illustration icon")
        println("-".repeat(60))
        runBenchmark("boy.svg") {
            SvgParser.parse(content)
        }
    }

    @Test
    fun benchmarkRealWorldConfettiBallSvg() {
        val content = loadResource("confetti-ball.svg")
        println("\n" + "=".repeat(60))
        println("Real-World SVG: confetti-ball.svg")
        println("=".repeat(60))
        println("File size: ${content.length} bytes (43KB)")
        println("Elements: 21 paths with 21 transforms")
        println("Type: Celebration/party icon")
        println("-".repeat(60))
        runBenchmark("confetti-ball.svg") {
            SvgParser.parse(content)
        }
    }

    @Test
    fun benchmarkRealWorldMapsSvg() {
        val content = loadResource("maps.svg")
        println("\n" + "=".repeat(60))
        println("Real-World SVG: maps.svg")
        println("=".repeat(60))
        println("File size: ${content.length} bytes (56KB)")
        println("Elements: 19 paths with 19 transforms")
        println("Type: Map marker/location icon")
        println("-".repeat(60))
        runBenchmark("maps.svg") {
            SvgParser.parse(content)
        }
    }

    @Test
    fun benchmarkRealWorldSwimmingPoolSvg() {
        val content = loadResource("swimming-pool.svg")
        println("\n" + "=".repeat(60))
        println("Real-World SVG: swimming-pool.svg")
        println("=".repeat(60))
        println("File size: ${content.length} bytes (55KB)")
        println("Elements: 20 paths with 20 transforms")
        println("Type: Amenity/facility icon")
        println("-".repeat(60))
        runBenchmark("swimming-pool.svg") {
            SvgParser.parse(content)
        }
    }

    @Test
    fun benchmarkRealWorldWomanSvg() {
        val content = loadResource("woman.svg")
        println("\n" + "=".repeat(60))
        println("Real-World SVG: woman.svg")
        println("=".repeat(60))
        println("File size: ${content.length} bytes (36KB)")
        println("Elements: 19 paths with 19 transforms")
        println("Type: Character illustration icon")
        println("-".repeat(60))
        runBenchmark("woman.svg") {
            SvgParser.parse(content)
        }
    }

    @Test
    fun benchmarkComprehensiveTestSvg() {
        val content = loadResource("comprehensive-test.svg")
        println("\n" + "=".repeat(60))
        println("Real-World SVG: comprehensive-test.svg")
        println("=".repeat(60))
        println("File size: ${content.length} bytes")
        println("Type: Comprehensive feature test file")
        println("-".repeat(60))
        runBenchmark("comprehensive-test.svg") {
            SvgParser.parse(content)
        }
    }

    // ========================================================================
    // SVG SAMPLE FILE BENCHMARKS
    // ========================================================================

    @Test
    fun benchmarkSampleCircleSvg() {
        val content = loadResource("circle.svg")
        println("\n" + "=".repeat(60))
        println("Sample SVG: circle.svg")
        println("=".repeat(60))
        println("File size: ${content.length} bytes")
        println("-".repeat(60))
        runBenchmark("circle.svg") {
            SvgParser.parse(content)
        }
    }

    @Test
    fun benchmarkSampleShapesSvg() {
        val content = loadResource("shapes.svg")
        println("\n" + "=".repeat(60))
        println("Sample SVG: shapes.svg")
        println("=".repeat(60))
        println("File size: ${content.length} bytes")
        println("-".repeat(60))
        runBenchmark("shapes.svg") {
            SvgParser.parse(content)
        }
    }

    @Test
    fun benchmarkSampleHeartSvg() {
        val content = loadResource("heart.svg")
        println("\n" + "=".repeat(60))
        println("Sample SVG: heart.svg")
        println("=".repeat(60))
        println("File size: ${content.length} bytes")
        println("-".repeat(60))
        runBenchmark("heart.svg") {
            SvgParser.parse(content)
        }
    }

    @Test
    fun benchmarkSampleLinearGradientSvg() {
        val content = loadResource("linear-gradient.svg")
        println("\n" + "=".repeat(60))
        println("Sample SVG: linear-gradient.svg")
        println("=".repeat(60))
        println("File size: ${content.length} bytes")
        println("-".repeat(60))
        runBenchmark("linear-gradient.svg") {
            SvgParser.parse(content)
        }
    }

    @Test
    fun benchmarkSampleRadialGradientSvg() {
        val content = loadResource("radial-gradient.svg")
        println("\n" + "=".repeat(60))
        println("Sample SVG: radial-gradient.svg")
        println("=".repeat(60))
        println("File size: ${content.length} bytes")
        println("-".repeat(60))
        runBenchmark("radial-gradient.svg") {
            SvgParser.parse(content)
        }
    }

    @Test
    fun benchmarkSampleTransformSvg() {
        val content = loadResource("transform.svg")
        println("\n" + "=".repeat(60))
        println("Sample SVG: transform.svg")
        println("=".repeat(60))
        println("File size: ${content.length} bytes")
        println("-".repeat(60))
        runBenchmark("transform.svg") {
            SvgParser.parse(content)
        }
    }

    @Test
    fun benchmarkSampleTextSvg() {
        val content = loadResource("text.svg")
        println("\n" + "=".repeat(60))
        println("Sample SVG: text.svg")
        println("=".repeat(60))
        println("File size: ${content.length} bytes")
        println("-".repeat(60))
        runBenchmark("text.svg") {
            SvgParser.parse(content)
        }
    }

    @Test
    fun benchmarkSampleClipPathSvg() {
        val content = loadResource("clip-path.svg")
        println("\n" + "=".repeat(60))
        println("Sample SVG: clip-path.svg")
        println("=".repeat(60))
        println("File size: ${content.length} bytes")
        println("-".repeat(60))
        runBenchmark("clip-path.svg") {
            SvgParser.parse(content)
        }
    }

    @Test
    fun benchmarkSampleSymbolUseSvg() {
        val content = loadResource("symbol-use.svg")
        println("\n" + "=".repeat(60))
        println("Sample SVG: symbol-use.svg")
        println("=".repeat(60))
        println("File size: ${content.length} bytes")
        println("-".repeat(60))
        runBenchmark("symbol-use.svg") {
            SvgParser.parse(content)
        }
    }

    @Test
    fun benchmarkSampleGearIconSvg() {
        val content = loadResource("gear-icon.svg")
        println("\n" + "=".repeat(60))
        println("Sample SVG: gear-icon.svg")
        println("=".repeat(60))
        println("File size: ${content.length} bytes")
        println("-".repeat(60))
        runBenchmark("gear-icon.svg") {
            SvgParser.parse(content)
        }
    }

    // ========================================================================
    // BENCHMARK UTILITIES
    // ========================================================================

    private fun loadResource(name: String): String {
        return javaClass.classLoader.getResourceAsStream(name)?.bufferedReader()?.readText()
            ?: throw IllegalStateException("Resource not found: $name")
    }

    private inline fun runBenchmark(name: String, crossinline block: () -> Unit) {
        // Warmup phase - allows JIT compiler to optimize
        repeat(WARMUP_ITERATIONS) { block() }

        // Benchmark phase
        val times = LongArray(BENCHMARK_ITERATIONS)
        repeat(BENCHMARK_ITERATIONS) { i ->
            times[i] = measureNanoTime { block() }
        }

        // Calculate statistics
        val avgNanos = times.average()
        val minNanos = times.minOrNull() ?: 0
        val maxNanos = times.maxOrNull() ?: 0
        val sortedTimes = times.sorted()
        val medianNanos = sortedTimes[BENCHMARK_ITERATIONS / 2]
        val p95Nanos = sortedTimes[(BENCHMARK_ITERATIONS * 0.95).toInt()]
        val p99Nanos = sortedTimes[(BENCHMARK_ITERATIONS * 0.99).toInt()]

        println("$name:")
        println("  Iterations: $BENCHMARK_ITERATIONS (after $WARMUP_ITERATIONS warmup)")
        println("  Average: ${formatNanos(avgNanos.toLong())}")
        println("  Median:  ${formatNanos(medianNanos)}")
        println("  P95:     ${formatNanos(p95Nanos)}")
        println("  P99:     ${formatNanos(p99Nanos)}")
        println("  Min:     ${formatNanos(minNanos)}")
        println("  Max:     ${formatNanos(maxNanos)}")
        println("  Ops/sec: ${(1_000_000_000.0 / avgNanos).toLong()}")
    }

    private fun formatNanos(nanos: Long): String {
        return when {
            nanos >= 1_000_000 -> String.format("%.3f ms", nanos / 1_000_000.0)
            nanos >= 1_000 -> String.format("%.3f µs", nanos / 1_000.0)
            else -> "$nanos ns"
        }
    }
}
