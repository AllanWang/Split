package com.pitchedapps.split.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.pitchedapps.split.ui.theme.SplitTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val selectImage: () -> Unit

    init {
        val selectImageContract =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { result ->
                if (result != null) {
                    viewModel.receiptImage = ReceiptImage.Loaded(result)
                } else {
                    viewModel.receiptImage = ReceiptImage.Error("Not found")
                }
            }
        selectImage = { selectImageContract.launch(arrayOf("image/*")) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SplitTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent(
                        viewModel = viewModel,
                        selectImage = selectImage
                    )
                }
            }
        }
    }
}
