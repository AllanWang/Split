package com.pitchedapps.split.main

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    var receiptImage: ReceiptImage by mutableStateOf(ReceiptImage.Pending)
}

sealed interface ReceiptImage {
    data class Loaded(val uri: Uri) : ReceiptImage
    data class Error(val message: String) : ReceiptImage
    object Pending : ReceiptImage
}