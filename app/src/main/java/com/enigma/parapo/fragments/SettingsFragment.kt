package com.enigma.parapo.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.enigma.parapo.activities.GetStartedActivity
import com.enigma.parapo.activities.ManageProfileActivity
import com.enigma.parapo.databinding.FragmentSettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

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

        binding.cvManageProfile.setOnClickListener {
            startActivity(Intent(requireContext(), ManageProfileActivity::class.java))
        }
        return view
    }
}