import java.io.FileWriter

fun main(args: Array<String>) {
    if (args.size < 4) {
        println(
            "Usage: java -jar gedcom-compare <input GEDCOM filename> <root individual reference> <other GEDCOM filename>" +
                    " <root individual reference in other GEDCOM file> [-createMissingFrom {LEFT|RIGHT} <output GEDCOM filename>]"
        )
        return
    }
    var createMissingFrom = CreateFromSide.NONE
    if (args.size > 5 && args[4] == "-createMissingFrom") {
        createMissingFrom = CreateFromSide.valueOf(args[5])
    }

    val gedcom = Gedcom()
    gedcom.parseFile(args[0])
    val otherGedcom = Gedcom()
    otherGedcom.parseFile(args[2])
    println("Comparing gedcom files")
    val gedcomCompare = GedcomCompare(gedcom, otherGedcom, createMissingFrom)


    gedcomCompare.compareFrom(args[1], args[3])
    if (createMissingFrom != CreateFromSide.NONE) {
        val writer = FileWriter(args[6])
        when (createMissingFrom) {
            CreateFromSide.LEFT -> otherGedcom.write(writer)
            CreateFromSide.RIGHT -> gedcom.write(writer)
        }
        writer.close()
    }
}
