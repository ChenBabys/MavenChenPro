package com.sahooz.library.countrypicker

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 国家信息实体
 * 注意 荷屬安德列斯群島已经分裂为三个国家，但其中两个都还用599（一个是599，一个是599-9）区号，所以保留
 * @property name 国家名称
 * @property code 国家区号，如86，1，599
 * @property pinYin 国家名称的拼音
 * @property locale 国家代号，如CN，US
 * @constructor Create empty Country
 */
class Country(val name: String, val code: Int, val pinYin: String, val locale: String) : PyEntity {
    // 在列表的位置，默认为中间
    var itemPosition: Int = ItemPosition.Middle.value

    enum class ItemPosition(val value: Int) {
        Top(0x01), // 列表头
        Bottom(0x02), // 列表尾
        Middle(0x4), // 列表中
    }

    companion object {
        // 记录当前语言
        var currentLanguage: Language = Language.ENGLISH
        private var countries = mutableListOf<Country>()

        @JvmStatic
        fun fromJson(json: String): Country? {
            if (json.isEmpty()) return null
            try {
                val jo = JSONObject(json)
                return Country(
                    name = jo.optString("name"),
                    code = jo.optInt("code"),
                    pinYin = jo.optString("pinyin"),
                    locale = jo.optString("locale"),
                )
            } catch (_: Exception) {
            }
            return null
        }

        @JvmStatic
        fun getAll(): List<Country> = countries

        @JvmStatic
        fun load(
            ctx: Context,
            language: Language,
        ) {
            load(ctx, language, "")
        }

        /**
         * 加载国家数据
         *
         *  [json]字段的值App自己去下载后台的国家数据并存储起来，然后传进来。如果不传就用本地的assets目录下的code.json
         *
         * @param ctx
         * @param language
         * @param json 如果不为空就用该json的国家数据，不拿本地assets目录下的code.json
         */
        @JvmStatic
        fun load(
            ctx: Context,
            language: Language,
            json: String = "",
        ) {
            currentLanguage = language
            val jsonString =
                json.ifEmpty {
                    ctx.assets.open("code.json").bufferedReader().useLines { lines ->
                        lines.joinToString("\n")
                    }
                }
            val ja = JSONArray(jsonString)
            val key = language.key

            countries =
                (0 until ja.length())
                    .map { i ->
                        val jo = ja.getJSONObject(i)
                        Country(
                            code = jo.getInt("code"),
                            name = jo.getString(key),
                            pinYin =
                                if (language == Language.ENGLISH) {
                                    jo.getString(key)
                                } else {
                                    jo.getString(
                                        "pinyin",
                                    )
                                },
                            locale = jo.getString("locale"),
                        )
                    }.toMutableList()
        }

        @JvmStatic
        fun destroy() {
            countries.clear()
        }
    }

    override fun getPinyin(): String = pinYin

    override fun hashCode(): Int = code

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Country
        if (name != other.name) return false
        if (code != other.code) return false
        if (pinYin != other.pinYin) return false
        if (locale != other.locale) return false

        return true
    }

    fun toJson(): String = "{\"name\":\"$name\", \"code\":$code, \"pinyin\":\"$pinYin\",\"locale\":\"$locale\"}"
}
