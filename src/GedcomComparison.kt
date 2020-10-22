fun main(args: Array<String>) {
    if (args.size < 4) {
        println("Usage: java -jar gedcom-compare <input GEDCOM filename> <root individual reference> <other GEDCOM filename> <root individual reference in other GEDCOM file>")
	return
    }
    val gedcom = Gedcom()
    gedcom.parseFile(args[0])
    val otherGedcom = Gedcom()
    otherGedcom.parseFile(args[2])
    println("Comparing gedcom files")
    val gedcomCompare = GedcomCompare(gedcom, otherGedcom)
    gedcomCompare.compareFrom(args[1], args[3])
}
