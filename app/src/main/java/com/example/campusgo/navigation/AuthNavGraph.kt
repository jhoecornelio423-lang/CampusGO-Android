package com.example.campusgo.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.campusgo.domain.model.UserProfile
import com.example.campusgo.features.auth.AuthRoute

@Composable
fun AuthNavGraph(
    onAuthSuccess: (UserProfile) -> Unit = {},
    modifier: Modifier = Modifier
) {
    AuthRoute(
        onAuthSuccess = onAuthSuccess,
        modifier = modifier.fillMaxSize()
    )
}
