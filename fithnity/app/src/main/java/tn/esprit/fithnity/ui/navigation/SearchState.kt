package tn.esprit.fithnity.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import android.location.Address

/**
 * Global search state that can be accessed from any screen
 * Used to share search query between TopBar and screens
 */
object SearchState {
    var searchQuery by mutableStateOf("")
        private set
    
    var onSearchQueryChange: ((String) -> Unit)? = null
    
    // Place suggestions state for dropdown
    var placeSuggestions by mutableStateOf<List<Address>>(emptyList())
        private set
    
    var isLoadingSuggestions by mutableStateOf(false)
        private set
    
    var showSuggestions by mutableStateOf(false)
        private set
    
    var isHomeScreen by mutableStateOf(false)
        private set
    
    fun updateQuery(query: String) {
        searchQuery = query
        onSearchQueryChange?.invoke(query)
    }
    
    fun setSearchHandler(handler: (String) -> Unit) {
        onSearchQueryChange = handler
    }
    
    fun clearSearchHandler() {
        onSearchQueryChange = null
    }
    
    fun updateSuggestions(suggestions: List<Address>, isLoading: Boolean, show: Boolean) {
        placeSuggestions = suggestions
        isLoadingSuggestions = isLoading
        showSuggestions = show
    }
    
    fun updateHomeScreen(isHome: Boolean) {
        isHomeScreen = isHome
        if (!isHome) {
            showSuggestions = false
            placeSuggestions = emptyList()
        }
    }
}

