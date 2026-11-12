package com.omerek.guitarlearn.ui.platform
import androidx.compose.runtime.Composable
@Composable fun KeepScreenOn() {}

class MicPermission(val granted: Boolean) { fun request() {} }
@Composable fun rememberMicPermission(): MicPermission = MicPermission(false)
class BackupActions(val export: () -> Unit, val import: () -> Unit)
@Composable fun rememberBackupActions(store: com.omerek.guitarlearn.data.UserSongStore, onResult: (String) -> Unit) = BackupActions({}, {})
@Composable fun rememberBluetoothPermission(): MicPermission = MicPermission(true)
@Composable fun rememberBinaryFilePicker(onPicked: (ByteArray?, String?) -> Unit): () -> Unit = {}
