package com.sahooz.library.countrypicker

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.sahooz.library.countrypicker.Country.Companion.getAll
import com.sahooz.library.countrypicker.databinding.ActivityPickBinding
import com.sahooz.library.countrypicker.view.SideBar
import java.util.Locale

@Deprecated("不建议App直接使用此Activity，只做使用参考")
class PickActivity : AppCompatActivity() {
    lateinit var binding: ActivityPickBinding

    private val selectedCountries = ArrayList<Country>()
    private val allCountries = ArrayList<Country>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPickBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivActivityBack.setOnClickListener {
            finish()
        }
        allCountries.clear()
        allCountries.addAll(getAll())
        selectedCountries.clear()
        selectedCountries.addAll(allCountries)

        val adapter = CAdapter(this, selectedCountries)
        adapter.callback =
            object : PickCountryCallback {
                override fun onPick(country: Country) {
                    setResult(RESULT_OK, intent.putExtra("country", country.toJson()))
                    finish()
                }
            }
        binding.rvPick.setLayoutManager(LinearLayoutManager(this))
        binding.rvPick.setAdapter(adapter)

        binding.etSearch.addTextChangedListener(
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

                override fun afterTextChanged(s: Editable) {
                    val string = s.toString()
                    selectedCountries.clear()
                    for (country in allCountries) {
                        if (country.name
                                .lowercase(Locale.getDefault())
                                .contains(string.lowercase(Locale.getDefault())) ||
                            country.code.toString().lowercase(Locale.getDefault()).contains(
                                string.lowercase(
                                    Locale.getDefault(),
                                ),
                            )
                        ) {
                            selectedCountries.add(country)
                        }
                    }
                    adapter.organizeEntities(selectedCountries)
                }
            },
        )

        binding.side.addIndex("#", binding.side.indexes.size)
        binding.side.onLetterChangeListener =
            object : SideBar.OnLetterChangeListener {
                override fun onLetterChange(letter: String?) {
                    if (letter == null) return
                    binding.tvLetter.visibility = View.VISIBLE
                    binding.tvLetter.text = letter
                    val position: Int = adapter.getLetterPosition(letter)
                    if (position != -1) {
                        (binding.rvPick.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(position, 0)
                    }
                }

                override fun onReset() {
                    binding.tvLetter.visibility = View.GONE
                }
            }

        try {
            // 底部导航栏
            window.apply {
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                statusBarColor = resources.getColor(R.color.black)
                navigationBarColor = resources.getColor(R.color.black)
            }
        } catch (_: Exception) {
        }
    }
}
