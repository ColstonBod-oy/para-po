package com.enigma.parapo.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.enigma.parapo.R
import com.enigma.parapo.databinding.ActivityMainBinding
import com.enigma.parapo.fragments.MapFragment
import np.com.susanthapa.curved_bottom_navigation.CbnMenuItem

class MainActivity : AppCompatActivity(), MapFragment.OnSearchPlaceListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val activeIndex = savedInstanceState?.getInt("activeIndex") ?: 1
        navController = findNavController(R.id.nav_host_fragment)

        val menuItems = arrayOf(
            CbnMenuItem(
                R.drawable.ic_profile,
                R.drawable.avd_profile
            ),
            CbnMenuItem(
                R.drawable.ic_dashboard,
                R.drawable.avd_dashboard
            ),
            CbnMenuItem(
                R.drawable.ic_settings,
                R.drawable.avd_settings
            )
        )

        binding.navView.setMenuItems(menuItems, activeIndex)
        binding.navView.setOnMenuItemClickListener { cbnMenuItem, index ->
            when (index) {
                0 -> {
                    navController.navigate(R.id.navigation_profile)
                }

                1 -> {
                    navController.navigate(R.id.navigation_map)
                }

                2 -> {
                    navController.navigate(R.id.navigation_settings)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("activeIndex", binding.navView.getSelectedIndex())
        super.onSaveInstanceState(outState)
    }

    // Implement the interface for hiding nav view
    override fun hideNavView() {
        binding.navView.isVisible = false
    }

    // Implement the interface for showing nav view
    override fun showNavView() {
        binding.navView.isVisible = true
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val currentDestinationId = navController.currentDestination?.id

        when (currentDestinationId) {
            R.id.navigation_profile -> {
                binding.navView.onMenuItemClick(0)
            }

            R.id.navigation_map -> {
                binding.navView.onMenuItemClick(1)
            }

            R.id.navigation_settings -> {
                binding.navView.onMenuItemClick(2)
            }
        }
    }
}