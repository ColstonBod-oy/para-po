package com.enigma.parapo.fragments

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
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
import kotlin.math.atan2

class ProfileFragment : Fragment(), View.OnClickListener {
    private var _binding: FragmentProfileBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var mFireStore: FirebaseFirestore

    private val stickerList = arrayListOf<Int>()
    private lateinit var stickerView: StickerView

    private var currentSkinResId = 0
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
        loadStickers()
        initViews()
        setJeepneyAvatar()

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

    private fun setJeepneyAvatar() {
        val mFireStore = FirebaseFirestore.getInstance()
        val userRef =
            mFireStore.collection(Constants.USERS).document(FireStoreClass().getCurrentUserId())
        userRef.get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                if (user != null && _binding != null) {
                    // If skin's value is 0 it means it's a new user
                    // and we set the actual resId of the default skin
                    if (user.skin == 0) {
                        userRef.update("skin", stickerList[0])
                        currentSkinResId = stickerList[0]
                        with(binding) {
                            stickerView.visibility = View.VISIBLE
                            btnEdit.visibility = View.VISIBLE
                            skins.visibility = View.VISIBLE
                            stickers.visibility = View.VISIBLE
                        }
                    } else {
                        changeSkin(user.skin)
                        binding.btnEdit.text = "Edit"

                        for (stickerData in user.stickers) {
                            val resId = (stickerData["resId"] as Number).toInt()

                            // A 3x3 transformation matrix:
                            // matrixValues[0] - ScaleX
                            // matrixValues[1] - SkewX
                            // matrixValues[2] - TranslationX
                            // matrixValues[3] - SkewY
                            // matrixValues[4] - ScaleY
                            // matrixValues[5] - TranslationY
                            // matrixValues[6] - Perspective0
                            // matrixValues[7] - Perspective1
                            // matrixValues[8] - Perspective2
                            val matrixValues = (stickerData["matrix"] as List<Float>).toFloatArray()

                            val bitmap = BitmapFactory.decodeResource(resources, resId)
                            val drawable: Drawable = BitmapDrawable(resources, bitmap)
                            val drawableSticker = DrawableSticker(drawable)
                            drawableSticker.resId = resId
                            stickerView.addSticker(drawableSticker)
                            drawableSticker.matrix.reset()
                            drawableSticker.matrix.preTranslate(matrixValues[2], matrixValues[5])
                            drawableSticker.matrix.preScale(matrixValues[0], matrixValues[4])
                            drawableSticker.matrix.preSkew(matrixValues[1], matrixValues[3])

                            // Get Inverse Tangent
                            val rotationDegrees = Math.toDegrees(
                                atan2(
                                    matrixValues[3].toDouble(),
                                    matrixValues[4].toDouble()
                                )
                            ).toFloat()
                            drawableSticker.matrix.preRotate(rotationDegrees)
                        }

                        stickerView.setLocked(true)

                        with(binding) {
                            stickerView.visibility = View.VISIBLE
                            btnEdit.visibility = View.VISIBLE
                            skins.visibility = View.VISIBLE
                            stickers.visibility = View.VISIBLE
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Failed to load Jeepney avatar: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
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
                        saveJeepneyAvatar()
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
        currentSkinResId = resId
        stickerView.setLocked(false)
        binding.btnEdit.text = "Save"
        imageView.setImageResource(resId)
        stickerView.invalidate()
    }

    private fun addSticker(resId: Int) {
        val bitmap = BitmapFactory.decodeResource(resources, resId)
        val drawable: Drawable = BitmapDrawable(resources, bitmap)
        val drawableSticker = DrawableSticker(drawable)
        drawableSticker.resId = resId
        stickerView.setLocked(false)
        binding.btnEdit.text = "Save"
        stickerView.addSticker(drawableSticker)
        stickerView.invalidate()
    }

    private fun saveJeepneyAvatar() {
        val stickersData = arrayListOf<Map<String, Any?>>()

        for (sticker in stickerView.stickers) {
            if (sticker is DrawableSticker) {
                // The matrix contains the skew, translate, scale, etc. of the sticker
                val matrixValues = FloatArray(9)
                sticker.matrix.getValues(matrixValues)

                // Convert FloatArray to Array<Any?>
                val matrixValuesArray = matrixValues.map { it as Any? }.toTypedArray()

                // Combine resId and matrixValues into a single array
                val info = mapOf(
                    "resId" to sticker.resId,
                    "matrix" to matrixValues.toList()
                )

                stickersData.add(info)
            }
        }

        val mFireStore = FirebaseFirestore.getInstance()
        mFireStore.collection(Constants.USERS).document(FireStoreClass().getCurrentUserId())
            .update("skin", currentSkinResId, "stickers", stickersData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Jeepney avatar updated", Toast.LENGTH_SHORT)
                    .show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Failed to update Jeepney avatar: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}