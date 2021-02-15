package uk.ac.warwick.postroom.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uk.ac.warwick.postroom.R
import uk.ac.warwick.postroom.adapter.RecipientAdapter
import uk.ac.warwick.postroom.services.ProvidesBaseUrl
import uk.ac.warwick.postroom.services.SscPersistenceService
import uk.ac.warwick.postroom.utils.KeyboardUtil


class AddPhotoBottomDialogFragment(
    private val sscPersistenceService: SscPersistenceService,
    private val baseUrl: ProvidesBaseUrl
) : BottomSheetDialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        var viewParent = view
        while (viewParent is View) {
            viewParent.setOnApplyWindowInsetsListener { _, insets -> insets }
            if (viewParent.parent == null) {
                break
            }
            viewParent = viewParent.parent as View?

        }
        KeyboardUtil(requireActivity(), viewParent!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // get the views and attach the listener
        return inflater.inflate(
            R.layout.activity_add_item, container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val COUNTRIES = arrayOf(
            "Belgium", "France", "Italy", "Germany", "Spain"
        )
        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
            this.requireContext(),
            android.R.layout.simple_dropdown_item_1line, COUNTRIES
        )

        //view.findViewById<AutoCompleteTextView>(R.id.recipient_dropdown).setAdapter(adapter)
        view.findViewById<AutoCompleteTextView>(R.id.recipient_dropdown).setAdapter(
            RecipientAdapter(
                this.requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                baseUrl,
                sscPersistenceService
            )
        )
    }

    companion object {
        fun newInstance(sscPersistenceService: SscPersistenceService, baseUrl: ProvidesBaseUrl): AddPhotoBottomDialogFragment {
            return AddPhotoBottomDialogFragment(sscPersistenceService, baseUrl)
        }
    }
}