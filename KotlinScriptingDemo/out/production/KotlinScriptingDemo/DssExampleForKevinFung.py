from hec.io import TimeSeriesContainer
from hec.script import Plot
from hec.heclib.util import HecTime
import hec.geometry.Axis

Months = ["January", "February", "March", "April", "May",
          "June", "July", "August", "September", "October",
          "November", "December"]


class MyDateTime:
    def __init__(self, year, month, dayOfMonth, hour, minute):
        self.year = year
        self.month = month
        self.dayOfMonth = dayOfMonth
        self.hour = hour
        self.minute = minute

    def getHecDateString(self):
        return "%02d%3s%04d" % (self.dayOfMonth,
                                Months[self.month - 1][0:3], self.year)

    def getHecTimeString(self):
        return "%02d%02d" % (self.hour, self.minute)

    def getHecDateTimeString(self):
        return self.getHecDateString() + " " + self.getHecTimeString()

    def getExcelDateTimeString(self):
        return "%02d/%02d/%04d %02d:%02d" % (self.month, self.dayOfMonth,
                                             self.year, self.hour, self.minute)


def saveFigure(plot, imfile, imtype):
    imtype = imtype.lower()
    if imtype == "png":
        plot.saveToPng(imfile + ".png")
    if imtype == "jpg":
        plot.saveToJpeg(imfile + ".jpg")
    if imtype == "wmf":
        plot.saveToMetaFile(imfile + ".wmf")


def plotTimeSeries(tsc, yLabel, plotTitle, legendLabel, imageFilename,
                   ymin=0.0, ymax=1000.0, autoScale=True):
    plot = Plot.newPlot()
    # plot.setLocation(-10000,-10000) # Plot offscreen
    layout = Plot.newPlotLayout()
    topView = layout.addViewport()
    topView.addCurve("Y1", tsc)
    plot.configurePlotLayout(layout)

    # Display the plot, then customize
    plot.showPlot()
    plot.setPlotTitleVisible(True)
    plot.setPlotTitleText(plotTitle)

    # Customize
    curve = plot.getCurve(tsc)
    curve.setLineColor("blue")
    lineWidth = 1.5
    curve.setLineWidth(lineWidth)
    plot.setLegendLabelText(tsc, legendLabel)
    vp0 = plot.getViewport(0)

    # Set axis limits and labels
    yaxisTop = vp0.getAxis("Y1")
    if not autoScale:
        yaxisTop.setScaleLimits(ymin, ymax)
    yaxisTop.setLabel(yLabel)
    plot.setSize(800, 400)
    saveFigure(plot, imageFilename, "png")


# Create DSS time series container, to store data
def createTimeSeriesContainer(Dates, Values, units, dataType,
                              timeInterval, A, B, C, D, E, F):
    tsc = TimeSeriesContainer()
    tsc.times = Dates
    tsc.values = Values
    tsc.startTime = Dates[0]
    tsc.endTime = Dates[-1]
    tsc.units = units
    tsc.type = dataType
    tsc.interval = timeInterval
    tsc.watershed = A
    tsc.location = B
    tsc.parameter = C
    tsc.version = F
    tsc.numberValues = len(Values)
    tsc.fullName = "/%s/%s/%s/%s/%s/%s/" % (A, B, C, D, E, F)

    return tsc


