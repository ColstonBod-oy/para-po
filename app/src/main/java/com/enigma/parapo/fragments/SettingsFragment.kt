package com.enigma.parapo.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.enigma.parapo.activities.GetStartedActivity
import com.enigma.parapo.contants.Constants
import com.enigma.parapo.databinding.FragmentSettingsBinding
import com.enigma.parapo.firebase.FireStoreClass
import com.enigma.parapo.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val view = binding.root

        auth = Firebase.auth
        binding.cvSignOut.setOnClickListener {
            auth.signOut()
            startActivity(Intent(requireContext(), GetStartedActivity::class.java))
            requireActivity().finish()
        }

        binding.btnSave.setOnClickListener {
            val newUsername = binding.etEditName.text.toString().trim()

            if (newUsername.isNotEmpty()) {
                val mFireStore = FirebaseFirestore.getInstance()
                mFireStore.collection(Constants.USERS).document(FireStoreClass().getCurrentUserId())
                    .update("name", newUsername)
                    .addOnSuccessListener {
                        setName()
                        binding.tilEditName.error = null
                        Toast.makeText(requireContext(), "Username updated", Toast.LENGTH_SHORT)
                            .show()
                    }
                    .addOnFailureListener { e ->
                        binding.tilEditName.error = null
                        Toast.makeText(
                            requireContext(),
                            "Failed to update username: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } else {
                binding.tilEditName.error = "Enter new username"
            }
        }

        setName()
        setProfilePic()

        return view
    }

    private fun setName() {
        val mFireStore = FirebaseFirestore.getInstance()
        mFireStore.collection(Constants.USERS).document(FireStoreClass().getCurrentUserId()).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                if (user != null && _binding != null) {
                    binding.tvMainName.text = user.name
                }
            }
    }

    private fun setProfilePic() {
        val mFireStore = FirebaseFirestore.getInstance()
        mFireStore.collection(Constants.USERS).document(FireStoreClass().getCurrentUserId()).get()
            .addOnSuccessListener { document ->
                val image = document.get("image").toString()
                val storageRef = Firebase.storage.reference
                val pathReference = storageRef.child(image)
                pathReference.downloadUrl.addOnSuccessListener { uri ->
                    // Reference exists, user signed up with email
                    Picasso.get().load(uri).into(binding.circleImageView)
                }.addOnFailureListener { exception ->
                    // Reference doesn't exist, user signed up with gmail
                    Picasso.get().load(image).into(binding.circleImageView)
                }
            }
    }
}