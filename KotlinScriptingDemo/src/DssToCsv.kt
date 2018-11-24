import hec.heclib.dss.HecDss
import hec.io.TimeSeriesContainer
import java.io.File

/**
 * HEC-DSS Path Container
 */
class DssPath(val path : String) {
    val parts = path.trim('/').split('/')
    val A = parts[0]
    val B = parts[1]
    val C = parts[2]
    val D = parts[3]
    val E = parts[4]
    val F = parts[5]

    constructor(A : String, B : String, C : String, D : String, E : String, F : String) :
            this("/$A/$B/$C/$D/$E/$F/")

}

/**
 * Create CSV output filename using the A, B, C, and F parts,
 * first removing colons and spaces
 */
fun makeOutfilename(dssPath : DssPath) : String {
    fun clean(part : String) =
            part.replace(":","@").replace(" ","^")
    val A = clean(dssPath.A)
    val C = clean(dssPath.C)
    val F = clean(dssPath.F)
    return "${A}%${C}%$F.csv"
}

/**
 * Export data from DSS to CSV
 */
fun dssToCsv(hecDssFile: HecDss?, dssPath: DssPath, outfilename: String) {
    var lines = ""

    try {
        // Get TimeSeriesContainer from DSS file
        val tsc = hecDssFile?.get(dssPath.path) as TimeSeriesContainer

        // Assemble a single string of lines to write to CSV
        lines = "Excel Date #,${dssPath.C}\n"
        for (i in tsc.values.indices) {
            lines += String.format("%.5f,%.3f\n", tsc.times[i]/24.0/60.0 + 1, tsc.values[i])
        }
    } catch (e : Exception) {
        println("Error reading DSS path: ${dssPath.path}")
        return
    } finally {
        hecDssFile?.close()
    }

    try {
        // Write the data to the CSV file
        val outfile = File(outfilename)
        outfile.writeText(lines)
    } catch (e : Exception) {
        println("Error writing CSV file: $outfilename")
    } finally {
        hecDssFile?.close()
    }
}

fun printProgress(i : Int, frequency : Int) {
    if (i % frequency == 0)
        println("Processing ${i+1} - ${i+1+frequency}")
}

fun main(args : Array<String>) {
    // Specify whether to extract all data from the DSS
    // or a subset, specified in "dssPaths.txt"
    val EXPORT_ALL = true

    // Open DSS file
    val inFilename = "C:/Users/q0hectes/Documents/IdeaProjects/KotlinScriptingDemo/CRSO_Test.dss"
    val startTime = "01Jan2014 0000"
    val endTime = "31Jan2014 2400"
    // val startTime = ""
    // val endTime = ""

    // Open DSS file
    // Note: Specifying a time window when opening does not work
    val hecDssFile = HecDss.open(inFilename)

    // Set time window, which will limit the time extents
    // of the data exported from DSS to CSV
    if (startTime.isNotEmpty() && endTime.isNotEmpty()) {
        hecDssFile.setTimeWindow(startTime, endTime)
    }

    if (EXPORT_ALL) {
        // Export all DSS records to CSV

        // Assemble a list of unique DSS paths.
        // The catalog path list contains multiple DSS paths
        // for each data record, one per data block (month).
        // We only want a single DSS path for each record to
        // export, so we are removing the D part from each
        // path and then adding it to a "set", which
        // only allows unique paths to be added.
        println("Assembling a list of unique paths...")
        var i = 0
        val outpathSet = mutableSetOf<String>()
//        hecDssFile.pathnameList.slice(0..200).forEach {
        hecDssFile.pathnameList.forEach {
            // Remove D part from DSS path
            val path = DssPath(it as String)
            val newPath = "/${path.A}/${path.B}/${path.C}//${path.E}/${path.F}/"
            outpathSet.add(newPath)
            printProgress(i, 100)
            i++
        }

        println("Exporting from DSS to CSV...")
        i = 0
        outpathSet.forEach {
            val dssPath = DssPath(it)
            val outFilename = makeOutfilename(dssPath)
            dssToCsv(hecDssFile, dssPath, outFilename)
            printProgress(i, 100)
            i++
        }
    }
    else {
        // Only export a subset of DSS records. These are
        // specified in a local file named "dssPaths.txt"
        val pathsFile = File("dssPaths.txt")
        val lines = pathsFile.readLines()
        for (line in lines) {
            val dssPath = DssPath(line.trim())
            val outFilename = makeOutfilename(dssPath)
            dssToCsv(hecDssFile, dssPath, outFilename)
        }
    }
}