def julianToDate(jday, startYear):
    isLeapYear = (startYear % 4 == 0)
    dayOfMonth = 0
    year = startYear
    jdayInt = int(jday)
    leapOffset = 0
    month = 0

    # The Julian days in the W2 ASCII file may be referenced from an earlier year.
    # For example, the record may start at Julian day 367 with 2017 as the
    # reference year. The following code will compute the true start year as 2018
    # and the new start day as 367 - 365 = 2 (i.e., a start date of January 2, 2018).
    while jdayInt >= 366:
        if not isLeapYear and jdayInt >= 366:
            jdayInt = jdayInt - 365
            year = year + 1
            isLeapYear = (year % 4) == 0
        elif jdayInt >= 367:
            jdayInt = jdayInt - 366
            year = year + 1
            isLeapYear = (year % 4) == 0

    if isLeapYear:
        leapOffset = 1
    else:
        leapOffset = 0

    # Determine month and day of the month
    if 1 <= jdayInt < 32:
        dayOfMonth = jdayInt
        month = 1
    elif 32 <= jdayInt < 60 + leapOffset:
        dayOfMonth = jdayInt - 31
        month = 2
    elif 60 + leapOffset <= jdayInt < 91 + leapOffset:
        dayOfMonth = jdayInt - 59 - leapOffset
        month = 3
    elif 91 + leapOffset <= jdayInt < 121 + leapOffset:
        dayOfMonth = jdayInt - 90 - leapOffset
        month = 4
    elif 121 + leapOffset <= jdayInt < 152 + leapOffset:
        dayOfMonth = jdayInt - 120 - leapOffset
        month = 5
    elif 152 + leapOffset <= jdayInt < 182 + leapOffset:
        dayOfMonth = jdayInt - 151 - leapOffset
        month = 6
    elif 182 + leapOffset <= jdayInt < 213 + leapOffset:
        dayOfMonth = jdayInt - 181 - leapOffset
        month = 7
    elif 213 + leapOffset <= jdayInt < 244 + leapOffset:
        dayOfMonth = jdayInt - 212 - leapOffset
        month = 8
    elif 244 + leapOffset <= jdayInt < 274 + leapOffset:
        dayOfMonth = jdayInt - 243 - leapOffset
        month = 9
    elif 274 + leapOffset <= jdayInt < 305 + leapOffset:
        dayOfMonth = jdayInt - 273 - leapOffset
        month = 10
    elif 305 + leapOffset <= jdayInt < 335 + leapOffset:
        dayOfMonth = jdayInt - 304 - leapOffset
        month = 11
    elif 335 + leapOffset <= jdayInt < 366 + leapOffset:
        dayOfMonth = jdayInt - 334 - leapOffset
        month = 12

    decimalHour = (jday - int(jday)) * 24.0
    hour = int(decimalHour)
    minute = int(((decimalHour - int(decimalHour)) * 60))

    return MyDateTime(year, month, dayOfMonth, hour, minute)


if __name__ == "__main__":
    # Example: Read CE-QUAL-W2 QIN file from the ColumbiaSlough example dataset provided by
    # Portland State University with the download of version 4.1
    infile = "C:/Users/q0hectes/Documents/IdeaProjects/KotlinScriptingDemo/data/qin_br1.npt"
    outfile = "C:/Users/q0hectes/Documents/IdeaProjects/KotlinScriptingDemo/results/qin_br1_from_DSS.npt"
    startYear = 2014  # The year must be specified to convert the Julian days to dates for DSS
    nHeaderLines = 3  # type: int # Number of lines to skip
    # Define path parts, etc.
    A = "Columbia Slough"
    B = "Branch 1"
    C = "Flow"
    D = ""
    E = "IR-MONTH"
    F = "Example"
    units = "cms"
    dataType = "PER-AVG"
    timeInterval = -1  # Irregular time series

    # Parse the data, skipping the header
    Lines = file(infile).readlines()
    Jday = []
    Values = []
    Dates = []
    DatesStr = []
    DatesStrExcel = []

    for line in Lines[nHeaderLines:]:
        # Split on whitespace, after removing commas and whitespace on each end
        data = line.strip().replace(',', ' ').split()
        jday = data[0].strip()
        value = data[1].strip()
        jday, value = map(float, [jday, value])
        Jday.append(jday)
        Values.append(value)

    for jday in Jday:
        date = julianToDate(jday, startYear)
        hecDateTimeString = date.getHecDateTimeString()
        DatesStr.append(hecDateTimeString)
        # The following function call was taken from the HEC-DSSVue user's manual,
        # Chapter 8, TimeSeriesContainer section, footnote 1:
        dateTimeAsInteger = HecTime(hecDateTimeString).value()
        DatesStrExcel.append(date.getExcelDateTimeString())
        Dates.append(dateTimeAsInteger)

    tsc = createTimeSeriesContainer(Dates, Values, units, dataType,
                                    timeInterval, A, B, C, D, E, F)

    # Plot time series
    yLabel = "Flow (cms)"
    legendLabel = "Inflow (Qin) Branch 1"
    plotTitle = "Columbia Slough"
    imageFilename = "Columbia_Slough_qin_br1"
    plotTimeSeries(tsc, yLabel, plotTitle, legendLabel, imageFilename)
