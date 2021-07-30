package uk.ac.warwick.postroom.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Bitmap
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
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.activity_add_item.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uk.ac.warwick.postroom.R
import uk.ac.warwick.postroom.activities.TAG
import uk.ac.warwick.postroom.adapter.RecipientAdapter
import uk.ac.warwick.postroom.databinding.ActivityAddItemBinding
import uk.ac.warwick.postroom.domain.Courier
import uk.ac.warwick.postroom.domain.ItemResult
import uk.ac.warwick.postroom.services.ItemService
import uk.ac.warwick.postroom.services.ProvidesBaseUrl
import uk.ac.warwick.postroom.services.RecipientDataService
import uk.ac.warwick.postroom.services.SscPersistenceService
import uk.ac.warwick.postroom.utils.KeyboardUtil
import uk.ac.warwick.postroom.vm.AddItemViewModel
import uk.ac.warwick.postroom.vm.CameraViewModel
import java.util.*


class AddPhotoBottomDialogFragment(
    private val sscPersistenceService: SscPersistenceService,
    private val baseUrl: ProvidesBaseUrl,
    private val initialModel: CameraViewModel,
    private val recipientDataService: RecipientDataService,
    private val itemService: ItemService
) : BottomSheetDialogFragment() {

    private val model: AddItemViewModel by viewModels()

    private var onDismissCallback: () -> Unit = {}

    private var bitmap: Bitmap? = null

    fun setOnDismissCallback(callback: () -> Unit) {
        this.onDismissCallback = callback
    }

    private var onSuccessCallback: (item: ItemResult) -> Unit = {}
    private var onFailureCallback: (result: Result<ItemResult, FuelError>) -> Unit = {}

    fun setOnSuccessCallback(callback: (item: ItemResult) -> Unit) {
        this.onSuccessCallback = callback
    }

    fun setOnFailureCallback(callback: (result: Result<ItemResult, FuelError>) -> Unit) {
        this.onFailureCallback = callback
    }

    fun setBitmap(bitmap: Bitmap) {
        this.bitmap = bitmap
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
        val recipientTextView = view.findViewById<AutoCompleteTextView>(R.id.recipientDropdown)
        recipientTextView.setAdapter(
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
            view.findViewById<AutoCompleteTextView>(R.id.courierDropdown)
                .setText(model.couriers.value!!.first { it.id == id }.name, false)
        }

        this.model.couriers.postValue(initialModel.couriers.value!!)

        if (initialModel.courierGuess.value != null) {
            this.model.courierId.postValue(initialModel.courierGuess.value!!.id)
        }

        if (initialModel.recipientGuesses.value != null && initialModel.recipientGuesses.value!!.distinctBy { it.id }.size == 1) {
            val id = initialModel.recipientGuesses.value!!.toList()[0].id
            recipientTextView
                .setText("Just a sec...", false)
            model.viewModelScope.launch(Dispatchers.IO) {
                val recipientResult = recipientDataService.getMiniRecipient(UUID.fromString(id))
                // back on UI thread
                if (recipientResult.isSuccess) {
                    model.recipientId.postValue(id)
                    val mr = recipientResult.getOrNull()!!
                    this.launch(Dispatchers.Main) {
                        recipientTextView.tag = "P"
                        recipientTextView
                            .setText(mr.toString(), false)
                        recipientTextView.tag = null
                    }
                }
            }
        }

        progressBarView.visibility = View.GONE
        view.findViewById<Button>(R.id.saveButton).setOnClickListener {
            // we have to create the item first for reasons.
            model.viewModelScope.launch(Dispatchers.IO) {
                val addedItem = itemService.addItem(model)
                val bitmap = bitmap
                dismiss()
                if (addedItem.component2() != null) {
                    onFailureCallback(addedItem)
                } else {
                    onSuccessCallback(addedItem.get())
                }
                if (bitmap != null && addedItem.component2() == null) {
                    itemService.uploadImageForItem(UUID.fromString(model.qrId.value), bitmap)
                }

            }
        }

        recipientTextView.onItemClickListener =
            (AdapterView.OnItemClickListener { parent, view, position, id ->
                Log.i(TAG, recipientAdapter.resolveIdToRecipient(position).id)
                model.recipientId.postValue(recipientAdapter.resolveIdToRecipient(position).id)
            })

        view.findViewById<AutoCompleteTextView>(R.id.courierDropdown).onItemClickListener =
            (AdapterView.OnItemClickListener { parent, view, position, id ->
                val arrayAdapter = courierDropdown.adapter as? ArrayAdapter<Courier>
                if (arrayAdapter?.getItem(position)?.id != null) {
                    model.courierId.postValue(arrayAdapter.getItem(position)!!.id)
                }
            })

        recipientTextView.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(p0: Editable?) {
                    if (recipientTextView.tag == null) {
                        model.recipientId.postValue(null)
                    }
                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    // don't care
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    // don't care
                }
            })
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    companion object {
        fun newInstance(
            sscPersistenceService: SscPersistenceService,
            baseUrl: ProvidesBaseUrl,
            model: CameraViewModel,
            recipientDataService: RecipientDataService,
            itemService: ItemService
        ): AddPhotoBottomDialogFragment {
            return AddPhotoBottomDialogFragment(
                sscPersistenceService,
                baseUrl,
                model,
                recipientDataService,
                itemService
            )
        }
    }
}