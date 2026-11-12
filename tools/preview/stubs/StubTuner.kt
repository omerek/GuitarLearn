package com.omerek.guitarlearn.ui.screens
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
@Composable fun TunerScreen(modifier: Modifier = Modifier) = TunerView(frequency = 107.2f, micGranted = true, onRequestMic = {}, modifier = modifier)
