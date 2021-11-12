package com.manuel.administradorred.about

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.manuel.administradorred.R
import com.manuel.administradorred.databinding.FragmentSendAnEmailBinding
import com.manuel.administradorred.utils.JavaMailAPI
import com.manuel.administradorred.utils.TextWatchers

class SendEmailFragment : DialogFragment(), DialogInterface.OnShowListener {
    private var binding: FragmentSendAnEmailBinding? = null
    private var fabSendEmail: FloatingActionButton? = null
    private var fabCancel: FloatingActionButton? = null
    private val snackBar: Snackbar by lazy {
        Snackbar.make(binding!!.root, "", Snackbar.LENGTH_SHORT).setTextColor(Color.YELLOW)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            binding = FragmentSendAnEmailBinding.inflate(LayoutInflater.from(context))
            binding?.let { view ->
                fabSendEmail = view.fabSendEmail
                fabCancel = view.fabCancel
                TextWatchers.validateFieldsAsYouType(
                    activity,
                    fabSendEmail!!,
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
                fabSendEmail?.setOnClickListener {
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
                        fabCancel?.isEnabled = false
                        fabSendEmail?.isEnabled = false
                    } else {
                        snackBar.apply {
                            setText(getString(R.string.you_must_select_the_subject_type))
                            show()
                        }
                    }
                }
            }
            fabCancel?.setOnClickListener {
                alertDialog.dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}