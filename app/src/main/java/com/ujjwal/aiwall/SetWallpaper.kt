package com.ujjwal.aiwall

import android.Manifest
import android.app.WallpaperManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min


@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun GetAndShowWallpaper() {
    val cntxt: Context = LocalContext.current
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

private fun checkPermissionOf(permit: String, cntxt: Context): Boolean {
    return ContextCompat.checkSelfPermission(cntxt, permit) == PackageManager.PERMISSION_GRANTED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperScreen() {
    var searchQuery by remember { mutableStateOf("Cars") }
    val imageFinder = UnsplashImageSearch()
    val dimensions = imageFinder.getScreenDimensions()
    println("dimensions $dimensions")
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
                if (triggerSearch) {
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
                            .fillMaxWidth(0.8f)
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

                    // Wallpaper preview and scaling options
                    var selectedScalingOption by remember { mutableStateOf(WallpaperScaling.SCALE_CROP) }
                    var selectedScreenOption by remember { mutableStateOf(WallpaperScreenOption.BOTH) }

                    Text(
                        "Preview & Scaling Options",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Preview of different scaling options
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .background(Color.Black)
                    ) {
                        // Phone frame outline
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .fillMaxHeight(0.95f)
                                .align(Alignment.Center)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            // Preview image with selected scaling
                            AsyncImage(
                                model = ImageRequest.Builder(context = LocalContext.current)
                                    .data(photoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Wallpaper preview",
                                error = painterResource(R.drawable.ic_launcher_foreground),
                                placeholder = painterResource(R.drawable.ic_launcher_foreground),
                                contentScale = when (selectedScalingOption) {
                                    WallpaperScaling.SCALE_CROP -> ContentScale.Crop
                                    WallpaperScaling.SCALE_FIT -> ContentScale.Fit
                                    WallpaperScaling.STRETCH -> ContentScale.FillBounds
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Label indicating current screen selection
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 8.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = when (selectedScreenOption) {
                                        WallpaperScreenOption.HOME -> "Home Screen"
                                        WallpaperScreenOption.LOCK -> "Lock Screen"
                                        WallpaperScreenOption.BOTH -> "Both Screens"
                                    },
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Scaling options
                    Text(
                        "Scaling Options",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        WallpaperScaling.values().forEach { option ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clickable { selectedScalingOption = option }
                                    .background(
                                        if (selectedScalingOption == option)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .border(
                                            width = 2.dp,
                                            color = if (selectedScalingOption == option)
                                                MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(4.dp)
                                ) {
                                    // Phone screen outline
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                                                    )
                                                )
                                            )
                                    ) {
                                        // Image representation based on scaling type
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .let {
                                                    when (option) {
                                                        WallpaperScaling.SCALE_CROP -> it
                                                            .fillMaxWidth(1.2f) // Overflow to show cropping
                                                            .fillMaxHeight(0.9f)
                                                        WallpaperScaling.SCALE_FIT -> it
                                                            .fillMaxWidth(0.7f)
                                                            .fillMaxHeight(0.9f)
                                                        WallpaperScaling.STRETCH -> it
                                                            .fillMaxSize(0.9f)
                                                    }
                                                }
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(
                                                            MaterialTheme.colorScheme.primary,
                                                            MaterialTheme.colorScheme.secondary
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                }

                                Text(
                                    text = option.displayName,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Screen option selection (Home/Lock/Both)
                    Text(
                        "Apply To",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        WallpaperScreenOption.values().forEach { option ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clickable { selectedScreenOption = option }
                                    .background(
                                        if (selectedScreenOption == option)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = when (option) {
                                        WallpaperScreenOption.HOME -> "Home"
                                        WallpaperScreenOption.LOCK -> "Lock"
                                        WallpaperScreenOption.BOTH -> "Both"
                                    },
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Set wallpaper button
                    Button(
                        onClick = {
                            setWallpaper(
                                context,
                                photoUrl,
                                wallpaperManager,
                                selectedScalingOption,
                                selectedScreenOption
                            )
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
                            text = "Apply Wallpaper",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

// Enum for wallpaper scaling options
enum class WallpaperScaling(val displayName: String) {
    SCALE_CROP("Center Crop"),
    SCALE_FIT("Fit Screen"),
    STRETCH("Stretch")
}

// Enum for wallpaper screen options
enum class WallpaperScreenOption {
    HOME, LOCK, BOTH
}

private fun setWallpaper(
    context: Context,
    imageUrl: String,
    wallpaperManager: WallpaperManager,
    scaling: WallpaperScaling = WallpaperScaling.SCALE_CROP,
    screenOption: WallpaperScreenOption = WallpaperScreenOption.BOTH
) {
    // Use coroutines to handle the image loading and setting
    val scope = CoroutineScope(Dispatchers.IO)
    scope.launch {
        try {
            // Download the image
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .build()

            val drawable = context.imageLoader.execute(request).drawable

            // Convert drawable to bitmap if possible
            if (drawable != null) {
                val originalBitmap = (drawable as? BitmapDrawable)?.bitmap

                originalBitmap?.let { bitmap ->
                    // Get screen dimensions for proper scaling
                    val displayMetrics = context.resources.displayMetrics
                    val screenWidth = displayMetrics.widthPixels
                    val screenHeight = displayMetrics.heightPixels

                    // Determine which flags to use based on screen option
                    val wallpaperFlag = when (screenOption) {
                        WallpaperScreenOption.HOME -> WallpaperManager.FLAG_SYSTEM
                        WallpaperScreenOption.LOCK -> WallpaperManager.FLAG_LOCK
                        WallpaperScreenOption.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                    }

                    // Process bitmap based on scaling option
                    val finalBitmap = when (scaling) {
                        WallpaperScaling.SCALE_CROP -> {
                            // Center crop (default Android behavior)
                            bitmap
                        }

                        WallpaperScaling.SCALE_FIT -> {
                            // Scale to fit screen without cropping
                            // Calculate ratio to maintain aspect ratio
                            val ratioX = screenWidth.toFloat() / bitmap.width
                            val ratioY = screenHeight.toFloat() / bitmap.height
                            val ratio = minOf(ratioX, ratioY)

                            val newWidth = (bitmap.width * ratio).toInt()
                            val newHeight = (bitmap.height * ratio).toInt()

                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

                            // Create a new bitmap with screen dimensions (black background)
                            val resultBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(resultBitmap)
                            canvas.drawColor(Color.Black.toArgb())

                            // Draw the scaled bitmap centered
                            val left = (screenWidth - newWidth) / 2f
                            val top = (screenHeight - newHeight) / 2f
                            canvas.drawBitmap(scaledBitmap, left, top, null)

                            resultBitmap
                        }

                        WallpaperScaling.STRETCH -> {
                            // Stretch to fill the entire screen
                            Bitmap.createScaledBitmap(
                                bitmap,
                                screenWidth,
                                screenHeight,
                                true
                            )
                        }
                    }

                    // Set the wallpaper using the appropriate flag
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wallpaperManager.setBitmap(
                            finalBitmap,
                            null,
                            true,
                            wallpaperFlag
                        )
                    } else {
                        // For older Android versions that don't support different wallpapers
                        wallpaperManager.setBitmap(finalBitmap)
                    }

                    // Show success message on the main thread
                    withContext(Dispatchers.Main) {
                        val screenMessage = when (screenOption) {
                            WallpaperScreenOption.HOME -> "Home Screen"
                            WallpaperScreenOption.LOCK -> "Lock Screen"
                            WallpaperScreenOption.BOTH -> "Home & Lock Screens"
                        }

                        Toast.makeText(
                            context,
                            "Wallpaper set for $screenMessage with ${scaling.displayName} scaling",
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
            e.printStackTrace()
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