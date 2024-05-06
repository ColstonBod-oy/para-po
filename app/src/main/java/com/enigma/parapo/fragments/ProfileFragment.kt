package com.enigma.parapo.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.enigma.parapo.contants.Constants
import com.enigma.parapo.databinding.FragmentProfileBinding
import com.enigma.parapo.firebase.FireStoreClass
import com.enigma.parapo.models.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var mFireStore: FirebaseFirestore
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val view = binding.root
        setName()
        setProfilePic()
        mFireStore = FirebaseFirestore.getInstance()
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
                    if (_binding != null) {
                        // Reference exists, user signed up with email
                        Picasso.get().load(uri).into(binding.mainProfilePic)
                    }
                }.addOnFailureListener { exception ->
                    if (_binding != null) {
                        // Reference doesn't exist, user signed up with gmail
                        Picasso.get().load(image).into(binding.mainProfilePic)
                    }
                }
            }
    }
}