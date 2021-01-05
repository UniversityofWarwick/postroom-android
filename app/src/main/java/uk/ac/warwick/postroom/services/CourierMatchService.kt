package uk.ac.warwick.postroom.services

import uk.ac.warwick.postroom.domain.Courier
import uk.ac.warwick.postroom.domain.CourierMatchPattern
import uk.ac.warwick.postroom.domain.RecognisedBarcode

interface CourierMatchService {
    fun guessFromBarcode(patterns: List<CourierMatchPattern>, barcode: RecognisedBarcode): CourierMatchPattern?
    fun fetchAllCouriers(callback: (List<Courier>) -> Unit)
    fun fetchAllCourierPatterns(callback: (List<CourierMatchPattern>) -> Unit)
    fun excludeRejects(courierPatterns: List<CourierMatchPattern>, barcodes: Set<RecognisedBarcode>): Set<RecognisedBarcode>
}