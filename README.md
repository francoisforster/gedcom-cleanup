A simple Kotlin library that cleans up GEDCOM (https://en.wikipedia.org/wiki/GEDCOM) files by:
* removing unreachable individuals and families (from a given starting individual)
* canonicalizing Notes and Sources and removing duplicates and unreachable ones

It can also compares 2 GEDCOM files from a specified root individual, providing information on differences of names and event places or dates, as well as events and individuals (parents, children, spouses) missing from either file.

It can also perform some validation on individuals, such as ensuring that sources are specified for all events (birth, death, marriage) that took place at a particular place.

For convenience, executable jars are provided to perform cleanup and comparison (only java runtime is needed to run):
* ```java -jar gedcom-cleanup.jar <Input GEDCOM filename> <Starting Individual Reference Id> <Output GEDCOM filename>```
* ```java -jar gedcom-compare.jar <Input GEDCOM filename> <Starting Individual Reference Id> <Other Input GEDCOM filename> <Starting Individual Reference Id from Other GEDCOM file>```

where individual reference id is in the form ```"@I35@"```

The library assumes a well-formed GEDCOM file, doesn't enforce the format and doesn't implement all its specifications.

Library Usage example:
```kotlin
import java.io.FileWriter

fun main() {
    // clean up GEDCOM file
    val gedcom = Gedcom()
    gedcom.parseFile("<Input GEDCOM filename>")
    val rootIndividual = gedcom.getIndividual("<Starting Individual Reference Id>")
    if (rootIndividual != null) {
        gedcom.removeUnreachable(rootIndividual)
    }
    gedcom.cleanUpReferences(SOURCE_TAG, SOURCE_REFERENCE_PREFIX)
    gedcom.cleanUpReferences(NOTE_TAG, NOTE_REFERENCE_PREFIX)
    val writer = FileWriter("<Output GEDCOM filename>")
    gedcom.write(writer)
    writer.close()
 
    // validate sources
    gedcom.validateEvents(::selectByPlaceAndYear, ::validateSource)

    // compare GEDCOM files
    val otherGedcom = Gedcom()
    otherGedcom.parseFile("<Other GEDCOM filename>")
    val gedcomCompare = GedcomCompare(gedcom, otherGedcom)
    gedcomCompare.compareFrom("<Starting Individual Reference Id in GEDCOM file>", "<Starting Individual Reference Id in other GEDCOM file>")
}

fun selectByPlaceAndYear(event: Event): Boolean {
    val year = event.getYear()
    return event.place?.contains("<PLACE>") == true && year != null && year >= <FROM YEAR> && year <= <TO YEAR>
}

fun validateSource(event: Event, gedcom: Gedcom): Boolean {
    val source = gedcom.getSource(event.source)
    return source != null && source.getSourceText()?.contains("http://...") == true
}

```
