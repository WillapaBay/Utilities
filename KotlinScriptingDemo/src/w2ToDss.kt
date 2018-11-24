import hec.gfx2d.G2dDialog
import hec.io.PairedDataContainer
import hec.io.TimeSeriesContainer
import hec.script.Plot
import java.io.File

data class MyDateTime(val year: Int, val month: Int, val dayOfMonth: Int,
                      val hour: Int, val minute: Int) {
    val Months = listOf("January", "February", "March", "April", "May",
            "June", "July", "August", "September", "October", "November", "December")
    fun getHecDateString() = String.format("%02d%3s%04d", dayOfMonth,
            Months.get(month-1).substring(0,3), year)
    fun getHecTimeString() = String.format("%02d%02d", hour, minute)
    fun getHecDateTimeString() = "${getHecDateString()} ${getHecTimeString()}"
    fun getExcelDateTimeString() = String.format("%02d/%02d/%04d %02d:%02d", month,
            dayOfMonth, year, hour, minute)
}

fun saveFigure(plot: G2dDialog?, imfile: String, imtype : String) {
    when (imtype.toLowerCase()) {
        "png" -> plot?.saveToPng("$imfile.png")
        "wmf" -> plot?.saveToMetafile("$imfile.wmf")
        "jpg" -> plot?.saveToJpeg("$imfile.jpg")
    }
}

fun plotTimeSeries(tsc: TimeSeriesContainer,
                   yLabel: String, plotTitle: String, legendLabel: String,
                   imageFilename: String,
                   ymin: Double = 0.0, ymax: Double = 1000.0,
                   autoScale: Boolean = true) {
    val plot = Plot.newPlot()
    // plot.setLocation(-10000,-10000) // Plot offscreen
    val layout = Plot.newPlotLayout()
    val topView = layout.addViewport()
    topView.addCurve("Y1", tsc)
    plot.configurePlotLayout(layout)

    // Display the plot, then customize
    plot.showPlot()
    plot.setPlotTitleVisible(true)
    plot.setPlotTitleText(plotTitle)

    // Customize
    val curve = plot.getCurve(tsc)
    curve.setLineColor("blue")
    val lineWidth = 1.5f
    curve.setLineWidth(lineWidth)
    plot.setLegendLabelText(tsc, legendLabel)
    val vp0 = plot.getViewport(0)

    // Set axis limits and labels
    val yaxisTop = vp0.getAxis("Y1")
    if (!autoScale) yaxisTop.setScaleLimits(ymin, ymax)
    yaxisTop.setLabel(yLabel)

    plot.setSize(800, 400)
    saveFigure(plot, imageFilename,"png")
}

fun plotPairedData(pdcList: List<PairedDataContainer>,
                   yLabel: String, plotTitle: String, legendLabels: List<String>,
                   imageFilename: String,
                   ymin: Double = 0.0, ymax: Double = 1000.0,
                   autoScale: Boolean = true) {
    val plot = Plot.newPlot()
    val layout = Plot.newPlotLayout()
    val topView = layout.addViewport()

    pdcList.forEach {
        topView.addCurve("Y1", it)
    }

    plot.configurePlotLayout(layout)

    // Display the plot, then customize
    plot.showPlot()
    plot.setPlotTitleVisible(true)
    plot.setPlotTitleText(plotTitle)

    // Customize
    pdcList.zip(legendLabels).forEach {
        //val curve = plot.getCurve(it.first)
        plot.setLegendLabelText(it.first, it.second)
    }
    // There appears to be a in DSSVue. The last label text in the list sets
    // both the first label in the legend as well as the last.
    // Therefore, we will set the first one again manually.
    plot.setLegendLabelText(pdcList.get(0), legendLabels.get(0))

    val vp0 = plot.getViewport(0)

    // Set axis limits and labels
    val yaxisTop = vp0.getAxis("Y1")
    if (!autoScale) yaxisTop.setScaleLimits(ymin, ymax)
    yaxisTop.setLabel(yLabel)

    plot.setSize(800, 400)
    saveFigure(plot, imageFilename,"png")
}

/**
 * Create DSS time series container, to store data
 */
fun createTimeSeriesContainer(Dates: List<Int>, Values: List<Double>,
                              units: String, type: String, timeInterval: Int,
                              A: String = "", B: String = "", C: String = "", D: String = "",
                              E: String = "", F: String = ""): TimeSeriesContainer {

    val dataType = when (type) {
        "INST-VAL" -> 1
        "PER-AVG"  -> 2
        "PER-CUM"  -> 3
        else       -> 0
    }
    val tsc = TimeSeriesContainer()
    tsc.times = Dates.toIntArray()
    tsc.values = Values.toDoubleArray()
    tsc.startTime = Dates.get(0)
    tsc.endTime = Dates.get(Dates.lastIndex)
    tsc.units = units
    tsc.type = type
    tsc.dataType = dataType
    tsc.interval = timeInterval
    tsc.watershed = A
    tsc.location = B
    tsc.parameter = C
    tsc.version = F
    tsc.numberValues = Values.size
    tsc.fullName = "/$A/$B/$C/$D/$E/$F/"

    return tsc
}

