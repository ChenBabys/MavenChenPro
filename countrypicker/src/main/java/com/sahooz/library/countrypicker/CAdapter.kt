package com.sahooz.library.countrypicker

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sahooz.library.countrypicker.utils.CoverUtil
import java.util.Locale

class CAdapter(private val ctx: Context, entities: List<out PyEntity>) : PyAdapter<RecyclerView.ViewHolder>(entities, '#') {
    var callback: PickCountryCallback? = null

    override fun onCreateLetterHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder =
        LetterHolder(
            LayoutInflater.from(ctx).inflate(R.layout.item_letter, parent, false),
        )

    override fun onCreateHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder =
        VH(
            LayoutInflater.from(ctx).inflate(R.layout.item_country_large_padding, parent, false),
        )

    @SuppressLint("SetTextI18n")
    override fun onBindHolder(
        holder: RecyclerView.ViewHolder,
        entity: PyEntity,
        position: Int,
    ) {
        val vh = holder as VH
        val country = entity as Country
        vh.tvFlag.text = CoverUtil.localeToEmoji(country.locale) + ""
        vh.tvName.text = country.name
        vh.tvCode.text = "+${country.code}"
        vh.itemView.setOnClickListener {
            callback?.onPick(country)
        }
    }

    override fun onBindLetterHolder(
        holder: RecyclerView.ViewHolder,
        entity: LetterEntity,
        position: Int,
    ) {
        ((holder as LetterHolder).itemView as? TextView)?.text = entity.letter.uppercase(Locale.getDefault())
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvName: TextView = itemView.findViewById(R.id.tv_name)
        var tvCode: TextView = itemView.findViewById(R.id.tv_code)
        var tvFlag: TextView = itemView.findViewById(R.id.tv_flag)
    }

    internal class LetterHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
