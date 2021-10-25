package com.manuel.administradorred.requested_contract

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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
    private lateinit var contractAdapter: RequestedContractAdapter
    private lateinit var requestedContractSelected: RequestedContract
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var listenerRegistration: ListenerRegistration
    private var requestedContractList: MutableList<RequestedContract> = mutableListOf()
    private val errorSnack: Snackbar by lazy {
        Snackbar.make(binding.root, "", Snackbar.LENGTH_SHORT).setTextColor(Color.YELLOW)
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
        configAnalytics()
    }

    override fun onResume() {
        super.onResume()
        configFirestoreRealtime()
    }

    override fun onPause() {
        super.onPause()
        listenerRegistration.remove()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_just_search, menu)
        val menuItem = menu?.findItem(R.id.action_search)
        val searchView = menuItem?.actionView as SearchView
        searchView.queryHint = getString(R.string.write_here_to_search)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val temporaryList: MutableList<RequestedContract> = ArrayList()
                for (requestedContract in requestedContractList) {
                    if (newText!! in requestedContract.id) {
                        temporaryList.add(requestedContract)
                    }
                }
                contractAdapter.updateList(temporaryList)
                if (temporaryList.isNullOrEmpty()) {
                    binding.tvWithoutResults.visibility = View.VISIBLE
                } else {
                    binding.tvWithoutResults.visibility = View.GONE
                }
                return false
            }
        })
        return super.onCreateOptionsMenu(menu)
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
                Toast.makeText(
                    this,
                    "${getString(R.string.contract)}: ${requestedContract.id} ${getString(R.string.updated)}.",
                    Toast.LENGTH_SHORT
                ).show()
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
        contractAdapter = RequestedContractAdapter(requestedContractList, this)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@RequestedContractActivity)
            adapter = this@RequestedContractActivity.contractAdapter
        }
    }

    private fun configFirestoreRealtime() {
        val db = FirebaseFirestore.getInstance()
        val requestedContractRef = db.collection(Constants.COLL_CONTRACTS_REQUESTED)
            .orderBy(Constants.PROP_REQUESTED, Query.Direction.DESCENDING)
        listenerRegistration = requestedContractRef.addSnapshotListener { snapshots, error ->
            if (error != null) {
                errorSnack.apply {
                    setText(getString(R.string.failed_to_query_the_data))
                    show()
                }
                return@addSnapshotListener
            }
            for (snapshot in snapshots!!.documentChanges) {
                val requestedContract = snapshot.document.toObject(RequestedContract::class.java)
                requestedContract.id = snapshot.document.id
                when (snapshot.type) {
                    DocumentChange.Type.ADDED -> contractAdapter.add(requestedContract)
                    DocumentChange.Type.MODIFIED -> contractAdapter.update(requestedContract)
                    DocumentChange.Type.REMOVED -> contractAdapter.delete(requestedContract)
                }
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