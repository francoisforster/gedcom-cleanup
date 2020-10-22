import java.io.FileWriter

fun main(args: Array<String>) {
    if (args.size < 3) {
        println("Usage: java -jar gedcom-cleanup.jar <input GEDCOM filename> <root individual reference> <output GEDCOM filename>")
        return
    }
    val gedcom = Gedcom()
    gedcom.parseFile(args[0])

    val rootIndividual = gedcom.getIndividual(args[1])
    if (rootIndividual == null) {
        println("Can't find individual ${args[1]}")
        return
    }
    gedcom.removeUnreachable(rootIndividual)
    gedcom.cleanUpReferences(SOURCE_TAG, SOURCE_REFERENCE_PREFIX)
    gedcom.cleanUpReferences(NOTE_TAG, NOTE_REFERENCE_PREFIX)
    val writer = FileWriter(args[2])
    gedcom.write(writer)
    writer.close()
}
