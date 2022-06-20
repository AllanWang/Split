package com.pitchedapps.split.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.rememberAsyncImagePainter
import com.pitchedapps.split.ui.theme.SplitTheme
import kotlinx.coroutines.launch

@Composable
fun MainContent(viewModel: MainViewModel, selectImage: suspend () -> Unit) {

    SplitTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box {
                when (val receiptImage: ReceiptImage = viewModel.receiptImage) {
                    is ReceiptImage.Loaded -> {
                        BackHandler {
                            viewModel.receiptImage = ReceiptImage.Pending
                        }
                        PreviewScreen(receiptImage = receiptImage)
                    }
                    else -> {
                        SelectScreen(action = selectImage)
                    }
                }
            }
        }
    }
}

@Composable
fun SelectScreen(action: suspend () -> Unit) {
    val scope = rememberCoroutineScope()

    Button(onClick = {
        scope.launch {
            action()
        }
    }) {
        Text("Select Image")
    }
}

@Composable
fun PreviewScreen(receiptImage: ReceiptImage.Loaded) {
    Column {
        Text("Preview")
        Image(
            painter = rememberAsyncImagePainter(receiptImage.uri),
            contentDescription = "Preview Image"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SplitTheme {
        Text("Android")
    }
}
