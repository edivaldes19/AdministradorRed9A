package com.manuel.administradorred.add

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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.manuel.administradorred.R
import com.manuel.administradorred.databinding.FragmentDialogAddBinding
import com.manuel.administradorred.models.EventPost
import com.manuel.administradorred.models.PackageService
import com.manuel.administradorred.package_service.MainAux
import com.manuel.administradorred.utils.Constants
import java.io.ByteArrayOutputStream

class AddDialogFragment : DialogFragment(), DialogInterface.OnShowListener {
    private var binding: FragmentDialogAddBinding? = null
    private var positiveButton: Button? = null
    private var negativeButton: Button? = null
    private var packageService: PackageService? = null
    private var photoSelectedUri: Uri? = null
    private val errorSnack: Snackbar by lazy {
        Snackbar.make(binding!!.root, "", Snackbar.LENGTH_SHORT).setTextColor(Color.RED)
    }
    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            if (activityResult.resultCode == Activity.RESULT_OK) {
                photoSelectedUri = activityResult.data?.data
                binding?.let { fragmentDialogAddBinding ->
                    Glide.with(this).load(photoSelectedUri).diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop().into(fragmentDialogAddBinding.imgPackagePreview)
                }
            }
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            binding = FragmentDialogAddBinding.inflate(LayoutInflater.from(context))
            binding?.let { fragmentDialogAddBinding ->
                val builder =
                    MaterialAlertDialogBuilder(activity).setTitle(getString(R.string.add_package))
                        .setPositiveButton(getString(R.string.add), null)
                        .setNegativeButton(getString(R.string.cancel), null)
                        .setView(fragmentDialogAddBinding.root)
                val dialog = builder.create()
                dialog.setOnShowListener(this)
                return dialog
            }
        }
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onShow(dialogInterface: DialogInterface?) {
        initPackage()
        configButtons()
        val dialog = dialog as? AlertDialog
        dialog?.let { alertDialog ->
            positiveButton = alertDialog.getButton(Dialog.BUTTON_POSITIVE)
            negativeButton = alertDialog.getButton(Dialog.BUTTON_NEGATIVE)
            packageService?.let { positiveButton?.text = getString(R.string.update) }
            positiveButton?.setOnClickListener {
                binding?.let { fragmentDialogAddBinding ->
                    enableUI(false)
                    uploadReducedImage(packageService?.id, packageService?.imagePath) { eventPost ->
                        if (eventPost.isSuccess) {
                            if (packageService == null) {
                                val packageService = PackageService(
                                    name = fragmentDialogAddBinding.etName.text.toString().trim(),
                                    description = fragmentDialogAddBinding.etDescription.text.toString()
                                        .trim(),
                                    availables = fragmentDialogAddBinding.etAvailables.text.toString()
                                        .trim().toInt(),
                                    price = fragmentDialogAddBinding.etPrice.text.toString().trim()
                                        .toInt(),
                                    speed = fragmentDialogAddBinding.etSpeed.text.toString().trim()
                                        .toInt(),
                                    limit = fragmentDialogAddBinding.etLimit.text.toString().trim()
                                        .toInt(),
                                    validity = fragmentDialogAddBinding.etValidity.text.toString()
                                        .toInt(),
                                    imagePath = eventPost.imagePath,
                                    administratorId = eventPost.administratorId
                                )
                                save(packageService, eventPost.documentId!!)
                            } else {
                                packageService?.apply {
                                    name = fragmentDialogAddBinding.etName.text.toString().trim()
                                    description =
                                        fragmentDialogAddBinding.etDescription.text.toString()
                                            .trim()
                                    availables =
                                        fragmentDialogAddBinding.etAvailables.text.toString()
                                            .trim().toInt()
                                    price = fragmentDialogAddBinding.etPrice.text.toString().trim()
                                        .toInt()
                                    speed = fragmentDialogAddBinding.etSpeed.text.toString().trim()
                                        .toInt()
                                    limit = fragmentDialogAddBinding.etLimit.text.toString().trim()
                                        .toInt()
                                    validity = fragmentDialogAddBinding.etValidity.text.toString()
                                        .toInt()
                                    imagePath = eventPost.imagePath
                                    update(this)
                                }
                            }
                        }
                    }
                }
            }
            negativeButton?.setOnClickListener {
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun initPackage() {
        packageService = (activity as? MainAux)?.getPackageServiceSelected()
        packageService?.let { packageService ->
            binding?.let { fragmentDialogAddBinding ->
                dialog?.setTitle(getString(R.string.update_package))
                fragmentDialogAddBinding.etName.setText(packageService.name)
                fragmentDialogAddBinding.etDescription.setText(packageService.description)
                fragmentDialogAddBinding.etAvailables.setText(packageService.availables.toString())
                fragmentDialogAddBinding.etPrice.setText(packageService.price.toString())
                fragmentDialogAddBinding.etSpeed.setText(packageService.speed.toString())
                fragmentDialogAddBinding.etLimit.setText(packageService.limit.toString())
                fragmentDialogAddBinding.etValidity.setText(packageService.validity.toString())
                Glide.with(this).load(packageService.imagePath)
                    .diskCacheStrategy(DiskCacheStrategy.ALL).centerCrop()
                    .into(fragmentDialogAddBinding.imgPackagePreview)
            }
        }
    }

    private fun configButtons() {
        binding?.let { fragmentDialogAddBinding ->
            fragmentDialogAddBinding.ibPackageService.setOnClickListener {
                openGallery()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(intent)
    }

    @SuppressLint("SetTextI18n")
    private fun uploadReducedImage(
        packageServiceId: String?,
        imageUrl: String?,
        callback: (EventPost) -> Unit
    ) {
        val eventPost = EventPost()
        imageUrl?.let { eventPost.imagePath = it }
        eventPost.documentId = packageServiceId ?: FirebaseFirestore.getInstance()
            .collection(Constants.COLL_PACKAGE_SERVICE).document().id
        FirebaseAuth.getInstance().currentUser?.let { user ->
            val imagesRef = FirebaseStorage.getInstance().reference.child(user.uid)
                .child(Constants.PATH_PACKAGE_SERVICE_IMAGES)
            val photoRef = imagesRef.child(eventPost.documentId!!).child("image0")
            eventPost.administratorId = user.uid
            if (photoSelectedUri == null) {
                eventPost.isSuccess = true
                callback(eventPost)
            } else {
                binding?.let { binding ->
                    getBitmapFromUri(photoSelectedUri!!)?.let { bitmap ->
                        binding.progressBar.visibility = View.VISIBLE
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, byteArrayOutputStream)
                        photoRef.putBytes(byteArrayOutputStream.toByteArray())
                            .addOnProgressListener { taskSnapshot ->
                                val progress =
                                    (100 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                                taskSnapshot.run {
                                    binding.progressBar.progress = progress
                                    binding.tvProgress.text =
                                        "${getString(R.string.uploading_image)} ${
                                            String.format(
                                                "%s%%",
                                                progress
                                            )
                                        }"
                                }
                            }.addOnSuccessListener { taskSnapshot ->
                                taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUrl ->
                                    Log.i("URL", downloadUrl.toString())
                                    eventPost.isSuccess = true
                                    eventPost.imagePath = downloadUrl.toString()
                                    callback(eventPost)
                                }
                            }.addOnFailureListener {
                                errorSnack.apply {
                                    setText(getString(R.string.error_uploading_image))
                                    show()
                                }
                                enableUI(true)
                                eventPost.isSuccess = false
                                callback(eventPost)
                            }
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
        if (width <= 500 && height <= 500) return image
        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = 500
            height = (width / bitmapRatio).toInt()
        } else {
            height = 500
            width = (height / bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
    }

    private fun save(packageService: PackageService, documentId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection(Constants.COLL_PACKAGE_SERVICE)
            .document(documentId)
            .set(packageService)
            .addOnSuccessListener {
                Toast.makeText(activity, getString(R.string.package_added), Toast.LENGTH_SHORT)
                    .show()
                dismiss()
            }
            .addOnFailureListener {
                errorSnack.apply {
                    setText(getString(R.string.failed_to_add_package))
                    show()
                }
                enableUI(true)
            }
            .addOnCompleteListener {
                binding?.progressBar?.visibility = View.INVISIBLE
            }
    }

    private fun update(packageService: PackageService) {
        val db = FirebaseFirestore.getInstance()
        packageService.id?.let { id ->
            db.collection(Constants.COLL_PACKAGE_SERVICE).document(id).set(packageService)
                .addOnSuccessListener {
                    Toast.makeText(
                        activity,
                        getString(R.string.package_updated),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .addOnFailureListener {
                    errorSnack.apply {
                        setText(getString(R.string.failed_to_update_package))
                        show()
                    }
                }
                .addOnCompleteListener {
                    enableUI(true)
                    binding?.progressBar?.visibility = View.INVISIBLE
                    dismiss()
                }
        }
    }

    private fun enableUI(enable: Boolean) {
        positiveButton?.isEnabled = enable
        negativeButton?.isEnabled = enable
        binding?.let { fragmentDialogAddBinding ->
            with(fragmentDialogAddBinding) {
                etName.isEnabled = enable
                etDescription.isEnabled = enable
                etAvailables.isEnabled = enable
                etPrice.isEnabled = enable
                etSpeed.isEnabled = enable
                etLimit.isEnabled = enable
                etValidity.isEnabled = enable
                progressBar.visibility = if (enable) View.INVISIBLE else View.VISIBLE
                tvProgress.visibility = if (enable) View.INVISIBLE else View.VISIBLE
            }
        }
    }
}