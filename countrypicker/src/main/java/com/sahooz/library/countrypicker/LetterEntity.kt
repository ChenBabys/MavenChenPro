package com.sahooz.library.countrypicker

import java.util.Locale

/**
 * 首字母实体
 * @property letter
 */
class LetterEntity(val letter: String) : PyEntity {
    override fun getPinyin(): String = letter.lowercase(Locale.getDefault())

    override fun hashCode(): Int = letter.lowercase(Locale.getDefault()).hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LetterEntity

        return letter == other.letter
    }
}
