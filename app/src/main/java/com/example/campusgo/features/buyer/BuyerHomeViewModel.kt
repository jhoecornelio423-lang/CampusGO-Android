package com.example.campusgo.features.buyer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusgo.domain.model.CampusMeetingPoint
import com.example.campusgo.domain.model.Category
import com.example.campusgo.domain.model.Order
import com.example.campusgo.domain.model.Product
import com.example.campusgo.domain.model.ProfileWarning
import com.example.campusgo.domain.model.StoreCatalogGroup
import com.example.campusgo.domain.model.SubOrder
import com.example.campusgo.domain.model.SupportTicket
import com.example.campusgo.domain.model.UserProfile
import com.example.campusgo.domain.repository.AdminRepository
import com.example.campusgo.domain.repository.CartRepository
import com.example.campusgo.domain.repository.ChatRepository
import com.example.campusgo.domain.repository.OrderRepository
import com.example.campusgo.domain.repository.ProductRepository
import com.example.campusgo.domain.repository.SupportRepository
import com.example.campusgo.domain.usecase.GetActiveCampusCatalogUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BuyerHomeUiState(
    val stores: List<StoreCatalogGroup> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoadingCatalog: Boolean = true,
    val buyerActiveTicket: SupportTicket? = null,
    val favoriteProductIds: Set<String> = emptySet()
)

class BuyerHomeViewModel(
    private val getActiveCampusCatalogUseCase: GetActiveCampusCatalogUseCase,
    private val productRepository: ProductRepository,
    private val adminRepository: AdminRepository,
    private val orderRepository: OrderRepository,
    private val cartRepository: CartRepository,
    private val chatRepository: ChatRepository,
    private val supportRepository: SupportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BuyerHomeUiState())
    val uiState: StateFlow<BuyerHomeUiState> = _uiState.asStateFlow()

    val cartCalculation = cartRepository.cartCalculation

    val meetingPoints: StateFlow<List<CampusMeetingPoint>> = adminRepository.observeMeetingPoints()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var initializedUser: String? = null

    fun observeOrders(buyerId: String): Flow<List<Order>> =
        orderRepository.observeOrdersForBuyer(buyerId)

    fun observeWarnings(buyerId: String): Flow<List<ProfileWarning>> =
        orderRepository.observeUserWarnings(buyerId)

    fun observeUnreadCount(userId: String): Flow<Int> =
        chatRepository.observeUnreadCount(userId)

    fun initialize(userCampus: String, userId: String) {
        if (initializedUser == userId) return
        initializedUser = userId

        viewModelScope.launch(Dispatchers.IO) {
            adminRepository.refreshMeetingPoints()
            loadCatalog(userCampus, isSilent = false)
            loadFavorites(userId)

            // Polling relajado en segundo plano (cada 15s en vez de cada 4s)
            while (isActive) {
                delay(15000L)
                loadCatalog(userCampus, isSilent = true)
            }
        }

        // Monitoreo de tickets de soporte relajado
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                refreshActiveTicket(userId)
                delay(12000L)
            }
        }
    }

    suspend fun refreshActiveTicket(userId: String) = withContext(Dispatchers.IO) {
        try {
            val ticketRes = supportRepository.getActiveTicketForUser(userId)
            _uiState.value = _uiState.value.copy(buyerActiveTicket = ticketRes.getOrNull())
        } catch (_: Exception) {}
    }

    fun loadCatalog(userCampus: String, isSilent: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!isSilent && _uiState.value.stores.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoadingCatalog = true)
            }
            try {
                val catalogResult = getActiveCampusCatalogUseCase(userCampus)
                catalogResult.onSuccess { res ->
                    _uiState.value = _uiState.value.copy(
                        stores = res.stores,
                        categories = res.categories,
                        isLoadingCatalog = false
                    )
                }.onFailure {
                    _uiState.value = _uiState.value.copy(isLoadingCatalog = false)
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingCatalog = false)
            }
        }
    }

    fun loadFavorites(buyerId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val favRes = productRepository.getFavoriteProductIds(buyerId)
            val favs = favRes.getOrDefault(emptyList()).toSet()
            _uiState.value = _uiState.value.copy(favoriteProductIds = favs)
        }
    }

    fun toggleFavorite(buyerId: String, productId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _uiState.value.favoriteProductIds
            val isFav = productId in current
            val newFavs = if (isFav) current - productId else current + productId
            _uiState.value = _uiState.value.copy(favoriteProductIds = newFavs)

            if (isFav) {
                productRepository.removeFavorite(buyerId, productId)
            } else {
                productRepository.addFavorite(buyerId, productId)
            }
        }
    }

    fun addToCart(storeId: String, storeName: String, product: Product, quantity: Int = 1) {
        cartRepository.setStoreName(storeId, storeName)
        cartRepository.addToCart(product, quantity)
    }

    suspend fun getSubOrderById(subId: String): SubOrder? = withContext(Dispatchers.IO) {
        orderRepository.getSubOrderById(subId).getOrNull()
    }

    suspend fun getTicketById(ticketId: String, userId: String): SupportTicket? = withContext(Dispatchers.IO) {
        supportRepository.getTicketById(ticketId).getOrNull()
            ?: supportRepository.getActiveTicketForUser(userId).getOrNull()
    }

    suspend fun updateUserProfile(profile: UserProfile): Result<UserProfile> = withContext(Dispatchers.IO) {
        productRepository.updateUserProfile(profile)
    }

    suspend fun uploadAvatarImage(path: String, bytes: ByteArray, oldUrl: String?): Result<String> = withContext(Dispatchers.IO) {
        val uploadRes = productRepository.uploadImage("business-assets", path, bytes)
        if (uploadRes.isSuccess && !oldUrl.isNullOrBlank()) {
            try {
                productRepository.deleteImage("business-assets", oldUrl)
            } catch (_: Exception) {}
        }
        uploadRes
    }
}
