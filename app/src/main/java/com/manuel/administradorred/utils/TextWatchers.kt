package com.manuel.administradorred.utils

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.manuel.administradorred.R

class TextWatchers {
    companion object {
        fun validateFieldsAsYouType(
            context: Context,
            materialButton: MaterialButton,
            vararg textInputEditText: TextInputEditText
        ) {
            textInputEditText.forEach { text ->
                text.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        if (text.text.toString().trim().isEmpty()) {
                            text.error = context.getString(R.string.this_field_is_required)
                            materialButton.isEnabled = false
                        } else {
                            text.error = null
                        }
                    }

                    override fun afterTextChanged(s: Editable?) {
                        textInputEditText.forEach { editText ->
                            materialButton.isEnabled = editText.text.toString().trim().isNotEmpty()
                        }
                    }
                })
            }
        }
    }
}