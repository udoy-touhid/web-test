package com.cature.webtest

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler
import com.cature.webtest.ObjectDetectorHelper.Companion.MODEL_EFFICIENTDETV0
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.tensorflow.lite.task.vision.detector.Detection
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit


private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)

        findViewById<TextView>(R.id.back).setOnClickListener {
            webView.goBack()
            Toast.makeText(this, "Back", Toast.LENGTH_SHORT).show()

        }
        findViewById<TextView>(R.id.copy).setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("url", webView.url)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.paste).setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val data = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
            if (data != null) {
                webView.loadUrl(data)
                Toast.makeText(this, "Pasted", Toast.LENGTH_SHORT).show()
            }
        }
        val assetLoader =
            CustomWebViewAssetLoader.Builder().addPathHandler("/assets/", AssetsPathHandler(this))
                .build()

        webView.apply {
            settings.javaScriptEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.allowFileAccessFromFileURLs = true
            settings.allowUniversalAccessFromFileURLs = true
            webViewClient = WVClient(assetLoader, context)

            // Disable web security for testing purposes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                settings.setAllowUniversalAccessFromFileURLs(true)
                settings.allowFileAccessFromFileURLs = true
            }
        }

        webView.settings.setAllowFileAccessFromFileURLs(true)
        webView.settings.setAllowUniversalAccessFromFileURLs(true)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }
        }
//        val url = "https://www.google.com/search?q=thob&sca_esv=a9f733130965a78f&biw=412&bih=784&prmd=sivmnbz&source=lnms&ved=1t:200715&ictx=111&tbm=isch"
//        val url = "https://twitter.com/elonmusk"
//        val url = "https://youtube.com"
//        val url = "https://amazon.com"
//        val url = "https://www.prothomalo.com/"
        val url = "https://dailybhorerdak.com/details.php?id=230519"
        webView.loadUrl(url)
    }
}

class WVClient(
    private val assetLoader: CustomWebViewAssetLoader,
    context: Context,
) : WebViewClient() {

    private var objectDetectorHelper: ObjectDetectorHelper

    init {
        objectDetectorHelper = ObjectDetectorHelper(
            context = context, currentModel = MODEL_EFFICIENTDETV0
        )
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        Log.e("shouldOverride", "url ${request?.url.toString()}")
        return super.shouldOverrideUrlLoading(view, request)
    }

    override fun shouldInterceptRequest(
        webView: WebView?, request: WebResourceRequest?
    ): WebResourceResponse? {

        Log.e("shouldIntercept", "url ${request?.url.toString()}")
        if (request == null || webView == null) {
            return assetLoader.shouldInterceptRequest(request!!.url)
        }

        try {
            val response = ApiClient.apiService.download(request.url.toString()).execute()
            val contentType = response.headers().get("Content-Type")
            Log.e("contentType", contentType ?: "contentType")

            if (contentType?.startsWith("image") == false) {
                return assetLoader.shouldInterceptRequest(request.url)
            }

            val regex = "[^A-Za-z0-9.]+"
            val fileName = request.url.toString().replace(Regex(regex), "_")
            val responseFilePath = webView.context.filesDir.path + "/" + fileName
            //todo apply caching and avoid re-downloading and processing of same image
            download(response, responseFilePath)

            Log.e("localStoragePath", responseFilePath)

            var imageUrl = request.url.toString()
            imageUrl = imageUrl.replaceFirst("https://", "")
            imageUrl = imageUrl.replaceFirst("http://", "")
            runBlocking {
                runCatching {
                    withContext(context = Dispatchers.Main) {
                        objectDetectorHelper.detect(BitmapFactory.decodeFile(responseFilePath),
                            0,
                            object : ObjectDetectorHelper.DetectorListener {
                                override fun onError(error: String) {
                                    Log.e("onError", "$error")
                                }

                                override fun onResults(
                                    results: MutableList<Detection>?,
                                    inferenceTime: Long,
                                    imageHeight: Int,
                                    imageWidth: Int
                                ) {

                                    var personFound = false

                                    results?.forEach { result ->
                                        val data = result.categories.find {
                                            it.label.equals(
                                                "person", ignoreCase = true
                                            )
                                        }
                                        if (data != null) {
                                            personFound = true
                                        }
                                    }
                                    if (personFound) {
                                        Log.e("onResults", "person found on https://$imageUrl")

                                    } else {
                                        Log.e("onResults", "person not found on https://$imageUrl")

                                    }
                                    if (!personFound) return
                                    val js = """
                                    (function() {
                                        var blurFunc = function (image){
                                           console.log('blur:' + image);
                                           console.log('blur:' + '$imageUrl');
                                           console.log('prev blur filter:' + image.style.cssText);
                                           var blurAmt = "blur(" + 10 + "px) "
                                           image.style.cssText += ';filter: ' + blurAmt + ' !important;'
                                           console.log('blur applied:' + '10px');
                                       };
                                       console.log('jsrun');

                                       document.querySelectorAll("img[src*='$imageUrl']").forEach(blurFunc);
                                       document.querySelectorAll("img[srcset*='$imageUrl']").forEach(blurFunc);
                                       document.querySelectorAll("div[style*='$imageUrl']").forEach(blurFunc);
                                       document.querySelectorAll("video[style*='$imageUrl']").forEach(blurFunc);
                                       document.querySelectorAll("iframe[style*='$imageUrl']").forEach(blurFunc);
                                       document.querySelectorAll("a[style*='$imageUrl']").forEach(blurFunc);
                                       document.querySelectorAll("i[style*='$imageUrl']").forEach(blurFunc);
                                       console.log('jsrun'+document.querySelectorAll("img[src*='$imageUrl']").length);

                            
                                      })()
                                """.trimIndent()
                                    webView.evaluateJavascript(js, null)

                                }

                            })
                    }
                }
            }

            val inputStream: InputStream = FileInputStream(File(responseFilePath))
            return WebResourceResponse(contentType, "UTF-8", inputStream)
        } catch (e: Exception) {
            Log.e("exp", e.message ?: "")
            return assetLoader.shouldInterceptRequest(request.url)
        }
    }
}

fun download(response: Response<ResponseBody>, path: String) {
    response.body()?.byteStream().use { input ->
        FileOutputStream(File(path)).use { output ->
            input?.copyTo(output)
        }
    }

}

interface ApiService {
    @GET
    fun download(@Url url: String): Call<ResponseBody>

}

class ApiClient {

    companion object {

        val apiService by lazy { create() }
        fun create(): ApiService {
            val httpClient by lazy {
                OkHttpClient.Builder().callTimeout(1, TimeUnit.MINUTES)
                    .connectTimeout(20, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS).build()
            }

            val retrofit by lazy {
                Retrofit.Builder()
                    // .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl("https://localhost.com")
                    // .client(httpClient)
                    .build()
            }
            return retrofit.create(ApiService::class.java)
        }
    }

}