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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontFamily
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


@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun GetAndShowWallpaper() {
    val cntxt: Context = LocalContext.current
    var isStoragePermissionGranted by remember {
        mutableStateOf(checkPermissionOf(Manifest.permission.SET_WALLPAPER, cntxt))
    }
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission(),
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
    var searchQuery by remember { mutableStateOf("") }
    val imageFinder = UnsplashImageSearch()
    val dimensions = imageFinder.getScreenDimensions()
    println("dimensions $dimensions")
    var imageUrls by remember { mutableStateOf<List<String>?>(null) }
    var triggerSearch by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 42.dp, 16.dp, 8.dp),
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
                        "Search wallpapers...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary
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
//                    Toast.makeText(context, "Searching for: $searchQuery", Toast.LENGTH_SHORT).show()
                }, colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ), modifier = Modifier
                    .padding(start = 8.dp)
                    .height(48.dp), shape = RoundedCornerShape(16.dp)
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
                    .padding(16.dp), contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp)
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
                        .padding(32.dp), contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No images found. Try a different search term.",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } ?: Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Good wallpapers found with a search, to get started",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, fontFamily = FontFamily.Serif

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
    var showScalingPreview by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val wallpaperManager = remember { WallpaperManager.getInstance(context) }

    Card(
        shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp, pressedElevation = 8.dp
        ), modifier = Modifier
            .aspectRatio(0.75f)  // Portrait aspect ratio for wallpapers
            .padding(4.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable {
                showScalingPreview = true
            }, colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context = LocalContext.current).data(photoUrl).crossfade(true).build(),
                contentDescription = "Wallpaper image",
                error = painterResource(R.drawable.ic_image_loading),
                placeholder = painterResource(R.drawable.ic_image_loading),
                contentScale = ContentScale.Crop,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }

    // Scaling preview dialog
    if (showScalingPreview) {
        Dialog(
            onDismissRequest = { showScalingPreview = false }, properties = DialogProperties(
                dismissOnBackPress = true, dismissOnClickOutside = true, usePlatformDefaultWidth = false
            )
        ) {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable { showScalingPreview = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp)
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Wallpaper scaling options
                    var selectedScalingOption by remember { mutableStateOf(WallpaperScaling.SCALE_CROP) }

                    // Preview tabs for Home Screen and Lock Screen
                    var selectedTab by remember { mutableStateOf(WallpaperPreviewTab.HOME_SCREEN) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        WallpaperPreviewTab.values().forEach { tab ->
                            Box(modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .background(
                                    if (selectedTab == tab) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                )
                                .clickable { selectedTab = tab }
                                .padding(8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = tab.displayName,
                                    color = if (selectedTab == tab) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    // Scaling Preview with Device Frame
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(0.6f)
                            .aspectRatio(0.5f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        // Phone display area
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                        ) {
                            // Background image (wallpaper)
                            AsyncImage(
                                model = ImageRequest.Builder(context = LocalContext.current).data(photoUrl)
                                    .crossfade(true).build(),
                                contentDescription = "Wallpaper preview",
                                error = painterResource(R.drawable.ic_wallpaper_loading),
                                placeholder = painterResource(R.drawable.ic_wallpaper_loading),
                                contentScale = when (selectedScalingOption) {
                                    WallpaperScaling.SCALE_CROP -> ContentScale.Crop
                                    WallpaperScaling.SCALE_FIT -> ContentScale.Fit
                                    WallpaperScaling.STRETCH -> ContentScale.FillBounds
                                },
                                modifier = Modifier.align(Alignment.Center)
                            )

                            // Phone UI overlay based on selected tab
                            when (selectedTab) {
                                WallpaperPreviewTab.HOME_SCREEN -> HomeScreenOverlay()
                                WallpaperPreviewTab.LOCK_SCREEN -> LockScreenOverlay()
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(15.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(0.8f), horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        WallpaperScaling.values().forEach { option ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clickable { selectedScalingOption = option }
                                    .background(
                                        if (selectedScalingOption == option) MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.2f
                                        )
                                        else Color.Transparent, RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .border(
                                            width = 2.dp,
                                            color = if (selectedScalingOption == option) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                when (option) {
                                                    WallpaperScaling.SCALE_CROP -> Brush.linearGradient(
                                                        listOf(
                                                            MaterialTheme.colorScheme.primary,
                                                            MaterialTheme.colorScheme.secondary
                                                        )
                                                    )

                                                    WallpaperScaling.SCALE_FIT -> Brush.linearGradient(
                                                        listOf(
                                                            MaterialTheme.colorScheme.primary,
                                                            MaterialTheme.colorScheme.secondary
                                                        )
                                                    )

                                                    WallpaperScaling.STRETCH -> Brush.linearGradient(
                                                        listOf(
                                                            MaterialTheme.colorScheme.tertiary,
                                                            MaterialTheme.colorScheme.primary
                                                        )
                                                    )
                                                }
                                            )
                                    )

                                    // Show icon representation of each option
                                    Box(
                                        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                                    ) {
                                        when (option) {
                                            WallpaperScaling.SCALE_CROP -> Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.8f)
                                                    .fillMaxHeight()
                                                    .background(Color.White.copy(alpha = 0.4f))
                                            )

                                            WallpaperScaling.SCALE_FIT -> Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.7f)
                                                    .fillMaxHeight(0.7f)
                                                    .background(Color.White.copy(alpha = 0.4f))
                                            )

                                            WallpaperScaling.STRETCH -> Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.White.copy(alpha = 0.4f))
                                            )
                                        }
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

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Apply Wallpaper For",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(0.8f), horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Set as home screen button
                        Button(
                            onClick = {
                                setWallpaper(
                                    context,
                                    photoUrl,
                                    wallpaperManager,
                                    selectedScalingOption,
                                    WallpaperManager.FLAG_SYSTEM
                                )
                                showScalingPreview = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .padding(end = 4.dp),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Text(
                                "Home Screen",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        // Set as lock screen button
                        Button(
                            onClick = {
                                setWallpaper(
                                    context,
                                    photoUrl,
                                    wallpaperManager,
                                    selectedScalingOption,
                                    WallpaperManager.FLAG_LOCK
                                )
                                showScalingPreview = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .padding(start = 4.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                "Lock Screen",
                                color = MaterialTheme.colorScheme.onSecondary,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Set as both screens button
                    Button(
                        onClick = {
                            setWallpaper(
                                context,
                                photoUrl,
                                wallpaperManager,
                                selectedScalingOption,
                                WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                            )
                            showScalingPreview = false
                        }, colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ), modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(48.dp), shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            "Both Screens",
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
    SCALE_CROP("Center Crop"), SCALE_FIT("Fit Screen"), STRETCH("Stretch")
}

// Enum for preview tabs
enum class WallpaperPreviewTab(val displayName: String) {
    HOME_SCREEN("Home Screen"), LOCK_SCREEN("Lock Screen")
}

@Composable
fun HomeScreenOverlay() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Status bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "12:34", color = Color.White, style = MaterialTheme.typography.bodySmall
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.White, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.White, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.White, CircleShape)
                )
            }
        }

        // App icons grid
        LazyVerticalGrid(columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 40.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = {
                items(4) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        )
                    }
                }
            })

    }
}

