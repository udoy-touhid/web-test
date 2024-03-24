package com.cature.webtest

import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {

    companion object {
        lateinit var webView: WebView

        val initCs = """
        (async function() {
            'use strict';
            
         var parent = document.getElementsByTagName('head').item(0);
         var script = document.createElement('script');
         script.type = 'module';
         script.src = "https://appassets.androidplatform.net/assets/testing.js"
         parent.appendChild(script);

        })();

        """.trimIndent()

        fun csInject() {

//            Log.e("csInject,", "oncsInject")
//            webView.evaluateJavascript("javascript:${MainActivity.initCs}", null)

        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById<WebView>(R.id.webView)

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", AssetsPathHandler(this))
            .build()

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.webViewClient = WVClient(assetLoader)
        webView.getSettings().setDomStorageEnabled(true)
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        webView.settings.allowFileAccessFromFileURLs = true
        webView.settings.allowUniversalAccessFromFileURLs = true


//        webView.loadUrl("https://news.google.com")
//        webView.loadUrl("https://publicholidays.com.bd/")
//        webView.loadUrl("https://www.shutterstock.com/explore/apag-stock-assets-0111/")
        webView.loadUrl("https://twitter.com/elonmusk")


        //js()

        webView.evaluateJavascript(
            """
                    (function() {
                        var script = document.createElement('script');
                        script.src = 'https://appassets.androidplatform.net/assets/tfjs.min.js';
                        script.type = 'text/javascript';
                        document.head.appendChild(script);
                    })();
                    """.trimIndent(),
            null
        )
        webView.evaluateJavascript(
            """
                    (function() {
                        var script = document.createElement('script');
                        script.src = 'https://appassets.androidplatform.net/assets/models/mobilenet_v2_mid/model.min.js';
                        document.head.appendChild(script);
                    })();
                    """.trimIndent(),
            null
        )
        webView.evaluateJavascript(
            """
                    (function() {
                        var script = document.createElement('script');
                        script.src = 'https://appassets.androidplatform.net/assets/models/mobilenet_v2_mid/group1-shard1of2.min.js';
                        document.head.appendChild(script);
                    })();
                    """.trimIndent(),
            null
        )
        webView.evaluateJavascript(
            """
                    (function() {
                        var script = document.createElement('script');
                        script.src = 'https://appassets.androidplatform.net/assets/models/mobilenet_v2_mid/group1-shard2of2.min.js';
                        document.head.appendChild(script);
                    })();
                    """.trimIndent(),
            null
        )
        webView.evaluateJavascript(
            """
                    (function() {
                        var script = document.createElement('script');
                        script.src = 'https://appassets.androidplatform.net/assets/models/mobilenet_v2_mid/model.min.js';
                        document.head.appendChild(script);
                    })();
                    """.trimIndent(),
            null
        )

        webView.evaluateJavascript(
            """
                    (function() {
                        var script = document.createElement('script');
                        script.src = 'https://appassets.androidplatform.net/assets/nsfwjs.min.js';
                        document.head.appendChild(script);
                    })();
                    """.trimIndent(),
            null
        )

        // Execute NSFWJS code after the library is loaded
        webView.evaluateJavascript(
            """
                    (function() {
                        const img = document.getElementById("logo");
                        

                        nsfwjs.load().then((model) => {
                            console.log("Predictions MobileNetV2Mid");
                    
                            // Classify the image.
                            model.classify(img).then((predictions) => {
                                console.log("Predictions", predictions);
                              });
                            });
                    })();
                    """.trimIndent(),
            null
        )

//        js()
    }

    private fun js() {


        Log.e("init", initCs)
        webView.evaluateJavascript("$initCs", null)

//        webView.setOnLongClickListener {

//            // Inject JavaScript with coordinates to get the parent div of the clicked image
//            webView.post {
//
//                val result = webView.hitTestResult
//
//
//                // if (result.type == WebView.HitTestResult.IMAGE_TYPE || result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
//
//                // The long-clicked element is an image
//                var imageUrl = result.extra ?: ""
//                var imageUrlHref = result.extra ?: ""
//
//
//                val href: Message = webView.getHandler().obtainMessage()
//                webView.requestFocusNodeHref(href)
//                val data: Bundle = href.data
//                val iurl = data.getString("url")
//                if (iurl != null && !iurl.isEmpty()) {
//                    imageUrlHref = iurl
//                    //Handle url as you want..
//                }
//                imageUrl = imageUrl.replaceFirst("https://", "")
//                imageUrl = imageUrl.replaceFirst("http://", "")
//                val js = """
//                    (function() {
//                           var blurFun = function (image){
//                           console.log('blur:' + image);
//                           console.log('blur:' + '$imageUrl');
//                           console.log('prev blur filter:' + image.style.cssText);
//                           if(image.style.filter === "" || image.style.cssText === "filter: blur(10px) !important;"){
//                               var blurAmt = "blur(" + 0 + "px) "
//                               image.style.cssText += ';filter: ' + blurAmt + ' !important;'
//                           }
//                           else {
//                                var blurAmt = "blur(" + 10 + "px) "
//                                image.style.cssText += ';filter: ' + blurAmt + ' !important;'
//                                 console.log('blur applied:' + '10px');
//                           }
//
//                       };
//                       var images = document.querySelectorAll("img[src*='$imageUrl']");
//                       images.forEach(blurFun);
//                       if(images.length <1)
//                          document.querySelectorAll("img[srcset*='$imageUrl']").forEach(blurFun);
////                     document.querySelectorAll("a[href='$imageUrlHref']").forEach(blurFun);
//
//                      })()
//                  """
//
////                Log.e("links", "imageUrl: $imageUrl....imageUrlHref:$imageUrlHref")
////                Log.e("eve", js)
//                webView.evaluateJavascript("javascript:$js", null)
    }
//            true // Consume the long click event
//        }
//    }
}


class WVClient(val assetLoader: WebViewAssetLoader) : WebViewClient() {

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
//        MainActivity.csInject()

    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        MainActivity.csInject()
    }

    @WorkerThread
    override fun shouldInterceptRequest(
        view: WebView?, request: WebResourceRequest?
    ): WebResourceResponse? {

//        Log.e("url",request!!.getUrl().toString());
     //   if (request!!.getUrl().toString().contains("appassets.androidplatform.net/assets/"))
            return assetLoader.shouldInterceptRequest(request!!.getUrl());

        return runBlocking {

            withContext(Dispatchers.Main) {
                // MainActivity.csInject()
            }
            super.shouldInterceptRequest(view, request)
        }
    }
}