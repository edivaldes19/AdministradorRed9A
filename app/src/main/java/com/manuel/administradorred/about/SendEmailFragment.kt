package com.manuel.administradorred.about

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.manuel.administradorred.R
import com.manuel.administradorred.databinding.FragmentSendAnEmailBinding
import com.manuel.administradorred.utils.JavaMailAPI
import com.manuel.administradorred.utils.TextWatchers

class SendEmailFragment : DialogFragment(), DialogInterface.OnShowListener {
    private var binding: FragmentSendAnEmailBinding? = null
    private var sendButton: MaterialButton? = null
    private var cancelButton: MaterialButton? = null
    private val errorSnack: Snackbar by lazy {
        Snackbar.make(binding!!.root, "", Snackbar.LENGTH_SHORT).setTextColor(Color.YELLOW)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            binding = FragmentSendAnEmailBinding.inflate(LayoutInflater.from(context))
            binding?.let { view ->
                sendButton = view.btnSendEmail
                cancelButton = view.btnCancel
                TextWatchers.validateFieldsAsYouType(
                    activity,
                    sendButton!!,
                    view.etEmail,
                    view.etMessage
                )
                val builder =
                    MaterialAlertDialogBuilder(activity).setTitle(getString(R.string.post_comments))
                        .setView(view.root)
                val dialog = builder.create()
                dialog.setOnShowListener(this)
                return dialog
            }
        }
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onShow(dialogInterface: DialogInterface?) {
        val dialog = dialog as? AlertDialog
        dialog?.let { alertDialog ->
            alertDialog.setCancelable(false)
            alertDialog.setCanceledOnTouchOutside(false)
            binding?.let { view ->
                val user = FirebaseAuth.getInstance().currentUser
                view.etEmail.setText(user?.email)
                view.cbAnotherEmail.setOnCheckedChangeListener { _, isChecked ->
                    view.tilEmail.isEnabled = isChecked
                }
                var subject = ""
                view.radioGroup.setOnCheckedChangeListener { group, checkedId ->
                    val radio: MaterialRadioButton = group.findViewById(checkedId)
                    subject = "${
                        radio.text.toString().trim()
                    } ${getString(R.string.of)} ${getString(R.string.app_name)}"
                }
                sendButton?.setOnClickListener {
                    if (view.rbComplain.isChecked || view.rbSuggestion.isChecked) {
                        val javaMailAPI = JavaMailAPI(
                            context,
                            view.etEmail.text.toString().trim(),
                            subject,
                            view.etMessage.text.toString().trim(),
                            view.progressBar,
                            alertDialog
                        )
                        javaMailAPI.execute()
                        cancelButton?.isEnabled = false
                        sendButton?.isEnabled = false
                    } else {
                        errorSnack.apply {
                            setText(getString(R.string.you_must_select_the_subject_type))
                            show()
                        }
                    }
                }
            }
            cancelButton?.setOnClickListener {
                alertDialog.dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}