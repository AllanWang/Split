package com.pitchedapps.split.main

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.pitchedapps.split.ocr.ReceiptOcr

class MainViewModel : ViewModel() {
    var receiptImage: ReceiptImage by mutableStateOf(ReceiptImage.Pending)
}

sealed interface ReceiptImage {
    object Pending : ReceiptImage
    data class Loaded(val uri: Uri) : ReceiptImage
    data class Parsed(val uri: Uri, val data: ReceiptOcr.Result) : ReceiptImage
    data class Error(val message: String) : ReceiptImage
}