import hec.gfx2d.G2dDialog
import hec.heclib.dss.HecDss
import hec.io.TimeSeriesContainer
import hec.script.Plot
import java.io.File

fun savePlot(plot: G2dDialog?, imfile: String, imtype : String) {
    when (imtype.toLowerCase()) {
        "png" -> plot?.saveToPng("$imfile.png")
        "wmf" -> plot?.saveToMetafile("$imfile.wmf")
        "jpg" -> plot?.saveToJpeg("$imfile.jpg")
    }
}

fun main(args : Array<String>) {
    // Open DSS file
    val infile = "C:/Users/q0hectes/Documents/IdeaProjects/KotlinScriptingDemo/data/RAS_WQ.dss"
    println("File exists? " + File(infile).exists())
    val hecDssFile = HecDss.open(infile)

    // Get one time series record
    // val inPath = "/CBT-REV/DWR/FLOW-OUT//1HOUR//"
    val inPath = "/GOES-COMPUTED-REV/ORFI/FLOW//IR-DAY//"

    val data1 = hecDssFile.get(inPath, true)
            as TimeSeriesContainer

    // Plot time series
    val ymin = 0.0
    val ymax = 25.0
    val plot = Plot.newPlot()
    // plot.setLocation(-10000,-10000) // Plot offscreen
    val layout = Plot.newPlotLayout()
    val topView = layout.addViewport()
    topView.addCurve("Y1", data1)
    plot.configurePlotLayout(layout)

    // Display the plot, then customize
    plot.showPlot()
    plot.setPlotTitleVisible(true)
    plot.setPlotTitleText("Kotlin DSS Plot")

    // Customize
    val curve = plot.getCurve(data1)
    curve.setLineColor("blue")
    val lineWidth = 1.5f
    curve.setLineWidth(lineWidth)
    val legendLabel = "Columbia River Mile 14.2"
    plot.setLegendLabelText(data1, legendLabel)
    val vp0 = plot.getViewport(0)

    // Set axis limits and labels
    val yaxisTop = vp0.getAxis("Y1")
//    yaxisTop.setScaleLimits(ymin, ymax)
    val ylabel = "Flow (cfs)"
    yaxisTop.setLabel(ylabel)

    plot.setSize(800, 400)
    savePlot(plot,"KotlinDssPlot","png")
    // plot.close()
}
