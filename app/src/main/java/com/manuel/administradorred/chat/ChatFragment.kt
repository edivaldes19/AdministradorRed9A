package com.manuel.administradorred.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.manuel.administradorred.R
import com.manuel.administradorred.contract.ContractAux
import com.manuel.administradorred.databinding.FragmentChatBinding
import com.manuel.administradorred.entities.Contract
import com.manuel.administradorred.entities.Message
import com.manuel.administradorred.utils.Constants

class ChatFragment : Fragment(), OnChatListener {
    private var binding: FragmentChatBinding? = null
    private lateinit var adapter: ChatAdapter
    private var contract: Contract? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChatBinding.inflate(inflater, container, false)
        binding?.let { fragmentChatBinding ->
            return fragmentChatBinding.root
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            activity?.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        (activity as? AppCompatActivity)?.let { appCompatActivity ->
            appCompatActivity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
            appCompatActivity.supportActionBar?.title = getString(R.string.my_contracts)
            setHasOptionsMenu(false)
        }
        super.onDestroy()
    }

    override fun deleteMessage(message: Message) {
        contract?.let { contract1 ->
            val database = Firebase.database
            val messageRef =
                database.getReference(Constants.PATH_CHATS).child(contract1.id).child(message.id)
            messageRef.removeValue { error, _ ->
                binding?.let { fragmentChatBinding ->
                    if (error != null) {
                        Snackbar.make(
                            fragmentChatBinding.root,
                            getString(R.string.error_clearing_message),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            activity,
                            getString(R.string.message_deleted),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getContract()
        setupRecyclerView()
        setupButtons()
    }

    private fun getContract() {
        contract = (activity as? ContractAux)?.getContractSelected()
        contract?.let {
            setupActionBar()
            setupRealtimeDatabase()
        }
    }

    private fun setupRealtimeDatabase() {
        contract?.let { it ->
            val database = Firebase.database
            val chatRef = database.getReference(Constants.PATH_CHATS).child(it.id)
            val childListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    getMessage(snapshot)?.let { message ->
                        adapter.add(message)
                        binding?.recyclerView?.scrollToPosition(adapter.itemCount - 1)
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    getMessage(snapshot)?.let { message ->
                        adapter.update(message)
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    getMessage(snapshot)?.let { message ->
                        adapter.delete(message)
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {
                    binding?.let { fragmentChatBinding ->
                        Snackbar.make(
                            fragmentChatBinding.root,
                            getString(R.string.error_loading_chat_messages),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            chatRef.addChildEventListener(childListener)
        }
    }

    private fun getMessage(snapshot: DataSnapshot): Message? {
        snapshot.getValue(Message::class.java)?.let { message ->
            snapshot.key?.let { it -> message.id = it }
            FirebaseAuth.getInstance().currentUser?.let { user ->
                message.myUid = user.uid
            }
            return message
        }
        return null
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(mutableListOf(), this)
        binding?.let { it ->
            it.recyclerView.apply {
                layoutManager = LinearLayoutManager(context).also { linearLayoutManager ->
                    linearLayoutManager.stackFromEnd = true
                }
                adapter = this@ChatFragment.adapter
            }
        }
    }

    private fun setupButtons() {
        binding?.let { binding ->
            binding.fabSend.setOnClickListener {
                sendMessage()
            }
        }
    }

    private fun sendMessage() {
        binding?.let { binding ->
            contract?.let { contract1 ->
                val database = Firebase.database
                val chatRef = database.getReference(Constants.PATH_CHATS).child(contract1.id)
                val user = FirebaseAuth.getInstance().currentUser
                user?.let { firebaseUser ->
                    val message = Message(
                        message = binding.etMessage.text.toString().trim(),
                        sender = firebaseUser.uid
                    )
                    binding.fabSend.isEnabled = false
                    chatRef.push().setValue(message).addOnSuccessListener {
                        binding.etMessage.setText("")
                    }.addOnCompleteListener {
                        binding.fabSend.isEnabled = true
                    }
                }
            }
        }
    }

    private fun setupActionBar() {
        (activity as? AppCompatActivity)?.let { appCompatActivity ->
            appCompatActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            appCompatActivity.supportActionBar?.title = getString(R.string.technical_support)
            setHasOptionsMenu(true)
        }
    }
}