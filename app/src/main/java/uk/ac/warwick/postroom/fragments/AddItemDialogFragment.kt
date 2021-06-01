package uk.ac.warwick.postroom.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uk.ac.warwick.postroom.R
import uk.ac.warwick.postroom.activities.TAG
import uk.ac.warwick.postroom.adapter.RecipientAdapter
import uk.ac.warwick.postroom.databinding.ActivityAddItemBinding
import uk.ac.warwick.postroom.domain.Courier
import uk.ac.warwick.postroom.services.ProvidesBaseUrl
import uk.ac.warwick.postroom.services.SscPersistenceService
import uk.ac.warwick.postroom.utils.KeyboardUtil
import uk.ac.warwick.postroom.vm.AddItemViewModel
import uk.ac.warwick.postroom.vm.CameraViewModel


class AddPhotoBottomDialogFragment(
    private val sscPersistenceService: SscPersistenceService,
    private val baseUrl: ProvidesBaseUrl,
    private val initialModel: CameraViewModel
) : BottomSheetDialogFragment() {

    private val model: AddItemViewModel by viewModels()

    private var onDismissCallback: () -> Unit = {}

    fun setOnDismissCallback(callback: () -> Unit) {
        this.onDismissCallback = callback
    }

    private var onSuccessCallback: () -> Unit = {}

    fun setOnSuccessCallback(callback: () -> Unit) {
        this.onSuccessCallback = callback
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

    override fun onDismiss(dialog: DialogInterface) {
        this.onDismissCallback()
        super.onDismiss(dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // get the views and attach the listener
        val view: ActivityAddItemBinding = DataBindingUtil.inflate(
            inflater,
            R.layout.activity_add_item, container,
            false
        )
        this.model.qrId.postValue(initialModel.qrId.value!!)
        if (initialModel.bestBarcode.value != null) {
            this.model.trackingBarcode.postValue(initialModel.bestBarcode.value!!.barcode)
        }
        view.model = model
        view.lifecycleOwner = this
        return view.bottomSheetParent
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val progressBarView = view.findViewById<ProgressBar>(R.id.progressBar)
        val recipientAdapter = RecipientAdapter(
            this.requireActivity(),
            android.R.layout.simple_dropdown_item_1line,
            baseUrl,
            sscPersistenceService,
            progressBarView
        )
        view.findViewById<AutoCompleteTextView>(R.id.recipientDropdown).setAdapter(
            recipientAdapter
        )

        model.couriers.observe(this.viewLifecycleOwner) { list ->
            val courierAdapter = ArrayAdapter<Courier>(
                this.requireActivity(),
                android.R.layout.simple_dropdown_item_1line,
                model.couriers.value!!
            )
            view.findViewById<AutoCompleteTextView>(R.id.courierDropdown).setAdapter(
                courierAdapter
            )
        }

        model.courierId.observe(this.viewLifecycleOwner) { id ->
            view.findViewById<AutoCompleteTextView>(R.id.courierDropdown).setText(model.couriers.value!!.first{it.id == id}.name, false)
        }

        this.model.couriers.postValue(initialModel.couriers.value!!)

        if (initialModel.courierGuess.value != null) {
            this.model.courierId.postValue(initialModel.courierGuess.value!!.id)
        }

        progressBarView.visibility = View.GONE
        view.findViewById<Button>(R.id.saveButton).setOnClickListener {

        }

        view.findViewById<AutoCompleteTextView>(R.id.recipientDropdown).onItemClickListener =
            (AdapterView.OnItemClickListener { parent, view, position, id ->
                Log.i(TAG, recipientAdapter.resolveIdToRecipient(position).id)
                model.recipientId.postValue(recipientAdapter.resolveIdToRecipient(position).id)
            })

        view.findViewById<AutoCompleteTextView>(R.id.recipientDropdown)
            .addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(p0: Editable?) {
                    model.recipientId.postValue(null)
                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    // don't care
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    // don't care
                }
            })
    }

    companion object {
        fun newInstance(
            sscPersistenceService: SscPersistenceService,
            baseUrl: ProvidesBaseUrl,
            model: CameraViewModel
        ): AddPhotoBottomDialogFragment {
            return AddPhotoBottomDialogFragment(sscPersistenceService, baseUrl, model)
        }
    }
}