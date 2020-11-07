package uk.ac.warwick.postroom.fuel

import com.github.kittinunf.fuel.core.Request
import uk.ac.warwick.postroom.SSC_NAME

fun Request.withSscAuth(ssc: String): Request {
    header("Cookie", "$SSC_NAME=$ssc")
    return this
}