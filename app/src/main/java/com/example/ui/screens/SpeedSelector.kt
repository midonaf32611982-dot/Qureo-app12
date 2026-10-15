package com.example.ui.screens

import androidx.compose.runtime.*

@Composable
fun SpeedSelector(onSpeedSelected: (Int) -> Unit) {
    var speed by remember { mutableIntStateOf(3) }
    
    // UI implementation omitted as per instructions to only use the provided logic
    // Assuming UI handles callback
}
