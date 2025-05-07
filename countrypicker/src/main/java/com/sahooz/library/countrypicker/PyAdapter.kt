package com.sahooz.library.countrypicker

import android.annotation.SuppressLint
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sahooz.library.countrypicker.utils.Logger
import java.util.Locale
import java.util.WeakHashMap

abstract class PyAdapter<VH : RecyclerView.ViewHolder>(entities: List<PyEntity>, private val specialLetter: Char) :
    RecyclerView.Adapter<VH>(),
    View.OnClickListener {
    private val holders = WeakHashMap<View, VH>()
    protected val entityList: ArrayList<PyEntity> = ArrayList()
    private val letterSet: HashSet<LetterEntity> = HashSet()

    interface OnItemClickListener {
        fun onItemClick(
            entity: PyEntity?,
            position: Int,
        )
    }

    enum class ItemType {
        TYPE_LETTER,
        TYPE_OTHER,
    }

    private var listener: OnItemClickListener? = null

    init {
        organizeEntities(entities)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): VH =
        if (viewType == ItemType.TYPE_LETTER.ordinal) {
            onCreateLetterHolder(parent, viewType)
        } else {
            onCreateHolder(parent, viewType)
        }

    override fun onBindViewHolder(
        holder: VH,
        position: Int,
    ) {
        val entity = entityList[position]
        holders[holder.itemView] = holder
        holder.itemView.setOnClickListener(this)
        if (entity is LetterEntity) {
            onBindLetterHolder(holder, entity, position)
        } else {
            onBindHolder(holder, entity, position)
        }
    }

    override fun getItemCount(): Int = entityList.size

    override fun getItemViewType(position: Int): Int {
        val entity = entityList[position]
        return if (entity is LetterEntity) ItemType.TYPE_LETTER.ordinal else ItemType.TYPE_OTHER.ordinal
    }

    abstract fun onCreateLetterHolder(
        parent: ViewGroup,
        viewType: Int,
    ): VH

    abstract fun onCreateHolder(
        parent: ViewGroup,
        viewType: Int,
    ): VH

    abstract fun onBindHolder(
        holder: RecyclerView.ViewHolder,
        entity: PyEntity,
        position: Int,
    )

    abstract fun onBindLetterHolder(
        holder: RecyclerView.ViewHolder,
        entity: LetterEntity,
        position: Int,
    )

    override fun onClick(v: View?) {
        val holder = holders[v]
        if (holder == null) {
            Logger.log("Holder onClick event, but why holder == null?")
            return
        }
        val position = holder.bindingAdapterPosition
        listener?.onItemClick(entityList[position], position)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun organizeEntities(entities: List<PyEntity>?) {
        if (entities == null) throw NullPointerException("entities == null!")
        entityList.clear()
        // 添加所有拼音实体
        entityList.addAll(entities)
        letterSet.clear()
        // 收集所有拼音实体的首字母
        // 遍历所有实体，获取每个实体的拼音首字母。如果首字母不是英文字母，则使用特殊字符（specialLetter）替代。然后将这些字母添加到 letterSet中。
        for (entity in entities) {
            val pinyin = entity.getPinyin()
            if (!TextUtils.isEmpty(pinyin)) {
                var letter = pinyin[0]
                if (!isLetter(letter)) letter = specialLetter
                letterSet.add(LetterEntity(letter.toString() + ""))
            }
        }
        // 将所有首字母实体添加到实体列表中
        entityList.addAll(letterSet)
        // 对实体列表进行排序，排序规则如下：
        // 使用自定义的排序规则对列表进行排序，排序规则如下：
        entityList.sortWith sort@{ o1: PyEntity, o2: PyEntity ->
            // sortWith方法返回-1表示o1排在o2前面，返回1表示o1排在o2后面，返回0表示o1和o2相等。
            val pinyin = o1.getPinyin().lowercase(Locale.getDefault())
            val anotherPinyin = o2.getPinyin().lowercase(Locale.getDefault())
            val letter = pinyin[0]
            val otherLetter = anotherPinyin[0]
            when {
                // 如果两个实体都是字母开头，按拼音字母顺序排序
                isLetter(letter) && isLetter(otherLetter) -> {
                    return@sort pinyin.compareTo(
                        anotherPinyin,
                    )
                }

                // 如果一个是字母开头，另一个不是，字母开头的排在前面
                isLetter(letter) && !isLetter(otherLetter) -> {
                    return@sort -1
                }

                !isLetter(letter) && isLetter(otherLetter) -> {
                    return@sort 1
                }

                else -> {
                    // 如果都不是字母开头：
                    when {
                        // 如果是特殊字母（specialLetter）且是字母实体，排在前面
                        letter == specialLetter && o1 is LetterEntity -> {
                            return@sort -1
                        }

                        otherLetter == specialLetter && o2 is LetterEntity -> {
                            return@sort 1
                        }

                        else -> {
                            // 否则按拼音字母顺序排序
                            return@sort pinyin.compareTo(anotherPinyin)
                        }
                    }
                }
            }
        }
        organizeEntitiesCompleted()
    }

    protected open fun organizeEntitiesCompleted() {
        notifyDataSetChanged()
    }

    private fun isLetter(letter: Char): Boolean = letter in 'a'..'z' || letter in 'A'..'Z'

    fun getLetterPosition(letter: String): Int = entityList.indexOf(LetterEntity(letter))
}
