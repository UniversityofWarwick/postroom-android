package uk.ac.warwick.postroom.adapter

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.LayoutRes
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf
import kotlinx.serialization.json.Json
import uk.ac.warwick.postroom.R
import uk.ac.warwick.postroom.activities.TAG
import uk.ac.warwick.postroom.domain.AutocompleteResponse
import uk.ac.warwick.postroom.domain.Recipient
import uk.ac.warwick.postroom.fuel.withSscAuth
import uk.ac.warwick.postroom.services.ProvidesBaseUrl
import uk.ac.warwick.postroom.services.SscPersistenceService


class RecipientAdapter(
    private val activity: Activity,
    @LayoutRes private val layoutResource: Int,
    val providesBaseUrl: ProvidesBaseUrl,
    val sscPersistenceService: SscPersistenceService,
    private val progressIndicator: ProgressBar,
    private val recipients: MutableList<Recipient> = mutableListOf()
) : ArrayAdapter<Recipient>(
    activity,
    layoutResource,
    recipients
) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        return createViewFromResource(position, convertView, parent)
    }

    private fun createViewFromResource(
        position: Int,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val view: TextView = convertView as TextView? ?: LayoutInflater.from(context).inflate(
            layoutResource,
            parent,
            false
        ) as TextView
        val recipient = recipients[position]
        view.text = """${recipient.firstName} ${recipient.lastName} / ${recipient.room} / ${recipient.universityId}"""
        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults? {
                val results = FilterResults()
                if (constraint != null) {
                    Log.i(TAG, "Got new search term of $constraint")
                    activity.runOnUiThread {
                        progressIndicator.visibility = View.VISIBLE
                    }
                    val result = Fuel.get(
                        "${providesBaseUrl.getBaseUrl()}admin/query",
                        listOf("q" to constraint.toString().toUpperCase())
                    ).useHttpCache(false).withSscAuth(sscPersistenceService.getSsc()!!)
                        .useHttpCache(true)
                        .responseObject<AutocompleteResponse>(kotlinxDeserializerOf(Json {
                            ignoreUnknownKeys = true
                        }))

                    val toProcess = result.third
                    activity.runOnUiThread {
                        progressIndicator.visibility = View.GONE
                    }
                    if (toProcess.component2() != null) {
                        Log.e(
                            TAG,
                            "HTTP request for recipient search was a failure",
                            toProcess.component2()!!.exception
                        )
                    } else {
                        recipients.clear()
                        recipients.addAll(
                            toProcess.component1()?.data?.toMutableList()?.take(10) ?: mutableListOf())
                        results.values = recipients
                        results.count = recipients.size

                    }
                }
                return results
            }

            override fun publishResults(
                constraint: CharSequence?,
                results: FilterResults?
            ) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged()
                } else notifyDataSetInvalidated()
            }
        }
    }
}