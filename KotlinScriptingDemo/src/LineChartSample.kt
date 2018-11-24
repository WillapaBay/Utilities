import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.chart.XYChart.Series
import javafx.stage.Stage


class LineChartSample : Application() {

    override fun start(stage: Stage) {
        stage.title = "Line Chart Sample"

        // Defining the axes
        val xAxis = NumberAxis()
        val yAxis = NumberAxis()
        xAxis.label = "Number of Month"

        // Creating the chart
        val lineChart = LineChart(xAxis, yAxis)
        lineChart.title = "Stock Monitoring, 2010"

        // Defining a series
        val series = Series<Number,Number>()
        series.name = "My portfolio"

        // Populating the series with data
        series.data.add(XYChart.Data(1, 23))
        series.data.add(XYChart.Data(2, 14))
        series.data.add(XYChart.Data(3, 15))
        series.data.add(XYChart.Data(4, 24))
        series.data.add(XYChart.Data(5, 34))
        series.data.add(XYChart.Data(6, 36))
        series.data.add(XYChart.Data(7, 22))
        series.data.add(XYChart.Data(8, 45))
        series.data.add(XYChart.Data(9, 43))
        series.data.add(XYChart.Data(10, 17))
        series.data.add(XYChart.Data(11, 29))
        series.data.add(XYChart.Data(12, 25))

        val scene = Scene(lineChart, 800.0, 600.0)
        lineChart.data.add(series)

        stage.scene = scene
        stage.show()
    }

}

fun main(args: Array<String>) {
    Application.launch(LineChartSample::class.java, *args)
}