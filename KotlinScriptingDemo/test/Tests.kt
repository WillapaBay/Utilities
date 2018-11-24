import hec.heclib.util.HecTime
import hec.hecmath.HecMath
import hec.io.PairedDataContainer
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.stage.Stage
import org.junit.Test
import java.io.File

var Jday = mutableListOf<Double>()
var JdayDiff = mutableListOf<Double>()
var Values = mutableListOf<Double>()
val chartTitle = "DeGray Reservoir"
val xLabel = "Julian Day"
val yLabel = "Flow (cms)"
var legendLabels = listOf("Inflow (Qin)", "Inflow + 5", "Inflow + 10")
var legendLabel = ""

fun sleep(time: Long) {
    try {
        Thread.sleep(time)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

class Plot1 : Application() {
    override fun start(stage: Stage) {
        stage.title = chartTitle

        // Defining the axes
        val xAxis = NumberAxis()
        val yAxis = NumberAxis()
        xAxis.label = xLabel
        yAxis.label = yLabel
        xAxis.lowerBound = Jday.min()!!
        xAxis.upperBound = Jday.max()!!

        // Creating the chart
        val lineChart = LineChart(xAxis, yAxis)
        lineChart.title = chartTitle

        // Defining a series
        val seriesList: MutableList<XYChart.Series<Number, Number>> = mutableListOf()
        legendLabels.forEach { label ->
            val series = XYChart.Series<Number, Number>()
            series.name = label
            seriesList.add(series)
        }

        // Populating the series with data
        Jday.zip(Values).forEach {
            seriesList.get(0).data.add(XYChart.Data(it.first, it.second))
            seriesList.get(1).data.add(XYChart.Data(it.first, it.second + 5))
            seriesList.get(2).data.add(XYChart.Data(it.first, it.second + 10))
        }

        val scene = Scene(lineChart, 800.0, 600.0)
        seriesList.forEach {
            lineChart.data.add(it)
        }

        stage.scene = scene
        stage.show()
    }
}

class Plot2() : Application() {
    private var legendLabel2: String = ""

    constructor(legendLabel2: String) : this() {
        this.legendLabel2 = legendLabel2
    }

    override fun start(stage: Stage) {
        stage.title = chartTitle

        // Defining the axes
        val xAxis = NumberAxis()
        val yAxis = NumberAxis()
        xAxis.label = "Julian Day"
        yAxis.label = "Julian Day Difference"
        xAxis.isAutoRanging = false
        xAxis.lowerBound = Jday.min()!!
        xAxis.upperBound = Jday.max()!!
        yAxis.isAutoRanging = false
        yAxis.lowerBound = JdayDiff.min()!!
        yAxis.upperBound = JdayDiff.max()!!

        // Creating the chart
        val lineChart = LineChart(xAxis, yAxis)
        lineChart.title = "Julian Day Difference, DeGray Reservoir Inflow"
        lineChart.createSymbols = false

        // Defining a series
        val seriesList: MutableList<XYChart.Series<Number, Number>> = mutableListOf()
        val series = XYChart.Series<Number, Number>()
        series.name = legendLabel
//        series.name = this.legendLabel2

        // Populating the series with data
        Jday.subList(0, Jday.lastIndex).zip(JdayDiff).forEach {
            series.data.add(XYChart.Data(it.first, it.second))
        }

        val scene = Scene(lineChart, 800.0, 600.0)
        lineChart.data.add(series)

        stage.scene = scene
        stage.show()
    }
}

class Tests {

    @Test
    fun deGrayReservoirExample() {
        // Example: Read CE-QUAL-W2 QIN file from the DeGray Reservoir example
        // dataset provided by Portland State University with the download of
        // version 4.1.
        val infile = "data/DeGray/qin_br1.npt"
        val outfile = "results/qin_br1_from_DSS.npt"
        val startYear = 2014 // The year must be specified to convert the Julian days to dates for DSS
        val nHeaderLines = 3 // Number of lines to skip
        // Define path parts, etc.
        val A = "DeGray Reservoir"
        val B = "Branch 1"
        val C = "Flow"
        val D = ""
        val E = "IR-MONTH"
        val F = "Example"
        val units = "cms"
        val dataType = "PER-AVG"
        val timeInterval = -1 // Irregular time series

        // Parse the data, skipping the header
        val Lines = File(infile).readLines()

        Lines.subList(nHeaderLines, Lines.size).forEach { line ->
            // Split on whitespace, after removing commas and whitespace on each end
            val data = line.trim().replace(',',' ')
                    .split("\\s+".toRegex()).map { it.trim() }
            val jday = data.get(0).toDouble()
            val value = data.get(1).toDouble()
            Jday.add(jday)
            Values.add(value)
        }

        val Dates = mutableListOf<Int>()
        val DatesStr = mutableListOf<String>()
        val DatesStrExcel = mutableListOf<String>()

        Jday.forEachIndexed { index, jday ->
            val date = julianToDate(jday, startYear)
            val hecDateTimeString = date.getHecDateTimeString()
            DatesStr.add(hecDateTimeString)
            // The following function call was taken from the HEC-DSSVue user's manual,
            // Chapter 8, TimeSeriesContainer section, footnote 1:
            val dateTimeAsInteger = HecTime(hecDateTimeString).value()
            DatesStrExcel.add(date.getExcelDateTimeString())
            Dates.add(dateTimeAsInteger)
        }

        val tsc = createTimeSeriesContainer(Dates.toList(), Values.toList(),
                units, dataType, timeInterval, A, B, C, D, E, F)

        // Plot time series
        val yLabel = "Flow (cms)"
        val legendLabel = "Inflow (Qin) Branch 1"
        val plotTitle = "DeGray Reservoir"
        val imageFilename = "DeGray_qin_br1"
        plotTimeSeries(tsc, yLabel, plotTitle, legendLabel, imageFilename)

        sleep(10)

        // Test JavaFX Plot1
        // This will use the global variables Jday and Values
        Application.launch(Plot1::class.java)
    }

    @Test
    // Analyze Julian day differences in the W2 input and output files
    fun dayDifferenceTest1() {
        val A = "DeGray Reservoir"
        val xunits = "Julian Day"
        val yunits = "Days"
        val path = "C:/CE-QUAL-W2/v4.1/Examples/DeGray Reservoir"
        val pdcList = mutableListOf<PairedDataContainer>()
        val infileList = listOf(
                "qin_br1.npt",
                "qwo_31.opt")

        infileList.forEach {infile ->
            val pdc = getDifferencePDC("$path/$infile", xunits, yunits, A=A, C=infile)
            pdcList.add(pdc)
        }

        plotPairedData(pdcList, "Days",
                "Julian Day Difference, DeGray Reservoir",
                infileList, "JulianDayDifference")

        sleep(100000)
    }

    @Test
    // Analyze Julian day differences in the W2 input and output files
    fun dayDifferenceTest2() {
        val A = "Particle Tracking"
        val xunits = "Julian Day"
        val yunits = "Days"
        val C = listOf("Inflow, BR1","Inflow, BR2","Inflow, BR3",
                "Inflow, BR4","Inflow, BR5","Inflow, BR6")

        val pdc1 = getDifferencePDC("data/ParticleTracking/qin_br1.csv", xunits, yunits, A=A, C=C[0])
        val pdc2 = getDifferencePDC("data/ParticleTracking/qin_br2.csv", xunits, yunits, A=A, C=C[1])
        val pdc3 = getDifferencePDC("data/ParticleTracking/qin_br3.csv", xunits, yunits, A=A, C=C[2])
        val pdc4 = getDifferencePDC("data/ParticleTracking/qin_br4.csv", xunits, yunits, A=A, C=C[3])
        val pdc5 = getDifferencePDC("data/ParticleTracking/qin_br5.csv", xunits, yunits, A=A, C=C[4])
        val pdc6 = getDifferencePDC("data/ParticleTracking/qin_br6.csv", xunits, yunits, A=A, C=C[5])

        var pdcList = listOf(pdc1, pdc2, pdc3, pdc4, pdc5, pdc6)
        plotPairedData(pdcList, "days",
                "Julian Day Difference, Particle Tracking Model, Inflows",
                legendLabels, "JulianDayDifference")

        legendLabels = listOf("Outflow, Seg64", "Outflow, Seg84", "Outflow, Gate1, Seg64")
        val pdc7 = getDifferencePDC("data/ParticleTracking/qwo_64.csv", xunits, yunits, A=A, C= legendLabels[0])
        val pdc8 = getDifferencePDC("data/ParticleTracking/qwo_84.csv", xunits, yunits, A=A, C= legendLabels[1])
        val pdc9 = getDifferencePDC("data/ParticleTracking/qwo_gate1_seg64.csv", xunits, yunits, A=A, C= legendLabels[2])

        pdcList = listOf(pdc7, pdc8, pdc9)
        plotPairedData(pdcList, "days",
                "Julian Day Difference, Particle Tracking Model, Outflows",
                C, "JulianDayDifference")

//        sleep(10)

        // Test JavaFX Plot2
        // This will use the following global variables
        Jday = pdc7.xOrdinates.toMutableList()
        JdayDiff = diff(Jday)
        legendLabel = "Particle Tracking, Julian Day Difference, qwo_64.csv"
        Application.launch(Plot2::class.java)
    }

    @Test
    // Analyze Julian day differences from Zhong's TDG output file
    fun dayDifferenceTest3() {
        val A = "TDG Example"
        val xunits = "Julian Day"
        val yunits = "Days"

        val pdc = getDifferencePDC("data/fromZhong/dwo_45.opt", xunits, yunits, A=A, C="Inflow, BR1")

        plotPairedData(listOf(pdc), "Days",
                "Julian Day Difference, Zhong's TDG File",
                listOf("TDG day difference"), "data/fromZhong/JulianDayDifference")

        sleep(100000)
    }

    @Test
    // Analyze Julian day differences from Particle Tracking model provided with W2 examples
    fun dayDifferenceTest4() {
        val A = "Particle Tracking DWO"
        val xunits = "Julian Day"
        val yunits = "Days"
        var infile = ""
//        val path = "data/ParticleTracking"
        val path = "C:/CE-QUAL-W2/v4.1/Examples/ParticleTracking2"

        val infileList = listOf(
                "qin_br1.csv",
                "qin_br2.csv",
                "qin_br3.csv",
                "qin_br4.csv",
                "qin_br5.csv",
                "qin_br6.csv",

//                "tin_br1.csv",
//                "tin_br2.csv",
//                "tin_br3.csv",
//                "tin_br4.csv",
//                "tin_br5.csv",
//                "tin_br6.csv",
//                "tdt_br1.csv",
//                "tsr_1_seg64.csv",
//                "tsr_2_seg84.csv",
//                "ttr_tr1.csv",


                "cin_br1.csv",
                "cin_br2.csv",
                "cin_br3.csv",
                "cin_br4.csv",
                "cin_br5.csv",
                "cin_br6.csv",
//                "cdt_br1.csv",
//                "ctr_tr1.csv",

                "qwo_64.csv"
//                "qwo_84.csv",
//                "qwo_gate1_seg64.csv",
//                "qwo_str1_seg64.csv",
//                "qwo_str2_seg64.csv",
//                "qwo_str3_seg64.csv",
//                "qwo_str4_seg64.csv",
//                "qwo_str5_seg64.csv",
//                "qwo_wd1_seg84.csv",
//                "qwo_wd2_seg84.csv",
//                "qwo_wd3_seg84.csv",
//                "qwo_wd4_seg84.csv",
//                "qwo_wd5_seg84.csv",
//                "cwo_64.csv",
//                "cwo_84.csv",
//                "cwo_gate1_seg64.csv",
//                "cwo_str1_seg64.csv",
//                "cwo_str2_seg64.csv",
//                "cwo_str3_seg64.csv",
//                "cwo_str4_seg64.csv",
//                "cwo_str5_seg64.csv",
//                "cwo_wd1_seg84.csv",
//                "cwo_wd2_seg84.csv",
//                "cwo_wd3_seg84.csv",
//                "cwo_wd4_seg84.csv",
//                "cwo_wd5_seg84.csv",
//                "dwo_64.csv",
//                "dwo_84.csv",
//                "dwo_gate1_seg64.csv",
//                "dwo_str1_seg64.csv",
//                "dwo_str2_seg64.csv",
//                "dwo_str3_seg64.csv",
//                "dwo_str4_seg64.csv",
//                "dwo_str5_seg64.csv",
//                "dwo_wd1_seg84.csv",
//                "dwo_wd2_seg84.csv",
//                "dwo_wd3_seg84.csv",
//                "dwo_wd4_seg84.csv",
//                "dwo_wd5_seg84.csv"
                )
        val pdcList = mutableListOf<PairedDataContainer>()
        infileList.forEach { infile ->
            val pdc = getDifferencePDC("$path/$infile", xunits, yunits, A=A, C=infile)
            pdcList.add(pdc)
        }

        plotPairedData(pdcList, "Days",
                "Julian Day Difference, Zhong's TDG File",
                infileList, "data/ParticleTracking/dwo_time_analysis")

        sleep(1000000)
    }

    @Test
    // Analyze Julian day differences from Dworshak
    fun dayDifferenceTest5() {
        val A = "Dworshak"
        val xunits = "Julian Day"
        val yunits = "Days"
        var infile = ""
        val path = "C:/Users/q0hectes/Documents/Sandbox/Columbia-Snake_2018_04_10/Columbia-Snake/runs/W2_test1/2014Test1b/CE-QUAL-W2/Dworshak/DWR"

        val infileList = listOf(
//                "Qin_DWR_inflow_2014.csv",
//                "DWR_BR2_QIN.npt",
//                "DWR_BR3_QIN.npt",
//                "CRSI_temp_2014.csv",
//                "CRSI_temp_2014_1.csv",
//                "CRSI_temp_2014_2.csv",
                "qwo_45.opt",
                "two_45.opt",
                "qwo_gate1_seg45.opt",
                "qwo_gate2_seg45.opt",
                "qwo_gate3_seg45.opt",
                "qwo_gate4_seg45.opt",
                "qwo_gate5_seg45.opt",
                "qwo_gate6_seg45.opt",
                "qwo_gate7_seg45.opt",
                "qwo_gate8_seg45.opt",
                "qwo_gate9_seg45.opt"
        )

        val pdcList = mutableListOf<PairedDataContainer>()
        infileList.forEach { infile ->
            val pdc = getDifferencePDC("$path/$infile", xunits, yunits, A=A, C=infile)
            pdcList.add(pdc)
        }

        plotPairedData(pdcList, "Days",
                "Julian Day Difference, Zhong's TDG File",
                infileList, "data/ParticleTracking/dwo_time_analysis")

        sleep(1000000)
    }

    @Test
    // Analyze Julian day differences from Spokane sample data provided with W2
    fun dayDifferenceTest6() {
        val A = "Spokane"
        val xunits = "Julian Day"
        val yunits = "Days"
        var infile = ""
        val path = "C:/CE-QUAL-W2/v4.1/Examples/Spokane River"

        val infileList = listOf(
                "cin_br8.npt",
                "cwo_10.opt",
                "cwo_41.opt",
                "cwo_sp1_seg41.opt"
        )

        val pdcList = mutableListOf<PairedDataContainer>()
        infileList.forEach { infile ->
            val pdc = getDifferencePDC("$path/$infile", xunits, yunits, A=A, C=infile)
            pdcList.add(pdc)
        }

        plotPairedData(pdcList, "Days",
                "Julian Day Difference, Zhong's TDG File",
                infileList, "data/ParticleTracking/dwo_time_analysis")

        sleep(1000000)
    }

    @Test
    fun interpolationTest() {
        // Example: Read CE-QUAL-W2 QIN file from the DeGray Reservoir example
        // dataset provided by Portland State University with the download of
        // version 4.1.
        val infile = "data/DeGray/qin_br1.npt"
        val startYear = 2014 // The year must be specified to convert the Julian days to dates for DSS
        val nHeaderLines = 3 // Number of lines to skip
        // Define path parts, etc.
        val A = "DeGray Reservoir"
        val B = "Branch 1"
        val C = "Flow"
        val D = ""
        val E = "IR-MONTH"
        val F = "Example"
        val units = "cms"
        val dataType = "PER-AVG"
        val timeInterval = -1 // Irregular time series

        // Parse the data, skipping the header
        val Lines = File(infile).readLines()

        Lines.subList(nHeaderLines, Lines.size).forEach { line ->
            // Split on whitespace, after removing commas and whitespace on each end
            val data = line.trim().replace(',', ' ')
                    .split("\\s+".toRegex()).map { it.trim() }
            val jday = data.get(0).toDouble()
            val value = data.get(1).toDouble()
            Jday.add(jday)
            Values.add(value)
        }

        val Dates = mutableListOf<Int>()
        val DatesStr = mutableListOf<String>()
        val DatesStrExcel = mutableListOf<String>()

        Jday.forEachIndexed { index, jday ->
            val date = julianToDate(jday, startYear)
            val hecDateTimeString = date.getHecDateTimeString()
            DatesStr.add(hecDateTimeString)
            // The following function call was taken from the HEC-DSSVue user's manual,
            // Chapter 8, TimeSeriesContainer section, footnote 1:
            val dateTimeAsInteger = HecTime(hecDateTimeString).value()
            DatesStrExcel.add(date.getExcelDateTimeString())
            Dates.add(dateTimeAsInteger)
        }

        val tsc = createTimeSeriesContainer(Dates.toList(), Values.toList(),
                units, dataType, timeInterval, A, B, C, D, E, F)

        val t = HecMath.generateRegularIntervalTimeSeries("01Jan2001 0100",
                "05Jan2001 0100","1HOUR", 0.0)

        val m = HecMath.createInstance(tsc)
        m.interpolateDataAtRegularInterval("1HOUR", "")
        println(m)
    }

}
