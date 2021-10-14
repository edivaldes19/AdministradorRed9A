package com.manuel.administradorred.requested_contract

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.manuel.administradorred.R
import com.manuel.administradorred.chat.ChatFragment
import com.manuel.administradorred.databinding.ActivityContractBinding
import com.manuel.administradorred.entities.Contract
import com.manuel.administradorred.fcm.NotificationRS
import com.manuel.administradorred.utils.Constants
import java.util.*

class RequestedRequestedRequestedContractActivity : AppCompatActivity(), OnRequestedContractListener, RequestedContractAux {
    private lateinit var binding: ActivityContractBinding
    private lateinit var adapterRequested: RequestedContractAdapter
    private lateinit var contractSelected: Contract
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private val aValues: Array<String> by lazy {
        resources.getStringArray(R.array.status_value)
    }
    private val aKeys: Array<Int> by lazy {
        resources.getIntArray(R.array.status_key).toTypedArray()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContractBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRecyclerView()
        setupFirestore()
        configAnalytics()
    }

    override fun onStartChat(contract: Contract) {
        contractSelected = contract
        val fragment = ChatFragment()
        supportFragmentManager.beginTransaction().add(R.id.containerMain, fragment)
            .addToBackStack(null).commit()
    }

    override fun onStatusChange(contract: Contract) {
        val db = FirebaseFirestore.getInstance()
        db.collection(Constants.COLL_CONTRACTS).document(contract.id)
            .update(Constants.PROP_STATUS, contract.status).addOnSuccessListener {
                Toast.makeText(this, getString(R.string.updated_contract), Toast.LENGTH_SHORT)
                    .show()
                notifyClient(contract)
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.ADD_SHIPPING_INFO) {
                    val products = mutableListOf<Bundle>()
                    contract.packages.forEach {
                        val bundle = Bundle()
                        bundle.putString("id_product", it.key)
                        products.add(bundle)
                    }
                    param(FirebaseAnalytics.Param.SHIPPING, products.toTypedArray())
                    param(FirebaseAnalytics.Param.PRICE, contract.totalPrice)
                }
            }.addOnFailureListener {
                Toast.makeText(
                    this,
                    getString(R.string.failed_to_update_contract),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    override fun getContractSelected(): Contract = contractSelected
    private fun setupRecyclerView() {
        adapterRequested = RequestedContractAdapter(mutableListOf(), this)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@RequestedRequestedRequestedContractActivity)
            adapter = this@RequestedRequestedRequestedContractActivity.adapterRequested
        }
    }

    private fun setupFirestore() {
        val db = FirebaseFirestore.getInstance()
        db.collection(Constants.COLL_CONTRACTS)
            .orderBy(Constants.PROP_DATE, Query.Direction.DESCENDING).get()
            .addOnSuccessListener { snapshot ->
                for (document in snapshot) {
                    val order = document.toObject(Contract::class.java)
                    order.id = document.id
                    adapterRequested.add(order)
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    getString(R.string.failed_to_query_the_data),
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
    }

    private fun configAnalytics() {
        firebaseAnalytics = Firebase.analytics
    }

    private fun notifyClient(contract: Contract) {
        val db = FirebaseFirestore.getInstance()
        db.collection(Constants.COLL_USERS).document(contract.clientId)
            .collection(Constants.COLL_TOKENS).get().addOnSuccessListener { snapshot ->
                var tokensStr = ""
                for (document in snapshot) {
                    val tokenMap = document.data
                    tokensStr += "${tokenMap.getValue(Constants.PROP_TOKEN)},"
                }
                if (tokensStr.isNotEmpty()) {
                    tokensStr = tokensStr.dropLast(1)
                    var names = ""
                    contract.packages.forEach { entry ->
                        names += "${entry.value.name}, "
                    }
                    names = names.dropLast(2)
                    val index = aKeys.indexOf(contract.status)
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
                Toast.makeText(
                    this,
                    getString(R.string.failed_to_query_the_data),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}