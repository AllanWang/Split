package com.pitchedapps.split.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.pitchedapps.split.ocr.ReceiptOcr
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
            when (val data = viewModel.receiptImage) {
                is ReceiptImage.Pending -> {
                    SelectScreen(action = selectImage)
                }
                is ReceiptImage.Error -> {
                    SelectScreen(action = selectImage, message = data.message)
                }
                is ReceiptImage.Loaded -> {
                    BackHandler {
                        viewModel.receiptImage = ReceiptImage.Pending
                    }
                    ParsingScreen(data = data, onResult = {
                        if (it == null) {
                            viewModel.receiptImage = ReceiptImage.Error("could not load data")
                        } else {
                            viewModel.receiptImage =
                                ReceiptImage.Parsed(uri = data.uri, data = it)
                        }
                    })
                }
                is ReceiptImage.Parsed -> {
                    BackHandler {
                        viewModel.receiptImage = ReceiptImage.Pending
                    }
                    PreviewScreen(data = data)
                }
            }
        }
    }
}

@Composable
fun SelectScreen(action: suspend () -> Unit, message: String? = null) {
    val scope = rememberCoroutineScope()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.7f)
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = {
                scope.launch {
                    action()
                }
            }) {
                Text("Select Image")
            }
            Spacer(modifier = Modifier.height(24.dp))
            if (message != null) {
                Text(text = message, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun ParsingScreen(data: ReceiptImage.Loaded, onResult: (ReceiptOcr.Result?) -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.Center)
        )
    }

    LaunchedEffect(data) {
        scope.launch {
            val result = ReceiptOcr.read(context, data.uri)
            onResult(result)
        }
    }
}

@Composable
fun PreviewScreen(data: ReceiptImage.Parsed) {
    var showImage by remember {
        mutableStateOf(false)
    }

    Column {
        Text("Preview")
        Image(
            painter = rememberAsyncImagePainter(data.uri),
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
