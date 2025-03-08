package com.ujjwal.aiwall

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

class UnsplashImageSearch {

    private val _API_KEY: String = "1P3e-w6y85C2x9R1jASNkKY_-vPOLBloLwmL3tfGpJw"
    private val API_KEY: String get() = _API_KEY

    private val _URL: String = "https://api.unsplash.com/photos/random?orientation=portrait&count=30&query="
    private val URL: String get() = _URL

    private var imageJson: String = ""

    private val client = OkHttpClient()

    suspend fun searchImages(
        query: String = "Futuristic Car", height: String = "1920", width: String = "1080"
    ): List<String>? = withContext(Dispatchers.IO) {
        val url = "$URL$query"
        try {
            val request = Request.Builder().url(url).addHeader("Authorization", "Client-ID $API_KEY").build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                imageJson = responseBody ?: ""
            } else {
                println("Unsplash API request failed: ${response.code} - ${response.message}")
            }
        } catch (e: Exception) {
            println("Unsplash url  ${url} response API error ${e.message}")
            e.printStackTrace()
        }
        return@withContext extractImageUrls(imageJson, height, width);
    }

    private fun extractImageUrls(jsonString: String, height: String, width: String): List<String> {
        val imageUrls = mutableListOf<String>()
        try {
            val results = JSONArray(jsonString)
            for (i in 0 until results.length()) {
                val result = results.getJSONObject(i)
                val urls = result.getJSONObject("urls")
                var regularUrl = urls.getString("raw")
                regularUrl = "$regularUrl&h=$height&w=$width&dpr=5"
                imageUrls.add(regularUrl)
            }
        } catch (e: Exception) {
            e.stackTrace
        }
        return imageUrls
    }

    @Composable
    fun getScreenDimensions(): List<String> {
        val localConfig = LocalConfiguration.current
        val height = localConfig.screenHeightDp.toString()
        val width = localConfig.screenWidthDp.toString()

        return listOf(height, width)
    }

}