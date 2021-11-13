package com.manuel.administradorred.chat

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
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
import com.manuel.administradorred.databinding.FragmentChatBinding
import com.manuel.administradorred.models.Message
import com.manuel.administradorred.models.RequestedContract
import com.manuel.administradorred.requested_contract.OnRequestedContractSelected
import com.manuel.administradorred.utils.Constants

class ChatFragment : Fragment(), OnChatListener {
    private var binding: FragmentChatBinding? = null
    private var requestedContract: RequestedContract? = null
    private lateinit var chatAdapter: ChatAdapter
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChatBinding.inflate(inflater, container, false)
        binding?.let { view ->
            return view.root
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getContract()
        setupRecyclerView()
        setupButtons()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            activity?.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.forEach { item ->
            item.isEnabled = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        (activity as? AppCompatActivity)?.let { appCompatActivity ->
            appCompatActivity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
            appCompatActivity.supportActionBar?.title = getString(R.string.contract_requests)
            setHasOptionsMenu(false)
        }
    }

    override fun deleteMessage(message: Message) {
        requestedContract?.let { contract1 ->
            val database = Firebase.database
            val messageRef =
                database.getReference(Constants.PATH_CHATS).child(contract1.id).child(message.id)
            messageRef.removeValue { error, _ ->
                binding?.let { view ->
                    if (error != null) {
                        Snackbar.make(
                            view.root,
                            getString(R.string.error_clearing_message),
                            Snackbar.LENGTH_SHORT
                        ).setTextColor(Color.RED).show()
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

    private fun getContract() {
        requestedContract = (activity as? OnRequestedContractSelected)?.getContractSelected()
        requestedContract?.let {
            setupActionBar()
            setupRealtimeDatabase()
        }
    }

    private fun setupRealtimeDatabase() {
        requestedContract?.let { contract ->
            val database = Firebase.database
            val chatRef = database.getReference(Constants.PATH_CHATS).child(contract.id)
            val childListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    getMessage(snapshot)?.let { message ->
                        chatAdapter.add(message)
                        binding?.recyclerView?.scrollToPosition(chatAdapter.itemCount - 1)
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    getMessage(snapshot)?.let { message ->
                        chatAdapter.update(message)
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    getMessage(snapshot)?.let { message ->
                        chatAdapter.delete(message)
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {
                    binding?.let { view ->
                        Snackbar.make(
                            view.root,
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
        chatAdapter = ChatAdapter(mutableListOf(), this)
        binding?.let { view ->
            view.recyclerView.apply {
                layoutManager = LinearLayoutManager(context).also { linearLayoutManager ->
                    linearLayoutManager.stackFromEnd = true
                }
                adapter = this@ChatFragment.chatAdapter
            }
        }
    }

    private fun setupButtons() {
        binding?.let { binding ->
            binding.fabSend.setOnClickListener {
                if (binding.etMessage.text.toString().trim().isEmpty()) {
                    binding.tilMessage.run {
                        error = getString(R.string.this_field_is_required)
                        requestFocus()
                    }
                } else {
                    binding.tilMessage.error = null
                    sendMessage()
                }
            }
        }
    }

    private fun sendMessage() {
        binding?.let { binding ->
            requestedContract?.let { contract1 ->
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
                        binding.etMessage.text = null
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