@Composable
fun LockScreenOverlay() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Status bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "12:34", color = Color.White, style = MaterialTheme.typography.bodySmall
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.White, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.White, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.White, CircleShape)
                )
            }
        }

        // Lock screen clock
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large digital clock
            Text(
                text = "12:34", color = Color.White, style = MaterialTheme.typography.displayLarge
            )

            // Date
            Text(
                text = "Monday, January 1",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

        }

        // Bottom swipe indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .align(Alignment.BottomCenter),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(4.dp)
                    .background(Color.White, RoundedCornerShape(2.dp))
            )
        }

        // Camera and flashlight icons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .align(Alignment.BottomCenter), horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Flashlight icon
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.White.copy(alpha = 0.7f), CircleShape)
                )
            }

            // Camera icon
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.White.copy(alpha = 0.7f), CircleShape)
                )
            }
        }
    }
}

private fun setWallpaper(
    context: Context,
    imageUrl: String,
    wallpaperManager: WallpaperManager,
    scaling: WallpaperScaling = WallpaperScaling.SCALE_CROP,
    which: Int = WallpaperManager.FLAG_SYSTEM
) {
    // Use coroutines to handle the image loading and setting
    val scope = CoroutineScope(Dispatchers.IO)
    scope.launch {
        try {
            // Download the image
            val request = ImageRequest.Builder(context).data(imageUrl).allowHardware(false).build()

            val drawable = context.imageLoader.execute(request).drawable

            // Convert drawable to bitmap if possible
            if (drawable != null) {
                val originalBitmap = (drawable as? BitmapDrawable)?.bitmap

                originalBitmap?.let { bitmap ->
                    // Get screen dimensions for proper scaling
                    val displayMetrics = context.resources.displayMetrics
                    val screenWidth = displayMetrics.widthPixels
                    val screenHeight = displayMetrics.heightPixels

                    // Apply the selected scaling option
                    when (scaling) {
                        WallpaperScaling.SCALE_CROP -> {
                            // Center crop (default Android behavior)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                wallpaperManager.setBitmap(
                                    bitmap, null, true, which
                                )
                            } else {
                                wallpaperManager.setBitmap(bitmap)
                            }
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
                            val finalBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(finalBitmap)
                            canvas.drawColor(Color.Black.toArgb())

                            // Draw the scaled bitmap centered
                            val left = (screenWidth - newWidth) / 2f
                            val top = (screenHeight - newHeight) / 2f
                            canvas.drawBitmap(scaledBitmap, left, top, null)

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                wallpaperManager.setBitmap(
                                    finalBitmap, null, true, which
                                )
                            } else {
                                wallpaperManager.setBitmap(finalBitmap)
                            }
                        }

                        WallpaperScaling.STRETCH -> {
                            // Stretch to fill the entire screen
                            val stretchedBitmap = Bitmap.createScaledBitmap(
                                bitmap, screenWidth, screenHeight, true
                            )

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                wallpaperManager.setBitmap(
                                    stretchedBitmap, null, true, which
                                )
                            } else {
                                wallpaperManager.setBitmap(stretchedBitmap)
                            }
                        }
                    }

                    // Get the screen type name for the toast message
                    val screenType = when (which) {
                        WallpaperManager.FLAG_SYSTEM -> "Home Screen"
                        WallpaperManager.FLAG_LOCK -> "Lock Screen"
                        WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK -> "Both Screens"
                        else -> "Wallpaper"
                    }

                    // Show success message on the main thread
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context, "Wallpaper set to $screenType with ${scaling.displayName} scaling", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context, "Failed to load image", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context, "Error setting wallpaper: ${e.message}", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}