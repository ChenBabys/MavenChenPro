package com.sahooz.library.countrypicker

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sahooz.library.countrypicker.Country.Companion.getAll
import java.util.Locale

@Deprecated("不建议App直接使用此类，只做使用参考")
class CountryPickerFragment : DialogFragment() {
    private val allCountries = ArrayList<Country>()
    private val selectedCountries = ArrayList<Country>()
    private var callback: PickCountryCallback? = null

    companion object {
        @JvmStatic
        fun newInstance(callback: PickCountryCallback?): CountryPickerFragment =
            CountryPickerFragment().apply {
                this.callback = callback
            }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val root = inflater.inflate(R.layout.dialog_country_picker, container, false)
        val etSearch = root.findViewById<EditText>(R.id.et_search)
        val rvCountry = root.findViewById<RecyclerView>(R.id.rv_country)
        allCountries.clear()
        allCountries.addAll(getAll())
        selectedCountries.clear()
        selectedCountries.addAll(allCountries)
        val adapter = CountryPickerFragmentListAdapter(requireContext())
        adapter.callback =
            object : PickCountryCallback {
                override fun onPick(country: Country) {
                    dismiss()
                    callback?.onPick(country)
                }
            }
        adapter.setSelectedCountries(selectedCountries)
        rvCountry.adapter = adapter
        rvCountry.layoutManager = LinearLayoutManager(context)
        etSearch.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence,
                    start: Int,
                    before: Int,
                    count: Int,
                ) {
                }

                @SuppressLint("NotifyDataSetChanged")
                override fun afterTextChanged(s: Editable) {
                    val string = s.toString()
                    selectedCountries.clear()
                    for (country in allCountries) {
                        if (country.name
                                .lowercase(Locale.getDefault())
                                .contains(string.lowercase(Locale.getDefault()))
                        ) {
                            selectedCountries.add(country)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
            },
        )
        return root
    }
}
