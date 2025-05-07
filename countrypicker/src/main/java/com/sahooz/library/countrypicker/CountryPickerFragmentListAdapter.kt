package com.sahooz.library.countrypicker

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sahooz.library.countrypicker.utils.CoverUtil

@Deprecated("")
class CountryPickerFragmentListAdapter(
    val ctx: Context,
) : RecyclerView.Adapter<CAdapter.VH>() {
    private var selectedCountries = mutableListOf<Country>()
    var callback: PickCountryCallback? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): CAdapter.VH = CAdapter.VH(LayoutInflater.from(ctx).inflate(R.layout.item_country, parent, false))

    override fun getItemCount(): Int = selectedCountries.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: CAdapter.VH,
        position: Int,
    ) {
        val country = selectedCountries[position]
        holder.tvFlag.text = CoverUtil.localeToEmoji(country.locale)
        holder.tvName.text = country.name
        holder.tvCode.text = "+" + country.code
        holder.itemView.setOnClickListener {
            callback?.onPick(country)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setSelectedCountries(selectedCountries: MutableList<Country>) {
        this.selectedCountries = selectedCountries
        notifyDataSetChanged()
    }
}
