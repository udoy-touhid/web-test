package com.cature.webtest

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.*
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
        val url = "https://www.google.com/search?q=thob&sca_esv=a9f733130965a78f&biw=412&bih=784&prmd=sivmnbz&source=lnms&ved=1t:200715&ictx=111&tbm=isch"
//        val url = "https://twitter.com/elonmusk"
        webView.loadUrl(url)
    }


}

//300 tk 180din
//600tk 1 year + 60din
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
//        injectJavaScript(view)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        Log.e("shouldOverride","url ${request?.url.toString()}")

        return super.shouldOverrideUrlLoading(view, request)
    }
    override fun shouldInterceptRequest(
        webView: WebView?, request: WebResourceRequest?
    ): WebResourceResponse? {
        Log.e("shouldIntercept","url ${request?.url.toString()}")
        listOf(".png", ".jpg", "jpeg").forEach {
            if (request == null || webView == null) return null
            if (request.url.toString().contains(it, ignoreCase = true)) {
                Log.e("shouldInterceptRequest", "img content " + request.url.toString())
                val localStoragePath =
                    webView.context.filesDir.path + "/" + request.url.lastPathSegment!!
                download(request.url.toString(), localStoragePath)

                var imageUrl = request.url.toString()
                imageUrl = imageUrl.replaceFirst("https://", "")
                imageUrl = imageUrl.replaceFirst("http://", "")
                runBlocking {
                    runCatching {
                        withContext(context = Dispatchers.Main) {
                            objectDetectorHelper.detect(BitmapFactory.decodeFile(localStoragePath),
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
                                            var data = result.categories.find {
                                                it.label.equals(
                                                    "person", ignoreCase = true
                                                )
                                            }
                                            if (data != null) {
                                                personFound = true
                                            }
                                        }
                                        if (personFound) {
                                            Log.e("onResults", "person found on $imageUrl")

                                        } else {
                                            Log.e("onResults", "person not found on $imageUrl")

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
                                       var images = document.querySelectorAll("img[src*='$imageUrl']");
                                       //if(images.length <1)
                                          document.querySelectorAll("img[srcset*='$imageUrl']").forEach(blurFunc);
                                       //else 
                                          images.forEach(blurFunc);
                            
                                      })()
                                """.trimIndent()
                                        webView.evaluateJavascript(js, null)

                                    }

                                })
                        }
                    }
                }
                try {

                    val `is`: InputStream = FileInputStream(File(localStoragePath))
                    return WebResourceResponse("image/png", "UTF-8", `is`)
                } catch (e: Exception) {
                    return null
                }

            }
        }
        // if(request?.method.equals("OPTIONS", ignoreCase = true))
        // {
        //Log.e("CORS","${request!!.url}")
        //return OptionsAllowResponse.build()
        //  }
        return assetLoader.shouldInterceptRequest(request!!.url)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun injectJavaScript(view: WebView?) {
        val initJs = """
        console.log("I AM RUNNING");
        (function() {
            var script = document.createElement('script');
            script.src = 'https://appassets.androidplatform.net/assets/human/human.js';
            document.head.appendChild(script);
            console.log("Human library is loaded");

            script.onload = async function() { // Mark the function as async
                console.log ("HUMAN JS LOADED");

                const modelsUrl = 'https://appassets.androidplatform.net/assets/models/human';

                const HUMAN_CONFIG = {
                    modelBasePath: modelsUrl,
                    backend: "humangl",
                    // debug: true,
                    cacheSensitivity: 0.9,
                    warmup: "none",
                    async: true,
                    filter: {
                        enabled: false,
                        // width: 224,
                        // height: 224,
                    },
                    face: {
                        enabled: true,
                        iris: { enabled: false },
                        mesh: { enabled: false },
                        emotion: { enabled: false },
                        detector: {
                            modelPath: "blazeface.json",
                            maxDetected: 2,
                            minConfidence: 0.25,
                        },
                        description: {
                            enabled: true,
                            modelPath: "faceres.json",
                        },
                    },
                    body: {
                        enabled: false,
                    },
                    hand: {
                        enabled: false,
                    },
                    gesture: {
                        enabled: false,
                    },
                    object: {
                        enabled: false,
                    },
                };

                // Define initHuman function
                async function initHuman() {
                    window._human = new Human.Human(HUMAN_CONFIG);
                    await window._human.load();
                    window._human.tf.enableProdMode();
                    // warmup the model
                    const tensor = window._human.tf.zeros([1, 224, 224, 3]);
                    await window._human.detect(tensor);
                    window._human.tf.dispose(tensor);
                    console.log("HB==Human model warmed up");
                }


                console.log("Call initHuman function");

                // Call initHuman function
                await initHuman();
                
                console.log("initHuman function finished");


                // Capture image from all the images on the WebView
                const images = document.querySelectorAll('img');
                images.forEach(async function(img) {
                    //const canvas = document.createElement('canvas');
                   // const context = canvas.getContext('2d');
                   // canvas.width = img.width;
                   // canvas.height = img.height;
                   // context.drawImage(img, 0, 0, img.width, img.height);
                   // const imageData = canvas.toDataURL('image/jpeg'); // Convert canvas to data URL
                    
                //    const image = new ImageData(new Uint8ClampedArray(img), img.width, img.height);

                    // Create HTMLImageElement and pass it to human.detect()
                   // const image = new Image();
                    //image.src = imageData;
                    img.crossOrigin = 'anonymous';
                   // img.onload = async function() {
                        const res = await window._human.detect(img); // Wait for the Promise to resolve
                        const gender = res.face[0].gender;
                        if (gender === "male") {
                            img.style.filter = "blur(5px)";
                            img.style.opacity = "0.5";
                        }
                        console.log(JSON.stringify(res));
                //    };
                });

            };
        })();
    """.trimIndent()

        view?.evaluateJavascript(initJs, null)

        view?.addJavascriptInterface(JavaScriptConsoleInterface(), "AndroidLogger")
    }

    inner class JavaScriptConsoleInterface {
        @JavascriptInterface
        fun log(message: String) {
            Log.d(TAG, "JavaScript Console: $message")
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
