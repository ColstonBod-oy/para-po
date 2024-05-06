package com.enigma.parapo.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.enigma.parapo.contants.Constants
import com.enigma.parapo.databinding.ActivityManageProfileBinding
import com.enigma.parapo.firebase.FireStoreClass
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso

class ManageProfileActivity : AppCompatActivity() {
    private var binding: ActivityManageProfileBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageProfileBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        binding?.ivBackBtnManageProfile?.setOnClickListener {
            finish()
        }

        setProfilePic()
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
                    Picasso.get().load(uri).into(binding?.circleImageView)
                }.addOnFailureListener { exception ->
                    // Reference doesn't exist, user signed up with gmail
                    Picasso.get().load(image).into(binding?.circleImageView)
                }
            }
    }
}
