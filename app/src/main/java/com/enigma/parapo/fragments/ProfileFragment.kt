package com.enigma.parapo.fragments

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.enigma.parapo.R
import com.enigma.parapo.contants.Constants
import com.enigma.parapo.databinding.FragmentProfileBinding
import com.enigma.parapo.firebase.FireStoreClass
import com.enigma.parapo.models.User
import com.enigma.parapo.stickerview.DrawableSticker
import com.enigma.parapo.stickerview.StickerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment(), View.OnClickListener {
    private var _binding: FragmentProfileBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var mFireStore: FirebaseFirestore

    private val stickerList = arrayListOf<Int>()
    private lateinit var stickerView: StickerView
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

        stickerView = binding.stickerView
        stickerView.setLocked(true)
        loadStickers()
        initViews()
        //val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.sticker1)
        //stickerView.addSticker(DrawableSticker(drawable))

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

    private fun loadStickers() {
        val array = resources.obtainTypedArray(R.array.stickers)
        for (index in 0 until array.length()) {
            val resId = array.getResourceId(index, 0)
            stickerList.add(resId)
        }
        array.recycle()
    }

    private fun initViews() {
        with(binding) {
            btnEdit.setOnClickListener(this@ProfileFragment)
            skin1.setOnClickListener(this@ProfileFragment)
            skin2.setOnClickListener(this@ProfileFragment)
            skin3.setOnClickListener(this@ProfileFragment)
            skin4.setOnClickListener(this@ProfileFragment)
            skin5.setOnClickListener(this@ProfileFragment)
            skin6.setOnClickListener(this@ProfileFragment)
            skin7.setOnClickListener(this@ProfileFragment)
            skin8.setOnClickListener(this@ProfileFragment)
            sticker1.setOnClickListener(this@ProfileFragment)
            sticker2.setOnClickListener(this@ProfileFragment)
            sticker3.setOnClickListener(this@ProfileFragment)
            sticker4.setOnClickListener(this@ProfileFragment)
            sticker5.setOnClickListener(this@ProfileFragment)
        }
    }

    override fun onClick(v: View?) {
        with(binding) {
            when (v?.id) {
                R.id.btnEdit -> {
                    if (btnEdit.text == "Edit") {
                        stickerView.setLocked(false)
                        btnEdit.text = "Save"
                    } else {
                        stickerView.setLocked(true)
                        btnEdit.text = "Edit"
                    }
                }

                R.id.skin1 -> {
                    changeSkin(stickerList[0])
                }

                R.id.skin2 -> {
                    changeSkin(stickerList[1])
                }

                R.id.skin3 -> {
                    changeSkin(stickerList[2])
                }

                R.id.skin4 -> {
                    changeSkin(stickerList[3])
                }

                R.id.skin5 -> {
                    changeSkin(stickerList[4])
                }

                R.id.skin6 -> {
                    changeSkin(stickerList[5])
                }

                R.id.skin7 -> {
                    changeSkin(stickerList[6])
                }

                R.id.skin8 -> {
                    changeSkin(stickerList[7])
                }

                R.id.sticker1 -> {
                    addSticker(stickerList[8])
                }

                R.id.sticker2 -> {
                    addSticker(stickerList[9])
                }

                R.id.sticker3 -> {
                    addSticker(stickerList[10])
                }

                R.id.sticker4 -> {
                    addSticker(stickerList[11])
                }

                R.id.sticker5 -> {
                    addSticker(stickerList[12])
                }
            }
        }
    }

    private fun changeSkin(resId: Int) {
        val imageView = stickerView.getChildAt(0) as ImageView
        stickerView.setLocked(false)
        binding.btnEdit.text = "Save"
        imageView.setImageResource(resId)
        stickerView.invalidate()
    }

    private fun addSticker(resId: Int) {
        val bitmap = BitmapFactory.decodeResource(resources, resId)
        val drawable: Drawable = BitmapDrawable(resources, bitmap)
        stickerView.setLocked(false)
        binding.btnEdit.text = "Save"
        stickerView.addSticker(DrawableSticker(drawable))
        stickerView.invalidate()
    }
}