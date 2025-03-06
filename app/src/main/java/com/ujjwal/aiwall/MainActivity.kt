package com.ujjwal.aiwall

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ujjwal.aiwall.ui.theme.AiWallTheme


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AiWallTheme {
                GetAndShowWallpaper()
            }
        }
    }
}

    @RequiresApi(Build.VERSION_CODES.R)
    @Composable
    fun GetAndShowWallpaper() {
        val cntxt : Context = LocalContext.current
        var isStoragePermissionGranted by remember {
            mutableStateOf(checkPermissionOf(Manifest.permission.CAMERA, cntxt))
        }
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                if (isGranted) {
                    isStoragePermissionGranted = true
                }

            })

        if (!isStoragePermissionGranted) {
            Column {
                Button(onClick = {
                    launcher.launch(Manifest.permission.CAMERA)
                }) {
                    Text("Please give storage access")
                }
            }
        } else {
            WallpaperScreen()
        }


    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperScreen() {
    var searchQuery by remember { mutableStateOf("Cars") }
    val imageFinder = UnsplashImageSearch()
    val dimensions = imageFinder.getScreenDimensions()
    var imageUrls by remember { mutableStateOf<List<String>?>(null) }
    var triggerSearch by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 4.dp),
                placeholder = {
                    Text(
                        "Search Images",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.textFieldColors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    containerColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.bodyLarge
            )
            IconButton(onClick = {
                triggerSearch = true;
                Toast.makeText(context, "Searching for: $searchQuery", Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            }
            LaunchedEffect(triggerSearch) {
                if(triggerSearch){
                    imageUrls = imageFinder.searchImages(searchQuery, dimensions[0], dimensions[1])
                    triggerSearch = false
                }
            }
        }

        imageUrls?.let {
            PhotosGridScreen(imageUrls = it)
        }
    }
}


    private fun checkPermissionOf(permit: String, cntxt: Context): Boolean {
        return ContextCompat.checkSelfPermission(cntxt, permit) == PackageManager.PERMISSION_GRANTED
    }

    @Composable
    fun PhotosGridScreen(
        imageUrls: List<String>,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(imageUrls.size) { index ->
                PhotoCard(photoUrl = imageUrls[index])
            }
        }
    }

    @Composable
    fun PhotoCard(photoUrl: String) {
        Card(
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.aspectRatio(1f) // Ensure square aspect ratio
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context = LocalContext.current).data(photoUrl)
                    .crossfade(true).build(),
                "photo with this",
                error = painterResource(R.drawable.ic_launcher_foreground),
                placeholder = painterResource(R.drawable.ic_launcher_foreground),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }


    @RequiresApi(Build.VERSION_CODES.R)
    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        AiWallTheme {
            GetAndShowWallpaper()
        }
    }

