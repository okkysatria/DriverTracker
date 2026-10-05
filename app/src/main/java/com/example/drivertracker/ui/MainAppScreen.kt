package com.example.drivertracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.drivertracker.ui.navigation.AppNavGraph

@Composable
fun MainAppScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    AppNavGraph(
        viewModel = viewModel,
        modifier = modifier
    )
}
