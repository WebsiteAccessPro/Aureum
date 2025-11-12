package com.example.aureum1.controller.activities.Registro

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class RegisterPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    
    override fun getItemCount(): Int = 3
    
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> RegisterStep1Fragment()
            1 -> RegisterStep2Fragment()
            2 -> RegisterStep3Fragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}