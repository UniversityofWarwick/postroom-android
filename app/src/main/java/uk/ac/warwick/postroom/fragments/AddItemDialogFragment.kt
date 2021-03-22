package uk.ac.warwick.postroom.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ProgressBar
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uk.ac.warwick.postroom.R
import uk.ac.warwick.postroom.activities.TAG
import uk.ac.warwick.postroom.adapter.RecipientAdapter
import uk.ac.warwick.postroom.services.ProvidesBaseUrl
import uk.ac.warwick.postroom.services.SscPersistenceService
import uk.ac.warwick.postroom.utils.KeyboardUtil


class AddPhotoBottomDialogFragment(
    private val sscPersistenceService: SscPersistenceService,
    private val baseUrl: ProvidesBaseUrl
) : BottomSheetDialogFragment() {


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
        view.findViewById<AutoCompleteTextView>(R.id.recipientDropdown).setAdapter(
            RecipientAdapter(
                this.requireActivity(),
                android.R.layout.simple_dropdown_item_1line,
                baseUrl,
                sscPersistenceService,
                view.findViewById(R.id.progressBar)
            )
        )
        view.findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
        view.findViewById<Button>(R.id.saveButton).setOnClickListener {

        }

        view.findViewById<AutoCompleteTextView>(R.id.recipientDropdown).onItemClickListener = (object: AdapterView.OnItemClickListener {
            override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Log.i(TAG, "$id is the id")
            }
        })
    }

    companion object {
        fun newInstance(sscPersistenceService: SscPersistenceService, baseUrl: ProvidesBaseUrl): AddPhotoBottomDialogFragment {
            return AddPhotoBottomDialogFragment(sscPersistenceService, baseUrl)
        }
    }
}