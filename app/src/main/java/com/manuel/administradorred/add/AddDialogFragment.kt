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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.manuel.administradorred.R
import com.manuel.administradorred.databinding.FragmentDialogAddBinding
import com.manuel.administradorred.models.EventPost
import com.manuel.administradorred.models.PackageService
import com.manuel.administradorred.package_service.OnPackageServiceSelected
import com.manuel.administradorred.utils.Constants
import com.manuel.administradorred.utils.TextWatchers
import com.manuel.administradorred.utils.TimestampToText
import java.io.ByteArrayOutputStream
import java.util.*

class AddDialogFragment : DialogFragment(), DialogInterface.OnShowListener {
    private var binding: FragmentDialogAddBinding? = null
    private var fabAdd: FloatingActionButton? = null
    private var fabCancel: FloatingActionButton? = null
    private var packageService: PackageService? = null
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
                        .into(view.imgPackagePreview)
                }
            }
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            binding = FragmentDialogAddBinding.inflate(LayoutInflater.from(context))
            binding?.let { view ->
                fabAdd = view.fabAdd
                fabCancel = view.fabCancel
                TextWatchers.validateFieldsAsYouType(
                    activity,
                    fabAdd!!,
                    view.etName,
                    view.etDescription,
                    view.etAvailables,
                    view.etPrice,
                    view.etSpeed,
                    view.etLimit,
                    view.etValidity
                )
                val builder =
                    MaterialAlertDialogBuilder(activity).setTitle(getString(R.string.add_package))
                        .setView(view.root)
                val dialog = builder.create()
                dialog.setOnShowListener(this)
                return dialog
            }
        }
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onShow(dialogInterface: DialogInterface?) {
        initPackage()
        setupButtons()
        val dialog = dialog as? AlertDialog
        dialog?.let { alertDialog ->
            alertDialog.setCanceledOnTouchOutside(false)
            packageService?.let {
                fabAdd?.setImageResource(R.drawable.ic_edit)
            }
            fabAdd?.setOnClickListener {
                binding?.let { view ->
                    enableAllInterface(false)
                    uploadCompressedImage(
                        packageService?.id,
                        packageService?.imagePath
                    ) { eventPost ->
                        if (eventPost.isSuccess) {
                            if (!theyAreEmpty()) {
                                if (packageService == null) {
                                    val packageService = PackageService(
                                        name = view.etName.text.toString().trim(),
                                        description = view.etDescription.text.toString().trim(),
                                        available = view.etAvailables.text.toString().trim()
                                            .toInt(),
                                        price = view.etPrice.text.toString().trim().toInt(),
                                        speed = view.etSpeed.text.toString().trim().toInt(),
                                        limit = view.etLimit.text.toString().trim().toInt(),
                                        validity = view.etValidity.text.toString().trim().toInt(),
                                        imagePath = eventPost.imagePath,
                                        administratorId = eventPost.administratorId,
                                        lastModification = Date().time
                                    )
                                    save(packageService, eventPost.documentId!!)
                                } else {
                                    packageService?.apply {
                                        name = view.etName.text.toString().trim()
                                        description = view.etDescription.text.toString().trim()
                                        available = view.etAvailables.text.toString().trim().toInt()
                                        price = view.etPrice.text.toString().trim().toInt()
                                        speed = view.etSpeed.text.toString().trim().toInt()
                                        limit = view.etLimit.text.toString().trim().toInt()
                                        validity = view.etValidity.text.toString().trim().toInt()
                                        imagePath = eventPost.imagePath
                                        lastModification = Date().time
                                        update(this)
                                    }
                                }
                            } else {
                                enableAllInterface(true)
                                snackBar.apply {
                                    setText(getString(R.string.there_are_still_empty_fields))
                                    show()
                                }
                            }
                        }
                    }
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

    @SuppressLint("SetTextI18n")
    private fun initPackage() {
        packageService = (activity as? OnPackageServiceSelected)?.getPackageServiceSelected()
        packageService?.let { packageService ->
            binding?.let { view ->
                dialog?.setTitle(getString(R.string.update_package))
                view.etName.setText(packageService.name)
                view.etDescription.setText(packageService.description)
                view.etAvailables.setText(packageService.available.toString())
                view.etPrice.setText(packageService.price.toString())
                view.etSpeed.setText(packageService.speed.toString())
                view.etLimit.setText(packageService.limit.toString())
                view.etValidity.setText(packageService.validity.toString())
                view.tvLastModification.text = "${getString(R.string.last_update)}: ${
                    TimestampToText.getTimeAgo(packageService.lastModification)
                        .lowercase(Locale.getDefault())
                }."
                Glide.with(this).load(packageService.imagePath)
                    .diskCacheStrategy(DiskCacheStrategy.ALL).into(view.imgPackagePreview)
            }
        }
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
    private fun uploadCompressedImage(
        packageServiceId: String?,
        imageUrl: String?,
        callback: (EventPost) -> Unit
    ) {
        val eventPost = EventPost()
        imageUrl?.let { path -> eventPost.imagePath = path }
        eventPost.documentId =
            packageServiceId ?: Firebase.firestore.collection(Constants.COLL_PACKAGE_SERVICE)
                .document().id
        FirebaseAuth.getInstance().currentUser?.let { user ->
            val imagesRef = Firebase.storage.reference.child(user.uid)
                .child(Constants.PATH_PACKAGE_SERVICE_IMAGES)
            val photoRef = imagesRef.child(eventPost.documentId!!).child("image0")
            eventPost.administratorId = user.uid
            if (photoSelectedUri == null) {
                eventPost.isSuccess = true
                callback(eventPost)
            } else {
                binding?.let { view ->
                    getBitmapFromUri(photoSelectedUri!!)?.let { bitmap ->
                        view.progressBar.visibility = View.VISIBLE
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
                        photoRef.putBytes(byteArrayOutputStream.toByteArray())
                            .addOnProgressListener { taskSnapshot ->
                                val progress =
                                    (100 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                                taskSnapshot.run {
                                    view.progressBar.progress = progress
                                    view.tvProgress.text =
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
                                snackBar.apply {
                                    setText(getString(R.string.error_uploading_image))
                                    show()
                                }
                                enableAllInterface(true)
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
        if (width <= 320 && height <= 320) return image
        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = 320
            height = (width / bitmapRatio).toInt()
        } else {
            height = 320
            width = (height / bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
    }

    private fun save(packageService: PackageService, documentId: String) {
        val db = Firebase.firestore
        db.collection(Constants.COLL_PACKAGE_SERVICE).document(documentId).set(packageService)
            .addOnSuccessListener {
                Toast.makeText(activity, getString(R.string.package_added), Toast.LENGTH_SHORT)
                    .show()
                dismiss()
            }.addOnFailureListener {
                snackBar.apply {
                    setText(getString(R.string.failed_to_add_package))
                    show()
                }
            }.addOnCompleteListener {
                enableAllInterface(true)
                binding?.progressBar?.visibility = View.INVISIBLE
            }
    }

    private fun update(packageService: PackageService) {
        val db = Firebase.firestore
        packageService.id?.let { id ->
            db.collection(Constants.COLL_PACKAGE_SERVICE).document(id).set(packageService)
                .addOnSuccessListener {
                    Toast.makeText(
                        activity,
                        getString(R.string.package_updated),
                        Toast.LENGTH_SHORT
                    ).show()
                    dismiss()
                }.addOnFailureListener {
                    snackBar.apply {
                        setText(getString(R.string.failed_to_update_package))
                        show()
                    }
                }.addOnCompleteListener {
                    enableAllInterface(true)
                    binding?.progressBar?.visibility = View.INVISIBLE
                }
        }
    }

    private fun enableAllInterface(enable: Boolean) {
        fabAdd?.isEnabled = enable
        fabCancel?.isEnabled = enable
        binding?.let { view ->
            with(view) {
                etName.isEnabled = enable
                etDescription.isEnabled = enable
                etAvailables.isEnabled = enable
                etPrice.isEnabled = enable
                etSpeed.isEnabled = enable
                etLimit.isEnabled = enable
                etValidity.isEnabled = enable
                progressBar.visibility = if (enable) {
                    View.INVISIBLE
                } else {
                    View.VISIBLE
                }
                tvProgress.visibility = if (enable) {
                    View.INVISIBLE
                } else {
                    View.VISIBLE
                }
            }
        }
    }

    private fun theyAreEmpty(): Boolean {
        binding?.let { view ->
            with(view) {
                return etName.text.isNullOrEmpty() || etDescription.text.isNullOrEmpty() ||
                        etAvailables.text.isNullOrEmpty() || etPrice.text.isNullOrEmpty() ||
                        etSpeed.text.isNullOrEmpty() || etLimit.text.isNullOrEmpty() ||
                        etValidity.text.isNullOrEmpty()
            }
        }
        return false
    }
}