/**
 * Create DSS paired data container, to store data
 */
fun createPairedDataContainer(X: DoubleArray, Y: Array<DoubleArray>,
                              xunits: String = "", yunits: String = "",
                              A: String = "", B: String = "", C: Array<String>, D: String = "",
                              E: String = "", F: String = ""): PairedDataContainer {

    val x = listOf(listOf(5.2, 2.4))
    val pdc = PairedDataContainer()
    pdc.xOrdinates = X
    pdc.yOrdinates = Y
    pdc.numberCurves = Y.size
    pdc.numberOrdinates = X.size
    pdc.xunits = xunits
    pdc.yunits = yunits
    pdc.watershed = A
    pdc.location = B
    pdc.parameters = C
    pdc.version = F
    pdc.fullName = "/$A/$B/${C.get(0)}/$D/$E/$F/"
    pdc.labelsUsed = true
    pdc.labels = C

    return pdc
}

fun julianToDate(jday: Double, startYear: Int): MyDateTime {
    var isLeapYear = (startYear % 4 == 0)
    var dayOfMonth = 0
    var year = startYear
    var jdayInt = jday.toInt()
    var leapOffset = 0
    var month = 0

    // The Julian days in the W2 ASCII file may be referenced from an earlier year. For example,
    // the record may start at Julian day 367 with 2017 as the reference year. The following code
    // will compute the true start year as 2018 and the new start day as 367 - 365 = 2
    // (i.e., a start date of January 2, 2018).
    while (jdayInt >= 366) {
        if (!isLeapYear && jdayInt >= 366) {
            jdayInt = jdayInt - 365
            year = year + 1
            isLeapYear = (year % 4) == 0
        } else if (jdayInt >= 367) {
            jdayInt = jdayInt - 366
            year = year + 1
            isLeapYear = (year % 4) == 0
        }
    }

    leapOffset = if (isLeapYear) 1 else 0

    // Determine month
    month = when (jdayInt) {
        in 1..31                                  -> 1
        in 32..(59 + leapOffset)                  -> 2
        in (60 + leapOffset)..(90 + leapOffset)   -> 3
        in (91 + leapOffset)..(120 + leapOffset)  -> 4
        in (121 + leapOffset)..(151 + leapOffset) -> 5
        in (152 + leapOffset)..(181 + leapOffset) -> 6
        in (182 + leapOffset)..(212 + leapOffset) -> 7
        in (213 + leapOffset)..(243 + leapOffset) -> 8
        in (244 + leapOffset)..(273 + leapOffset) -> 9
        in (274 + leapOffset)..(304 + leapOffset) -> 10
        in (305 + leapOffset)..(334 + leapOffset) -> 11
        in (335 + leapOffset)..(365 + leapOffset) -> 12
        else -> throw Exception("Error computing the month.")
    }

    // Determine day of month
    dayOfMonth = when (month) {
        1  -> jdayInt
        2  -> jdayInt - 31
        3  -> jdayInt - 59  - leapOffset
        4  -> jdayInt - 90  - leapOffset
        5  -> jdayInt - 120 - leapOffset
        6  -> jdayInt - 151 - leapOffset
        7  -> jdayInt - 181 - leapOffset
        8  -> jdayInt - 212 - leapOffset
        9  -> jdayInt - 243 - leapOffset
        10 -> jdayInt - 273 - leapOffset
        11 -> jdayInt - 304 - leapOffset
        12 -> jdayInt - 334 - leapOffset
        else -> throw Exception("Error computing the day of the month.")
    }

    val decimalHour = (jday - jday.toInt()) * 24.0
    val hour = decimalHour.toInt()
    val minute = ((decimalHour - decimalHour.toInt()) * 60).toInt()

    return MyDateTime(year, month, dayOfMonth, hour, minute)
}

fun parseJulianDay(infile: String, nHeaderLines: Int): MutableList<Double> {
    val Jday = mutableListOf<Double>()
    val Lines = File(infile).readLines()
    Lines.subList(nHeaderLines, Lines.size).forEach { line ->
        // Split on whitespace, after removing commas and whitespace on each end
        val data = line.trim().replace(',',' ')
                .split("\\s+".toRegex()).map { it.trim() }
        val jday = data.get(0).toDouble()
        Jday.add(jday)
    }
    return Jday
}

fun diff(X: MutableList<Double>): MutableList<Double> {
    val diffArray = mutableListOf<Double>()
    for (i in 1..X.lastIndex) {
        val diff = X.get(i) - X.get(i - 1)
        diffArray.add(diff)
    }
    return diffArray
}

fun getDifferencePDC(infile: String, xunits: String, yunits: String,
                     A: String = "", B: String = "", C: String = "", D: String = "",
                     E: String = "", F: String = ""): PairedDataContainer {
    val Jday = parseJulianDay(infile, 3)
    val JdayDiff = diff(Jday)
    val pdc = createPairedDataContainer(Jday.subList(0, Jday.lastIndex).toDoubleArray(),
            arrayOf(JdayDiff.toDoubleArray()), xunits, yunits,
            A, B, arrayOf(C), D, E, F)
    return pdc
}
