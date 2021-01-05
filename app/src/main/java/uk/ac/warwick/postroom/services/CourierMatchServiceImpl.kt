package uk.ac.warwick.postroom.services

import android.content.Context
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.builtins.ListSerializer
import uk.ac.warwick.postroom.domain.Courier
import uk.ac.warwick.postroom.domain.CourierMatchPattern
import uk.ac.warwick.postroom.domain.RecognisedBarcode
import uk.ac.warwick.postroom.fuel.withSscAuth
import javax.inject.Inject

class CourierMatchServiceImpl @Inject constructor(
    @ApplicationContext val applicationContext: Context,
    val providesBaseUrl: ProvidesBaseUrl,
    val sscPersistenceService: SscPersistenceService
) : CourierMatchService {

    override fun guessFromBarcode(
        patterns: List<CourierMatchPattern>,
        barcode: RecognisedBarcode
    ): CourierMatchPattern? {
        val patternMatchingWithHints = patterns.firstOrNull {
            barcode.format != null && it.matchesBarcode(barcode) && !it.reject
        }

        if (patternMatchingWithHints != null) {
            return patternMatchingWithHints
        }

        val patternMatchingWithoutHints = patterns.firstOrNull {
            it.pattern.toRegex().matchEntire(barcode.barcode) != null && !it.reject
        }

        return patternMatchingWithoutHints
    }

    override fun fetchAllCouriers(callback: (List<Courier>) -> Unit) {
        Fuel.get(
            "${providesBaseUrl.getBaseUrl()}api/couriers"
        ).useHttpCache(false).withSscAuth(sscPersistenceService.getSsc()!!).useHttpCache(true)
            .responseObject<List<Courier>>(kotlinxDeserializerOf()) { _, _, result ->
                if (result.component2() != null) {
                    throw IllegalStateException("Failed to fetch couriers")
                }
                callback(result.get())
            }
    }

    override fun fetchAllCourierPatterns(callback: (List<CourierMatchPattern>) -> Unit) {
        Fuel.get(
            "${providesBaseUrl.getBaseUrl()}api/courier-patterns"
        ).useHttpCache(false).withSscAuth(sscPersistenceService.getSsc()!!).useHttpCache(true)
            .responseObject(
                kotlinxDeserializerOf(
                    ListSerializer(
                        CourierMatchPattern.serializer()
                    )
                )
            ) { _, _, result ->
                if (result.component2() != null) {
                    throw IllegalStateException("Failed to fetch courier patterns")
                }
                callback(result.get())
            }
    }

    override fun excludeRejects(
        courierPatterns: List<CourierMatchPattern>,
        barcodes: Set<RecognisedBarcode>
    ): Set<RecognisedBarcode> = barcodes.map {
        it to courierPatterns.any { cmp: CourierMatchPattern ->
            cmp.matchesBarcode(it) && cmp.reject
        }
    }.filterNot { it.second }.map { it.first }.toSet()
}