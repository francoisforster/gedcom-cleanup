A simple Kotlin application that cleans up GEDCOM (https://en.wikipedia.org/wiki/GEDCOM) files by:
* removing unreachable individuals and families (from a given starting individual)
* canonicalizing Notes and Sources and removing duplicates and unreachable ones

The application assumes a well-formed GEDCOM file and doesn't enforce the format.
