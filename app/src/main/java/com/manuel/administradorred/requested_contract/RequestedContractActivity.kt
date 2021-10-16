package com.manuel.administradorred.requested_contract

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.manuel.administradorred.R
import com.manuel.administradorred.chat.ChatFragment
import com.manuel.administradorred.databinding.ActivityRequestedContractBinding
import com.manuel.administradorred.fcm.NotificationRS
import com.manuel.administradorred.models.RequestedContract
import com.manuel.administradorred.utils.Constants
import java.util.*

class RequestedContractActivity : AppCompatActivity(), OnRequestedContractListener,
    RequestedContractAux {
    private lateinit var binding: ActivityRequestedContractBinding
    private lateinit var adapterRequested: RequestedContractAdapter
    private lateinit var requestedContractSelected: RequestedContract
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private val errorSnack: Snackbar by lazy {
        Snackbar.make(binding.root, "", Snackbar.LENGTH_SHORT).setTextColor(Color.RED)
    }
    private val aValues: Array<String> by lazy {
        resources.getStringArray(R.array.status_value)
    }
    private val aKeys: Array<Int> by lazy {
        resources.getIntArray(R.array.status_key).toTypedArray()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequestedContractBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRecyclerView()
        setupFirestore()
        configAnalytics()
    }

    override fun onStartChat(requestedContract: RequestedContract) {
        requestedContractSelected = requestedContract
        val fragment = ChatFragment()
        supportFragmentManager.beginTransaction().add(R.id.containerMain, fragment)
            .addToBackStack(null).commit()
    }

    override fun onStatusChange(requestedContract: RequestedContract) {
        val db = FirebaseFirestore.getInstance()
        db.collection(Constants.COLL_CONTRACTS_REQUESTED).document(requestedContract.id)
            .update(Constants.PROP_STATUS, requestedContract.status).addOnSuccessListener {
                Toast.makeText(this, getString(R.string.updated_contract), Toast.LENGTH_SHORT)
                    .show()
                notifyClient(requestedContract)
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.ADD_SHIPPING_INFO) {
                    val packagesServices = mutableListOf<Bundle>()
                    requestedContract.packagesServices.forEach { entry ->
                        val bundle = Bundle()
                        bundle.putString("id_package_service", entry.key)
                        packagesServices.add(bundle)
                    }
                    param(FirebaseAnalytics.Param.SHIPPING, packagesServices.toTypedArray())
                    param(FirebaseAnalytics.Param.PRICE, requestedContract.totalPrice.toDouble())
                }
            }
            .addOnFailureListener {
                errorSnack.apply {
                    setText(getString(R.string.image_upload_error))
                    show()
                }
            }
    }

    override fun getContractSelected(): RequestedContract = requestedContractSelected
    private fun setupRecyclerView() {
        adapterRequested = RequestedContractAdapter(mutableListOf(), this)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@RequestedContractActivity)
            adapter = this@RequestedContractActivity.adapterRequested
        }
    }

    private fun setupFirestore() {
        val db = FirebaseFirestore.getInstance()
        db.collection(Constants.COLL_CONTRACTS_REQUESTED)
            .orderBy(Constants.PROP_DATE, Query.Direction.DESCENDING).get()
            .addOnSuccessListener { snapshot ->
                for (document in snapshot) {
                    val contract = document.toObject(RequestedContract::class.java)
                    contract.id = document.id
                    adapterRequested.add(contract)
                }
            }.addOnFailureListener {
                errorSnack.apply {
                    setText(getString(R.string.failed_to_query_the_data))
                    show()
                }
            }
    }

    private fun configAnalytics() {
        firebaseAnalytics = Firebase.analytics
    }

    private fun notifyClient(requestedContract: RequestedContract) {
        val db = FirebaseFirestore.getInstance()
        db.collection(Constants.COLL_USERS).document(requestedContract.userId)
            .collection(Constants.COLL_TOKENS).get().addOnSuccessListener { snapshot ->
                var tokensStr = ""
                for (document in snapshot) {
                    val tokenMap = document.data
                    tokensStr += "${tokenMap.getValue(Constants.PROP_TOKEN)},"
                }
                if (tokensStr.isNotEmpty()) {
                    tokensStr = tokensStr.dropLast(1)
                    var names = ""
                    requestedContract.packagesServices.forEach { entry ->
                        names += "${entry.value.name}, "
                    }
                    names = names.dropLast(2)
                    val index = aKeys.indexOf(requestedContract.status)
                    val notificationRS = NotificationRS()
                    notificationRS.sendNotification(
                        "${getString(R.string.your_contract_is)} ${
                            aValues[index].lowercase(
                                Locale.getDefault()
                            )
                        }", names, tokensStr
                    )
                }
            }.addOnFailureListener {
                errorSnack.apply {
                    setText(getString(R.string.failed_to_query_the_data))
                    show()
                }
            }
    }
}