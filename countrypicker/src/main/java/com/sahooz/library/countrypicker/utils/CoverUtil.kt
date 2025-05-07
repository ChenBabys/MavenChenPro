package com.sahooz.library.countrypicker.utils

object CoverUtil {
    /**
     * 将国家名称的缩写转成国旗emoji
     *
     * @param localeStr
     * @return
     */
    fun localeToEmoji(localeStr: String): String {
        val firstLetter = Character.codePointAt(localeStr, 0) - 0x41 + 0x1F1E6
        val secondLetter = Character.codePointAt(localeStr, 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
    }
}
