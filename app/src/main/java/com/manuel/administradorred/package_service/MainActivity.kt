package com.manuel.administradorred.package_service

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.GridLayoutManager
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.ktx.storage
import com.manuel.administradorred.R
import com.manuel.administradorred.about.AboutActivity
import com.manuel.administradorred.add.AddDialogFragment
import com.manuel.administradorred.databinding.ActivityMainBinding
import com.manuel.administradorred.models.PackageService
import com.manuel.administradorred.offers_and_promotions.OffersAndPromotionsFragment
import com.manuel.administradorred.requested_contract.RequestedContractActivity
import com.manuel.administradorred.utils.ConnectionReceiver
import com.manuel.administradorred.utils.Constants

class MainActivity : AppCompatActivity(), OnPackageServiceListener, OnPackageServiceSelected,
    ConnectionReceiver.ReceiverListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private lateinit var packageServiceAdapter: PackageServiceAdapter
    private lateinit var listenerRegistration: ListenerRegistration
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private var packageServiceSelected: PackageService? = null
    private var packageServiceList = mutableListOf<PackageService>()
    private val snackBar: Snackbar by lazy {
        Snackbar.make(binding.root, "", Snackbar.LENGTH_SHORT).setTextColor(Color.YELLOW)
    }
    private val authLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            val response = IdpResponse.fromResultIntent(activityResult.data)
            if (activityResult.resultCode == RESULT_OK) {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    Toast.makeText(
                        this,
                        "${getString(R.string.welcome)} ${user.displayName}",
                        Toast.LENGTH_LONG
                    ).show()
                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN) {
                        param(FirebaseAnalytics.Param.SUCCESS, 100)
                        param(FirebaseAnalytics.Param.METHOD, Constants.PARAM_LOGIN)
                    }
                }
            } else {
                if (response == null) {
                    Toast.makeText(this, getString(R.string.see_you_soon), Toast.LENGTH_SHORT)
                        .show()
                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN) {
                        param(FirebaseAnalytics.Param.SUCCESS, 200)
                        param(FirebaseAnalytics.Param.METHOD, Constants.PARAM_LOGIN)
                    }
                    finishAffinity()
                } else {
                    response.error?.let { firebaseUiException ->
                        if (firebaseUiException.errorCode == ErrorCodes.NO_NETWORK) {
                            snackBar.apply {
                                setText("${getString(R.string.error_code)}: ${firebaseUiException.errorCode}")
                                show()
                            }
                        } else {
                            snackBar.apply {
                                setText(getString(R.string.network_error))
                                show()
                            }
                        }
                        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN) {
                            param(
                                FirebaseAnalytics.Param.SUCCESS,
                                firebaseUiException.errorCode.toLong()
                            )
                            param(FirebaseAnalytics.Param.METHOD, Constants.PARAM_LOGIN)
                        }
                    }
                }
            }
        }
    private var count = 0
    private val uriList = mutableListOf<Uri>()
    private val progressSnack: Snackbar by lazy {
        Snackbar.make(binding.root, "", Snackbar.LENGTH_INDEFINITE)
    }
    private var galleryResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            if (activityResult.resultCode == RESULT_OK) {
                if (activityResult.data?.clipData != null) {
                    count = activityResult.data!!.clipData!!.itemCount
                    for (i in 0 until count) {
                        uriList.add(activityResult.data!!.clipData!!.getItemAt(i).uri)
                    }
                    if (count > 0) {
                        uploadingMultipleImages(0)
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_AdministradorRed)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupAuth()
        setupRecyclerView()
        setupButtons()
        setupAnalytics()
        checkInternetConnection()
    }

    override fun onResume() {
        super.onResume()
        firebaseAuth.addAuthStateListener(authStateListener)
        setupFirestoreInRealtime()
    }

    override fun onPause() {
        super.onPause()
        firebaseAuth.removeAuthStateListener(authStateListener)
        listenerRegistration.remove()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val menuItem = menu?.findItem(R.id.action_search)
        val searchView = menuItem?.actionView as SearchView
        searchView.queryHint = getString(R.string.search_by_name)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                val filteredList = mutableListOf<PackageService>()
                for (packageService in packageServiceList) {
                    if (newText!!.lowercase() in packageService.name.toString().lowercase()) {
                        filteredList.add(packageService)
                    }
                }
                packageServiceAdapter.updateList(filteredList)
                binding.tvWithoutResults.visibility = if (filteredList.isNullOrEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                return false
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_contract_requests -> startActivity(
                Intent(
                    this,
                    RequestedContractActivity::class.java
                )
            )
            R.id.action_promo -> {
                OffersAndPromotionsFragment().show(
                    supportFragmentManager,
                    OffersAndPromotionsFragment::class.java.simpleName
                )
            }
            R.id.action_about -> startActivity(
                Intent(
                    this,
                    AboutActivity::class.java
                )
            )
            R.id.action_sign_out -> {
                MaterialAlertDialogBuilder(this).setTitle(getString(R.string.sign_off))
                    .setMessage(getString(R.string.are_you_sure_to_take_this_action))
                    .setPositiveButton(getString(R.string.delete)) { _, _ ->
                        AuthUI.getInstance().signOut(this).addOnSuccessListener {
                            Toast.makeText(
                                this,
                                getString(R.string.you_have_logged_out),
                                Toast.LENGTH_SHORT
                            ).show()
                            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN) {
                                param(FirebaseAnalytics.Param.SUCCESS, 100)
                                param(FirebaseAnalytics.Param.METHOD, Constants.PARAM_SIGN_OUT)
                            }
                        }.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                binding.nsvPackagesServices.visibility = View.GONE
                                binding.llProgress.visibility = View.VISIBLE
                                binding.efabNewPackageService.hide()
                            } else {
                                snackBar.apply {
                                    setText(getString(R.string.failed_to_log_out))
                                    show()
                                }
                                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN) {
                                    param(FirebaseAnalytics.Param.SUCCESS, 201)
                                    param(FirebaseAnalytics.Param.METHOD, Constants.PARAM_SIGN_OUT)
                                }
                            }
                        }
                    }.setNegativeButton(getString(R.string.cancel), null).show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(packageService: PackageService) {
        packageServiceSelected = packageService
        AddDialogFragment().show(supportFragmentManager, AddDialogFragment::class.java.simpleName)
    }

    override fun onLongClick(packageService: PackageService) {
        val adapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice)
        adapter.add(getString(R.string.delete))
        adapter.add(getString(R.string.add_more_images))
        MaterialAlertDialogBuilder(this).setAdapter(adapter) { _: DialogInterface, position: Int ->
            when (position) {
                0 -> confirmDeletePackageService(packageService)
                1 -> {
                    packageServiceSelected = packageService
                    val intent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    galleryResult.launch(intent)
                }
            }
        }.show()
    }

    override fun getPackageServiceSelected() = packageServiceSelected
    override fun onNetworkChange(isConnected: Boolean) = showNetworkErrorSnackBar(isConnected)
    private fun checkInternetConnection() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Constants.ACTION_INTENT)
        registerReceiver(ConnectionReceiver(), intentFilter)
        ConnectionReceiver.receiverListener = this
        val manager =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = manager.activeNetworkInfo
        val isConnected = networkInfo != null && networkInfo.isConnectedOrConnecting
        showNetworkErrorSnackBar(isConnected)
    }

    private fun showNetworkErrorSnackBar(isConnected: Boolean) {
        if (!isConnected) {
            Snackbar.make(
                binding.root,
                getString(R.string.no_network_connection),
                Snackbar.LENGTH_INDEFINITE
            ).setTextColor(Color.WHITE)
                .setAction(getString(R.string.go_to_settings)) { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
                .show()
        }
    }

    private fun setupFirestoreInRealtime() {
        val db = Firebase.firestore
        val packageServiceRef = db.collection(Constants.COLL_PACKAGE_SERVICE)
        listenerRegistration = packageServiceRef.addSnapshotListener { querySnapshot, error ->
            if (error != null) {
                snackBar.apply {
                    setText(getString(R.string.failed_to_query_the_data))
                    show()
                }
                return@addSnapshotListener
            }
            for (documentChange in querySnapshot!!.documentChanges) {
                val packageService = documentChange.document.toObject(PackageService::class.java)
                packageService.id = documentChange.document.id
                when (documentChange.type) {
                    DocumentChange.Type.ADDED -> packageServiceAdapter.add(packageService)
                    DocumentChange.Type.MODIFIED -> packageServiceAdapter.update(packageService)
                    DocumentChange.Type.REMOVED -> packageServiceAdapter.delete(packageService)
                }
            }
        }
    }

    private fun setupAuth() {
        firebaseAuth = FirebaseAuth.getInstance()
        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser != null) {
                supportActionBar?.title = auth.currentUser?.displayName
                binding.llProgress.visibility = View.GONE
                binding.nsvPackagesServices.visibility = View.VISIBLE
                binding.efabNewPackageService.show()
            } else {
                val providers = arrayListOf(
                    AuthUI.IdpConfig.EmailBuilder().build(),
                    AuthUI.IdpConfig.GoogleBuilder().build()
                )
                val loginView = AuthMethodPickerLayout.Builder(R.layout.view_login)
                    .setEmailButtonId(R.id.btnEmail).setGoogleButtonId(R.id.btnGoogle)
                    .setTosAndPrivacyPolicyId(R.id.tvTermsAndConditions).build()
                authLauncher.launch(
                    AuthUI.getInstance().createSignInIntentBuilder()
                        .setAvailableProviders(providers).setIsSmartLockEnabled(false)
                        .setTosAndPrivacyPolicyUrls(
                            Constants.TERMS_AND_CONDITIONS,
                            Constants.PRIVACY_POLICY
                        ).setAuthMethodPickerLayout(loginView).setTheme(R.style.LoginTheme).build()
                )
            }
        }
    }

    private fun setupRecyclerView() {
        packageServiceAdapter = PackageServiceAdapter(packageServiceList, this)
        binding.recyclerView.apply {
            layoutManager =
                GridLayoutManager(this@MainActivity, 3, GridLayoutManager.HORIZONTAL, false)
            adapter = this@MainActivity.packageServiceAdapter
        }
    }

    private fun setupButtons() {
        binding.efabNewPackageService.setOnClickListener {
            packageServiceSelected = null
            AddDialogFragment().show(
                supportFragmentManager,
                AddDialogFragment::class.java.simpleName
            )
        }
    }

    private fun setupAnalytics() {
        firebaseAnalytics = Firebase.analytics
    }

    private fun uploadingMultipleImages(position: Int) {
        FirebaseAuth.getInstance().currentUser?.let { user ->
            progressSnack.apply {
                setText("${getString(R.string.uploading_image)} ${position + 1} de $count...")
                show()
            }
            val packageServiceRef = Firebase.storage.reference.child(user.uid)
                .child(Constants.PATH_PACKAGE_SERVICE_IMAGES).child(packageServiceSelected!!.id!!)
                .child("image${position + 1}")
            packageServiceRef.putFile(uriList[position]).addOnSuccessListener {
                if (position < count - 1) {
                    uploadingMultipleImages(position + 1)
                } else {
                    progressSnack.apply {
                        setTextColor(Color.CYAN)
                        setText(getString(R.string.images_uploaded_successfully))
                        duration = Snackbar.LENGTH_SHORT
                        show()
                    }
                }
            }.addOnFailureListener {
                progressSnack.apply {
                    setTextColor(Color.RED)
                    setText("${getString(R.string.image_upload_error)} ${position + 1}")
                    duration = Snackbar.LENGTH_LONG
                    show()
                }
            }
        }
    }

    private fun confirmDeletePackageService(packageService: PackageService) {
        MaterialAlertDialogBuilder(this).setTitle(getString(R.string.delete_package))
            .setMessage(packageService.name)
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                packageService.id?.let { id ->
                    packageService.imagePath?.let { url ->
                        try {
                            val reference = Firebase.storage.getReferenceFromUrl(url)
                            reference.delete()
                                .addOnSuccessListener { deletePackageServiceInFirestore(id) }
                                .addOnFailureListener { exception ->
                                    if ((exception as StorageException).errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                                        deletePackageServiceInFirestore(id)
                                    } else {
                                        snackBar.apply {
                                            setText(getString(R.string.failed_to_delete_image))
                                            show()
                                        }
                                    }
                                }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            deletePackageServiceInFirestore(id)
                        }
                    }
                }
            }.setNegativeButton(getString(R.string.cancel), null).show()
    }

    private fun deletePackageServiceInFirestore(packageServiceId: String) {
        val db = Firebase.firestore
        val packageServiceRef = db.collection(Constants.COLL_PACKAGE_SERVICE)
        packageServiceRef.document(packageServiceId).delete().addOnSuccessListener {
            Toast.makeText(
                this,
                getString(R.string.package_removed),
                Toast.LENGTH_SHORT
            ).show()
        }.addOnFailureListener {
            snackBar.apply {
                setText(getString(R.string.failed_to_remove_package))
                show()
            }
        }
    }
}