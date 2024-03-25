package com.cature.webtest

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)

        val assetLoader = CustomWebViewAssetLoader.Builder()
            .addPathHandler("/assets/", AssetsPathHandler(this))
            .build()

        webView.apply {
            settings.javaScriptEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.allowFileAccessFromFileURLs = true
            settings.allowUniversalAccessFromFileURLs = true
            webViewClient = WVClient(assetLoader)

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

        webView.loadUrl("https://www.mymodefa.com/collections/mens-islamic-thobes/products/mens-casual-full-length-islamic-thobe-emirati-506-white")
    }
}

class WVClient(private val assetLoader: CustomWebViewAssetLoader) : WebViewClient() {

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        injectJavaScript(view)
    }

    override fun shouldInterceptRequest(
        view: WebView?, request: WebResourceRequest?
    ): WebResourceResponse? {
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

                // Call initHuman function
                await initHuman();

                // Capture image from the WebView
                const canvas = document.createElement('canvas');
                const context = canvas.getContext('2d');
                const img = document.querySelector('img.photoswipe__image');
                canvas.width = img.width;
                canvas.height = img.height;
                context.drawImage(img, 0, 0, img.width, img.height);
                const imageData = canvas.toDataURL('image/jpeg'); // Convert canvas to data URL
                console.log(imageData)

                // Create HTMLImageElement and pass it to human.detect()
                const image = new Image();
                image.src = imageData;
                image.onload = async function() {
                    const res = await window._human.detect(image); // Wait for the Promise to resolve
                    const gender = res.face[0].gender; // Corrected this line
                    if (gender === "male") {
                            img.style.filter = "blur(5px)";
                            img.style.opacity = "0.5";
                    }
                    console.log(JSON.stringify(res));
                };
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