package com.omerek.guitarlearn.ui.platform
import androidx.compose.runtime.Composable
@Composable fun KeepScreenOn() {}

class MicPermission(val granted: Boolean) { fun request() {} }
@Composable fun rememberMicPermission(): MicPermission = MicPermission(false)
