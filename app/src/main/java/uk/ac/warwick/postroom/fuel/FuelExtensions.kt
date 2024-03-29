package uk.ac.warwick.postroom.fuel

import com.github.kittinunf.fuel.core.Request
import uk.ac.warwick.postroom.activities.SSC_NAME

fun Request.withSscAuth(ssc: String): Request {
    header("Cookie", "$SSC_NAME=$ssc")
    return this
}

fun Request.withSscAuthAndCsrfToken(ssc: String, csrfToken: String): Request {
    header("Cookie", "$SSC_NAME=$ssc; XSRF-TOKEN=$csrfToken")
    return this
}