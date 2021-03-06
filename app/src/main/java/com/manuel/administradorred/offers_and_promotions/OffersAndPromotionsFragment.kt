package com.manuel.administradorred.offers_and_promotions

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.manuel.administradorred.R
import com.manuel.administradorred.databinding.FragmentOffersAndPromotionsBinding
import com.manuel.administradorred.fcm.NotificationRS
import com.manuel.administradorred.utils.Constants
import com.manuel.administradorred.utils.TextWatchers
import java.io.ByteArrayOutputStream

class OffersAndPromotionsFragment : DialogFragment(), DialogInterface.OnShowListener {
    private var binding: FragmentOffersAndPromotionsBinding? = null
    private var fabAdd: FloatingActionButton? = null
    private var fabCancel: FloatingActionButton? = null
    private var photoSelectedUri: Uri? = null
    private val snackBar: Snackbar by lazy {
        Snackbar.make(binding!!.root, "", Snackbar.LENGTH_SHORT).setTextColor(Color.YELLOW)
    }
    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            if (activityResult.resultCode == Activity.RESULT_OK) {
                photoSelectedUri = activityResult.data?.data
                binding?.let { view ->
                    Glide.with(this).load(photoSelectedUri).diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(view.imgPackageServicePreview)
                }
            }
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            binding = FragmentOffersAndPromotionsBinding.inflate(LayoutInflater.from(context))
            binding?.let { view ->
                fabAdd = view.fabAdd
                fabCancel = view.fabCancel
                TextWatchers.validateFieldsAsYouType(
                    activity,
                    fabAdd!!,
                    view.etTitle,
                    view.etDescription
                )
                val builder =
                    MaterialAlertDialogBuilder(activity).setTitle(getString(R.string.new_promotion))
                        .setView(view.root)
                val dialog = builder.create()
                dialog.setOnShowListener(this)
                return dialog
            }
        }
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onShow(dialogInterface: DialogInterface?) {
        setupButtons()
        val dialog = dialog as? AlertDialog
        dialog?.let { alertDialog ->
            alertDialog.setCanceledOnTouchOutside(false)
            fabAdd?.setOnClickListener {
                binding?.let {
                    enableAllInterface(false)
                    uploadCompressedImage()
                }
            }
            fabCancel?.setOnClickListener {
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun setupButtons() {
        binding?.let { view ->
            view.ibPackageService.setOnClickListener {
                openGallery()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(intent)
    }

    @SuppressLint("SetTextI18n")
    private fun uploadCompressedImage() {
        photoSelectedUri?.let { uri ->
            binding?.let { binding ->
                getBitmapFromUri(uri)?.let { bitmap ->
                    binding.progressBar.visibility = View.VISIBLE
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
                    val promoRef =
                        Firebase.storage.reference.child(Constants.PROP_OFFERS_AND_PROMOTIONS)
                            .child(binding.tvTopic.text.toString().trim())
                    promoRef.putBytes(byteArrayOutputStream.toByteArray())
                        .addOnProgressListener { taskSnapshot ->
                            val progress =
                                (100 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                            taskSnapshot.run {
                                binding.progressBar.progress = progress
                                binding.tvProgress.text = "${getString(R.string.uploading_image)} ${
                                    String.format(
                                        "%s%%",
                                        progress
                                    )
                                }"
                            }
                        }.addOnSuccessListener { taskSnapshot ->
                            taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUrl ->
                                val notificationRS = NotificationRS()
                                notificationRS.sendNotificationByTopic(
                                    binding.etTitle.text.toString().trim(),
                                    binding.etDescription.text.toString().trim(),
                                    binding.tvTopic.text.toString().trim(),
                                    downloadUrl.toString()
                                ) { success ->
                                    if (success) {
                                        Toast.makeText(
                                            activity,
                                            getString(R.string.promotion_sent_to_all_users),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        dismiss()
                                    } else {
                                        snackBar.apply {
                                            setText(getString(R.string.failed_to_send_the_promotion))
                                            show()
                                        }
                                    }
                                    enableAllInterface(true)
                                }
                            }
                        }.addOnFailureListener {
                            snackBar.apply {
                                setText(getString(R.string.image_upload_error))
                                show()
                            }
                            enableAllInterface(true)
                        }
                }
            }
        }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        activity?.let { fragmentActivity ->
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(fragmentActivity.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(fragmentActivity.contentResolver, uri)
            }
            return getResizedImage(bitmap)
        }
        return null
    }

    private fun getResizedImage(image: Bitmap): Bitmap {
        var width = image.width
        var height = image.height
        if (width <= 480 && height <= 480) return image
        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = 480
            height = (width / bitmapRatio).toInt()
        } else {
            height = 480
            width = (height / bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
    }

    private fun enableAllInterface(enable: Boolean) {
        fabAdd?.isEnabled = enable
        fabCancel?.isEnabled = enable
        binding?.let { view ->
            with(view) {
                etTitle.isEnabled = enable
                etDescription.isEnabled = enable
            }
        }
    }
}