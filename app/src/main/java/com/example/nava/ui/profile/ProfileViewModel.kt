package com.example.nava.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel class ProfileViewModel @Inject constructor(private val supabase: SupabaseClient) : ViewModel() {
    private val _premium = MutableStateFlow(false)
    val premium = _premium.asStateFlow()
    fun upgrade() = viewModelScope.launch { runCatching { supabase.postgrest.rpc("enable_demo_premium") }.onSuccess { _premium.value = true } }
}
