package example3

/*
 * @author Augustus
 * created on 2024-09-19
 *
 * example just to check how kotlin compiler handles not nulls for generated code
 *
 * do not expect this example do anything
*/

fun main() {
    val doc = example1.generated.pain_001_001_11.Document()

    // expected not nulls (if it was valid doc)
    // kotlin don't ask for nullability (?.)
    doc.cstmrCdtTrfInitn.grpHdr.initnSrc

    // initnSrc is nullable - "?." is required
    doc.cstmrCdtTrfInitn.grpHdr.initnSrc?.nm
}