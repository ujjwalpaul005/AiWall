package com.ujjwal.aiwall

import android.Manifest
import android.app.WallpaperManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.ujjwal.aiwall.ui.theme.AiWallTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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
        mutableStateOf(checkPermissionOf(Manifest.permission.SET_WALLPAPER, cntxt))
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
                launcher.launch(Manifest.permission.SET_WALLPAPER)
            }) {
                Text("Please give SET_WALLPAPER access")
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
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Title
        Text(
            text = "AI Wall",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
        )

        // Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                placeholder = {
                    Text(
                        "Search wallpapers...",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.textFieldColors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    containerColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
                textStyle = MaterialTheme.typography.bodyLarge
            )

            Button(
                onClick = {
                    triggerSearch = true
                    isLoading = true
                    Toast.makeText(context, "Searching for: $searchQuery", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Search")
            }

            LaunchedEffect(triggerSearch) {
                if(triggerSearch){
                    imageUrls = imageFinder.searchImages(searchQuery, dimensions[0], dimensions[1])
                    triggerSearch = false
                    isLoading = false
                }
            }
        }

        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Image grid
        imageUrls?.let {
            if (it.isNotEmpty()) {
                PhotosGridScreen(imageUrls = it)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No images found. Try a different search term.",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } ?: Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Search for wallpapers to get started",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge
            )
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
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(imageUrls.size) { index ->
            PhotoCard(photoUrl = imageUrls[index])
        }
    }
}

@Composable
fun PhotoCard(photoUrl: String) {
    var showFullImage by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val wallpaperManager = remember { WallpaperManager.getInstance(context) }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        ),
        modifier = Modifier
            .aspectRatio(0.75f)  // Portrait aspect ratio for wallpapers
            .padding(4.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable {
                showFullImage = true
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context = LocalContext.current)
                    .data(photoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Wallpaper image",
                error = painterResource(R.drawable.ic_launcher_foreground),
                placeholder = painterResource(R.drawable.ic_launcher_foreground),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // Full screen image dialog
    if (showFullImage) {
        Dialog(
            onDismissRequest = { showFullImage = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(16.dp)
                    .clickable { showFullImage = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.9f)
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Image container (80% of screen)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(1f)
                            .aspectRatio(0.75f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context = LocalContext.current)
                                .data(photoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Wallpaper image fullscreen",
                            error = painterResource(R.drawable.ic_launcher_foreground),
                            placeholder = painterResource(R.drawable.ic_launcher_foreground),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Set wallpaper button
                    Button(
                        onClick = {
                            setWallpaper(context, photoUrl, wallpaperManager)
                            showFullImage = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text(
                            "Set as Wallpaper",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

private fun setWallpaper(context: Context, imageUrl: String, wallpaperManager: WallpaperManager) {
    // Use coroutines to handle the image loading and setting
    val scope = CoroutineScope(Dispatchers.IO)
    scope.launch {
        try {
            // Download the image
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .build()

            val drawable = context.imageLoader.execute(request).drawable

            // Convert drawable to bitmap if possible
            if (drawable != null) {
                val bitmap = (drawable as? BitmapDrawable)?.bitmap

                bitmap?.let {
                    // Set the wallpaper on the IO thread
                    wallpaperManager.setBitmap(it)

                    // Show success message on the main thread
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Wallpaper set successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Failed to load image",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Error setting wallpaper: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
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