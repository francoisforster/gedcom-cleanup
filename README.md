A simple Kotlin library that cleans up GEDCOM (https://en.wikipedia.org/wiki/GEDCOM) files by:
* removing unreachable individuals and families (from a given starting individual)
* canonicalizing Notes and Sources and removing duplicates and unreachable ones

The application assumes a well-formed GEDCOM file, doesn't enforce the format and doesn't implement all its specifications.

Usage example:
```kotlin
import java.io.FileWriter

fun main() {
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
}

```
