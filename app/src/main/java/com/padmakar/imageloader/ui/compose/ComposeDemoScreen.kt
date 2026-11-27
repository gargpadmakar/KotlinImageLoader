package com.padmakar.imageloader.ui.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.padmakar.imageloader.R
import com.padmakar.imageloader.compose.AsyncImage
import com.padmakar.imageloader.compose.AsyncImageState
import com.padmakar.imageloader.core.model.ImageResult
import com.padmakar.imageloader.data.TestImages

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeDemoScreen(
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Features Demo", "50+ Item List")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jetpack Compose Demo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(
                            text = "←",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> ComposeFeaturesContent()
                1 -> ComposeListContent()
            }
        }
    }
}

@Composable
private fun ComposeFeaturesContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Single Success & Error / Retry
        item {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Single Image & Error/Retry",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    var urlToLoad by rememberSaveable { mutableStateOf(TestImages.SINGLE_SUCCESS_URL) }
                    var retryCount by rememberSaveable { mutableIntStateOf(0) }
                    var loadStatus by remember { mutableStateOf("Idle") }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = urlToLoad,
                                contentDescription = "Demo Image",
                                placeholder = painterResource(R.drawable.ic_placeholder),
                                error = painterResource(R.drawable.ic_error),
                                modifier = Modifier.fillMaxSize(),
                                onStateChanged = { state ->
                                    loadStatus = when (state) {
                                        is AsyncImageState.Loading -> "Loading..."
                                        is AsyncImageState.Success -> "Success (${state.result.dataSource})"
                                        is AsyncImageState.Error -> "Failed (Error state)"
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text("Status: $loadStatus", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    urlToLoad = TestImages.SINGLE_SUCCESS_URL
                                    retryCount++
                                }
                            ) {
                                Text("Load Success URL")
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedButton(
                                onClick = {
                                    urlToLoad = TestImages.INVALID_ERROR_URL
                                    retryCount++
                                }
                            ) {
                                Text("Load 404 URL")
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Large Image Downsampling
        item {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "2. Downsampling Large Image",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Original: 2500x1667 px rendered in small 120x80 dp area",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    var decodedDimensions by remember { mutableStateOf("Measuring...") }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(width = 120.dp, height = 80.dp)
                                .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                        ) {
                            AsyncImage(
                                model = TestImages.LARGE_IMAGE_URL,
                                contentDescription = "Downsampled Image",
                                placeholder = painterResource(R.drawable.ic_placeholder),
                                error = painterResource(R.drawable.ic_error),
                                modifier = Modifier.fillMaxSize(),
                                onStateChanged = { state ->
                                    if (state is AsyncImageState.Success) {
                                        val bmp = state.result.image as? android.graphics.Bitmap
                                        decodedDimensions = if (bmp != null) {
                                            "Decoded to: ${bmp.width}x${bmp.height} px"
                                        } else "Loaded"
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = decodedDimensions,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Section 3: Rapid URL Switcher (Cancellation & Stale Image Check)
        item {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "3. Rapid Cancellation Check",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Click rapidly. Stale requests cancel; requested ID must match loaded ID.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    var currentRapidIndex by rememberSaveable { mutableIntStateOf(0) }
                    val currentItem = TestImages.RAPID_TEST_IMAGES[currentRapidIndex]
                    var lastCompletedId by remember { mutableStateOf<Int?>(null) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = currentItem.second,
                                contentDescription = "Rapid Image ${currentItem.first}",
                                placeholder = painterResource(R.drawable.ic_placeholder),
                                error = painterResource(R.drawable.ic_error),
                                modifier = Modifier.fillMaxSize(),
                                onStateChanged = { state ->
                                    if (state is AsyncImageState.Success) {
                                        lastCompletedId = currentItem.first
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text("Target ID: ${currentItem.first}", style = MaterialTheme.typography.titleSmall)
                            Text("Rendered ID: ${lastCompletedId ?: "In Flight"}", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    currentRapidIndex = (currentRapidIndex + 1) % TestImages.RAPID_TEST_IMAGES.size
                                }
                            ) {
                                Text("Next Image Quickly")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComposeListContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(TestImages.LIST_IMAGES, key = { it.first }) { (index, url) ->
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .border(0.5.dp, Color.LightGray, RoundedCornerShape(4.dp))
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = "List Item #$index",
                            placeholder = painterResource(R.drawable.ic_placeholder),
                            error = painterResource(R.drawable.ic_error),
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Item #$index",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Recycling verification item",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
