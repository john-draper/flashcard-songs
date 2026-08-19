package com.johndraper.flashcardsongs

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebViewAssetLoader
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Offline wrapper around the flashcard-songs site bundled in assets/web/.
 * The site is served from https://appassets.androidplatform.net/assets/ via
 * WebViewAssetLoader so localStorage, relative URLs and blob downloads behave
 * like on a real https origin. No INTERNET permission: the app is fully offline.
 */
class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private var pendingFileChooser: ValueCallback<Array<Uri>>? = null

    private val assetLoader by lazy {
        WebViewAssetLoader.Builder()
            .setDomain(ASSET_DOMAIN)
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
    }

    private val fileChooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val callback = pendingFileChooser
            pendingFileChooser = null
            callback?.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            )
        }

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (webView.canGoBack()) webView.goBack() else finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)
        // The site styles its own safe-area padding for iOS only; on Android keep the
        // WebView inside the system bars ourselves (matters on API 35 edge-to-edge).
        ViewCompat.setOnApplyWindowInsetsListener(webView) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        configureWebView()
        onBackPressedDispatcher.addCallback(this, backCallback)
        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            webView.loadUrl(startUrl)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true // deck auto-save (studied cards, position, filter) uses localStorage
        }
        webView.setBackgroundColor(0xFFF5FA.toInt())
        webView.webViewClient = object : WebViewClient() {

            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url
                if (url.host == ASSET_DOMAIN) {
                    // Hub links point at "songs/<slug>/" but directory URLs don't resolve
                    // to index.html when serving from assets.
                    if (url.path?.endsWith("/") == true) {
                        view.loadUrl(url.toString() + "index.html")
                        return true
                    }
                    return false
                }
                // Off-origin links (the "View source on GitHub" link) go to the browser.
                return try {
                    startActivity(Intent(Intent.ACTION_VIEW, url))
                    true
                } catch (e: ActivityNotFoundException) {
                    true
                }
            }

            override fun onPageFinished(view: WebView, url: String?) {
                // "Save Progress" downloads a JSON blob via <a download>, which a WebView
                // cannot do natively. Capture blob payloads so the DownloadListener can
                // persist them through MediaStore instead.
                view.evaluateJavascript(BLOB_CAPTURE_SHIM, null)
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            // Default alert/confirm handling (the deck's Reset confirm) comes for free here.

            override fun onShowFileChooser(
                view: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean {
                pendingFileChooser?.onReceiveValue(null)
                pendingFileChooser = callback
                // "*/*" rather than the page's application/json accept, which some
                // pickers filter into an empty document list.
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
                return try {
                    fileChooserLauncher.launch(intent)
                    true
                } catch (e: ActivityNotFoundException) {
                    pendingFileChooser = null
                    Toast.makeText(this@MainActivity, "No file picker available", Toast.LENGTH_SHORT).show()
                    false
                }
            }
        }
        webView.setDownloadListener { url, _, _, _, _ -> handleProgressExport(url) }
    }

    private fun handleProgressExport(url: String) {
        if (!url.startsWith("blob:")) return
        webView.evaluateJavascript(
            "(window.__blobPayloads || {})[" + jsStringLiteral(url) + "] || null"
        ) { result ->
            when (result) {
                null, "null", "undefined" ->
                    Toast.makeText(this, "Save wasn't ready — tap Save again", Toast.LENGTH_SHORT).show()
                else -> writeProgressFile(result)
            }
        }
    }

    private fun writeProgressFile(jsonPayload: String) {
        val name = "flashcards-progress_" +
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) + ".json"
        val savedTo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, "application/json")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("MediaStore rejected the file")
                contentResolver.openOutputStream(uri)?.use { it.write(jsonPayload.toByteArray()) }
                    ?: throw IllegalStateException("Could not open output stream")
                "Downloads/$name"
            } else {
                val dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: filesDir
                val file = File(dir, name)
                file.writeText(jsonPayload)
                file.absolutePath
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Could not save progress: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(this, "💾 Saved to $savedTo", Toast.LENGTH_LONG).show()
    }

    private fun jsStringLiteral(s: String) =
        "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'"

    companion object {
        private const val ASSET_DOMAIN = "appassets.androidplatform.net"
        private const val START_PATH = "/assets/web/index.html"
        private val startUrl get() = "https://$ASSET_DOMAIN$START_PATH"

        private val BLOB_CAPTURE_SHIM = """
            (function(){
              if (window.__blobCaptureInstalled) return;
              window.__blobCaptureInstalled = true;
              var origCreate = URL.createObjectURL.bind(URL);
              URL.createObjectURL = function(blob){
                var u = origCreate(blob);
                try {
                  if (blob && /json/.test(blob.type || '')) {
                    blob.text().then(function(t){
                      try {
                        window.__blobPayloads = window.__blobPayloads || {};
                        window.__blobPayloads[u] = JSON.parse(t);
                      } catch (e) {}
                    });
                  }
                } catch (e) {}
                return u;
              };
            })();
        """.trimIndent()
    }
}
