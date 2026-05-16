package com.shilpakala.artisan

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.util.Patterns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var store: LocalStore
    private lateinit var root: LinearLayout
    private var capturedUri: Uri? = null
    private var selectedProductUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = LocalStore(this)
        showSplash()
    }

    override fun onBackPressed() {
        if (!::root.isInitialized) {
            super.onBackPressed()
            return
        }
        when (root.tag) {
            "home", "login", "splash" -> super.onBackPressed()
            "otp" -> renderLogin()
            "profile" -> if (store.isLoggedIn()) renderHome() else renderLogin()
            "editor", "gallery" -> renderHome()
            else -> super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::root.isInitialized && root.tag == "home") {
            renderHome()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CAMERA && resultCode == RESULT_OK) {
            selectedProductUri = capturedUri
            renderEditor()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_PERMISSION && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            toast("Camera permission is needed to photograph products.")
        }
    }

    private fun showSplash() {
        val screen = baseScreen("splash")
        screen.setBackgroundColor(BROWN)
        val logo = TextView(this).apply {
            text = "SK"
            textSize = 42f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(PAPER)
            background = rounded(GOLD, 120)
        }
        screen.addView(logo, LinearLayout.LayoutParams(dp(120), dp(120)).withMargins(0, dp(80), 0, dp(22)))
        screen.addView(label("SHILPA-KALA", 30f, PAPER, true, Gravity.CENTER))
        screen.addView(label("Digital portfolio assistant for Karnataka artisans", 15f, 0xDDEEE5D6.toInt(), false, Gravity.CENTER).withTop(dp(8)))
        setContentView(screen)
        screen.postDelayed({
            if (store.isLoggedIn()) renderHome() else renderLogin()
        }, 1200)
    }

    private fun renderLogin() {
        val content = scrollScreen("login", "Welcome to Shilpa-Kala", "Use mobile or email. Works offline after setup.")
        val email = input("Email address", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        val password = input("Password", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        val mobile = input("Mobile number", InputType.TYPE_CLASS_PHONE)

        content.addView(card().apply {
            addView(label("Artisan Sign In", 20f, BROWN, true))
            addView(email)
            addView(password)
            addView(button("Login", BROWN) {
                val mail = email.text.toString().trim()
                val pass = password.text.toString()
                when {
                    !Patterns.EMAIL_ADDRESS.matcher(mail).matches() -> toast("Enter a valid email address.")
                    pass.length < 6 -> toast("Password must be at least 6 characters.")
                    else -> {
                        store.saveSession(mail)
                        renderProfile(firstRun = !store.hasProfile())
                    }
                }
            })
            addView(label("Offline mode stores your profile on this phone. Online sync can be added later without breaking offline use.", 13f, MUTED, false).withTop(dp(10)))
        })

        content.addView(card().apply {
            addView(label("Quick Mobile Entry", 20f, BROWN, true))
            addView(mobile)
            addView(button("Continue with Mobile", GREEN) {
                val value = mobile.text.toString().trim()
                if (!value.matches(Regex("^[6-9]\\d{9}$"))) {
                    toast("Enter a valid 10 digit Indian mobile number.")
                } else {
                    renderOtpVerification("+91$value")
                }
            })
        })
        setWrapped(content)
    }

    private fun renderOtpVerification(mobile: String) {
        val content = scrollScreen("otp", "Verify Mobile", "We've sent a 6-digit code to $mobile for secure artisan access.")
        val otpInput = input("Enter 6-digit OTP", InputType.TYPE_CLASS_NUMBER)

        content.addView(card().apply {
            addView(label("OTP Verification", 20f, BROWN, true))
            addView(otpInput)
            addView(button("Verify & Login", BROWN) {
                val otp = otpInput.text.toString().trim()
                if (otp == "123456") { // Simulated OTP check
                    store.saveSession(mobile)
                    toast("Mobile verified successfully.")
                    renderProfile(firstRun = !store.hasProfile())
                } else {
                    toast("Invalid OTP. Try 123456 for testing.")
                }
            })
            addView(button("Resend OTP", Color.DKGRAY) {
                toast("OTP resent to $mobile")
            })
            addView(button("Change Number", Color.TRANSPARENT) {
                renderLogin()
            }.apply {
                setTextColor(BROWN)
            })
        })
        setWrapped(content)
    }

    private fun renderProfile(firstRun: Boolean) {
        val content = scrollScreen("profile", if (firstRun) "Set Up Profile" else "Edit Profile", "Your brand details appear on saved product images.")
        if (!firstRun) {
            content.addView(button("Back to Home", Color.DKGRAY) { renderHome() }, 2)
        }
        val current = store.profile()
        val name = input("Full name", InputType.TYPE_CLASS_TEXT, current.name)
        val craft = input("Craft type, e.g. Sandalwood carving", InputType.TYPE_CLASS_TEXT, current.craft)
        val location = input("Town or district", InputType.TYPE_CLASS_TEXT, current.location)
        val phone = input("Mobile number", InputType.TYPE_CLASS_PHONE, current.mobile)

        content.addView(card().apply {
            addView(name)
            addView(craft)
            addView(location)
            addView(phone)
            addView(button("Save Profile", BROWN) {
                if (name.text.isBlank() || craft.text.isBlank() || location.text.isBlank()) {
                    toast("Name, craft and location are required.")
                } else {
                    store.saveProfile(Profile(name.text.toString(), craft.text.toString(), location.text.toString(), phone.text.toString()))
                    toast("Profile saved.")
                    renderHome()
                }
            })
        })
        setWrapped(content)
    }

    private fun renderHome() {
        val content = scrollScreen("home", "Shilpa-Kala", "Photograph, brand, save and share artisan products.")
        root.tag = "home"
        val online = isOnline()
        content.addView(statusBand(if (online) "Online" else "Offline", if (online) "Ready for future sync. Local tools remain active." else "No internet. Camera, gallery and sharing from local files still work.", online))
        content.addView(card().apply {
            addView(label("Create Product Photo", 22f, BROWN, true))
            addView(label("Capture a product and add handmade Karnataka branding with price and craft details.", 14f, TEXT, false).withTop(dp(6)))
            addView(button("Open Camera", BROWN) { ensureCameraPermission() })
        })
        content.addView(card().apply {
            addView(label("Portfolio", 22f, BROWN, true))
            addView(label("${store.products().size} saved product image(s) available offline.", 14f, TEXT, false).withTop(dp(6)))
            addView(button("Open Gallery", GREEN) { renderGallery() })
        })
        content.addView(card().apply {
            addView(label("Account", 22f, BROWN, true))
            addView(label(store.profile().summary(), 14f, TEXT, false).withTop(dp(6)))
            addView(button("Edit Profile", GOLD_DARK) { renderProfile(firstRun = false) })
            addView(button("Logout", Color.DKGRAY) {
                store.logout()
                renderLogin()
            })
        })
        setWrapped(content)
    }

    private fun renderEditor() {
        val uri = selectedProductUri
        val content = scrollScreen("editor", "Brand Product Photo", "Add product details before saving to your offline portfolio.")
        content.addView(button("Cancel and Go Back", Color.DKGRAY) { renderHome() }, 2)
        val title = input("Product name", InputType.TYPE_CLASS_TEXT)
        val price = input("Price in rupees", InputType.TYPE_CLASS_NUMBER)
        val material = input("Material or craft note", InputType.TYPE_CLASS_TEXT, store.profile().craft)

        if (uri != null) {
            val preview = ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageURI(uri)
                background = rounded(0xFFECE2D4.toInt(), 12)
            }
            content.addView(preview, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(280)).withMargins(0, dp(12), 0, dp(14)))
        }

        content.addView(card().apply {
            addView(title)
            addView(price)
            addView(material)
            addView(button("Save Branded Image", BROWN) {
                val source = selectedProductUri
                if (source == null) {
                    toast("No product image selected.")
                    return@button
                }
                if (title.text.isBlank() || price.text.isBlank()) {
                    toast("Product name and price are required.")
                    return@button
                }
                val saved = processAndSaveImage(source, title.text.toString(), price.text.toString(), material.text.toString())
                if (saved != null) {
                    val product = Product(title.text.toString(), price.text.toString(), material.text.toString(), saved.toString(), System.currentTimeMillis(), store.syncState())
                    store.addProduct(product)
                    toast("Saved to Shilpa-Kala gallery.")
                    renderGallery()
                }
            })
        })
        setWrapped(content)
    }

    private fun renderGallery() {
        val products = store.products()
        val content = scrollScreen("gallery", "Gallery", "Saved branded images are available online and offline.")
        content.addView(button("Back to Home", Color.DKGRAY) { renderHome() })
        if (products.isEmpty()) {
            content.addView(card().apply {
                addView(label("No products yet", 22f, BROWN, true))
                addView(label("Use the camera to create your first branded product photo.", 14f, TEXT, false).withTop(dp(6)))
            })
        } else {
            products.forEach { product ->
                content.addView(card().apply {
                    val uri = Uri.parse(product.imageUri)
                    val image = ImageView(this@MainActivity).apply {
                        setImageURI(uri)
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        background = rounded(0xFFECE2D4.toInt(), 12)
                    }
                    addView(image, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(220)))
                    addView(label(product.name, 20f, BROWN, true).withTop(dp(10)))
                    addView(label("Rs ${product.price} | ${product.material}", 14f, TEXT, false).withTop(dp(4)))
                    addView(label(product.syncLabel, 12f, MUTED, false).withTop(dp(4)))
                    val actions = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                    }.withTop(dp(10)) as LinearLayout
                    actions.addView(button("Share", GREEN) { shareImage(uri, product.name) }.apply {
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    })
                    val space = View(this@MainActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(dp(12), 1)
                    }
                    actions.addView(space)
                    actions.addView(button("Delete", Color.RED) {
                        store.deleteProduct(product)
                        toast("Product removed.")
                        renderGallery()
                    }.apply {
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    })
                    addView(actions)
                })
            }
        }
        setWrapped(content)
    }

    private fun ensureCameraPermission() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQ_PERMISSION)
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        val uri = createImageUri("capture")
        if (uri == null) {
            toast("Could not prepare camera file.")
            return
        }
        capturedUri = uri
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        try {
            startActivityForResult(intent, REQ_CAMERA)
        } catch (e: Exception) {
            toast("No camera app found on this device.")
        }
    }

    private fun processAndSaveImage(source: Uri, name: String, price: String, material: String): Uri? {
        return try {
            val input = contentResolver.openInputStream(source) ?: return null
            val sourceBitmap = BitmapFactory.decodeStream(input) ?: return null
            val width = 1200
            val height = 1600
            val scaled = Bitmap.createScaledBitmap(sourceBitmap, width, height, true)
            val branded = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(branded)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            canvas.drawColor(PAPER)
            canvas.drawBitmap(scaled, 0f, 0f, paint)
            paint.color = 0xAA000000.toInt()
            canvas.drawRect(0f, height - 330f, width.toFloat(), height.toFloat(), paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.WHITE
            paint.textSize = 58f
            canvas.drawText(name.take(28), 60f, height - 230f, paint)

            paint.textSize = 40f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(material.take(38), 60f, height - 165f, paint)

            paint.color = GOLD
            canvas.drawRoundRect(RectF(width - 370f, height - 260f, width - 60f, height - 140f), 24f, 24f, paint)
            paint.color = BROWN
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 54f
            canvas.drawText("Rs $price", width - 330f, height - 182f, paint)

            paint.color = GREEN
            canvas.drawRoundRect(RectF(60f, 58f, 560f, 142f), 22f, 22f, paint)
            paint.color = Color.WHITE
            paint.textSize = 34f
            paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            canvas.drawText("Handmade in Karnataka", 86f, 112f, paint)

            val outUri = createImageUri("shilpa-kala")
            if (outUri != null) {
                contentResolver.openOutputStream(outUri)?.use { output ->
                    branded.compress(Bitmap.CompressFormat.JPEG, 92, output)
                }
            }
            outUri
        } catch (_: FileNotFoundException) {
            toast("Image file could not be opened.")
            null
        } catch (_: Exception) {
            toast("Image processing failed.")
            null
        }
    }

    private fun createImageUri(prefix: String): Uri? {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "${prefix}_$stamp.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ShilpaKala")
            }
        }
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    private fun shareImage(uri: Uri, title: String) {
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "$title - Handmade in Karnataka")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(share, "Share product photo"))
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo?.isConnectedOrConnecting == true
    }

    private fun scrollScreen(tag: String, title: String, subtitle: String): LinearLayout {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(28))
            this.tag = tag
        }
        content.addView(label(title, 30f, BROWN, true))
        content.addView(label(subtitle, 14f, MUTED, false).withTop(dp(6)))
        return content
    }

    private fun setWrapped(content: LinearLayout) {
        root = content
        val scroll = ScrollView(this).apply {
            setBackgroundColor(PAPER)
            addView(content)
        }
        setContentView(scroll)
    }

    private fun baseScreen(tag: String): LinearLayout {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
            this.tag = tag
        }
        return root
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        background = rounded(Color.WHITE, 12)
        elevation = dp(2).toFloat()
    }.withTop(dp(18)) as LinearLayout

    private fun statusBand(title: String, body: String, online: Boolean): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = rounded(if (online) 0xFFE6F2EA.toInt() else 0xFFFFEBCD.toInt(), 12)
        addView(label(title, 18f, if (online) GREEN else BROWN, true))
        addView(label(body, 13f, TEXT, false).withTop(dp(4)))
    }.withTop(dp(16)) as LinearLayout

    private fun input(hint: String, type: Int, value: String = ""): EditText = EditText(this).apply {
        this.hint = hint
        inputType = type
        setText(value)
        textSize = 16f
        setSingleLine(true)
        setPadding(dp(14), 0, dp(14), 0)
        background = roundedStroke(Color.WHITE, 10, 0xFFC9B99C.toInt())
    }.withTop(dp(12)) as EditText

    private fun button(text: String, color: Int, action: () -> Unit): Button = Button(this).apply {
        this.text = text
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE)
        background = rounded(color, 10)
        setOnClickListener { action() }
    }.withTop(dp(14)) as Button

    private fun label(text: String, size: Float, color: Int, bold: Boolean, gravityValue: Int = Gravity.START): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            gravity = gravityValue
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun View.withTop(top: Int): View {
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, top, 0, 0)
        layoutParams = params
        return this
    }

    private fun LinearLayout.LayoutParams.withMargins(left: Int, top: Int, right: Int, bottom: Int): LinearLayout.LayoutParams {
        setMargins(left, top, right, bottom)
        return this
    }

    private fun rounded(color: Int, radius: Int) = android.graphics.drawable.GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun roundedStroke(color: Int, radius: Int, stroke: Int) = android.graphics.drawable.GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), stroke)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    companion object {
        private const val REQ_CAMERA = 41
        private const val REQ_PERMISSION = 42
        private const val BROWN = 0xFF7A3E16.toInt()
        private const val GOLD = 0xFFD7A642.toInt()
        private const val GOLD_DARK = 0xFF9A6A1E.toInt()
        private const val GREEN = 0xFF2F6B4F.toInt()
        private const val PAPER = 0xFFFFF8ED.toInt()
        private const val TEXT = 0xFF2E241C.toInt()
        private const val MUTED = 0xFF6D6259.toInt()
    }
}

data class Profile(
    val name: String = "",
    val craft: String = "",
    val location: String = "",
    val mobile: String = ""
) {
    fun summary(): String {
        return if (name.isBlank()) {
            "No artisan profile yet."
        } else {
            "$name | $craft | $location"
        }
    }
}

data class Product(
    val name: String,
    val price: String,
    val material: String,
    val imageUri: String,
    val createdAt: Long,
    val syncLabel: String
)

class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("shilpa_kala_store", Context.MODE_PRIVATE)

    fun isLoggedIn(): Boolean = prefs.getBoolean("logged_in", false)

    fun saveSession(identifier: String) {
        prefs.edit().putBoolean("logged_in", true).putString("identifier", identifier).apply()
    }

    fun logout() {
        prefs.edit().putBoolean("logged_in", false).apply()
    }

    fun hasProfile(): Boolean = prefs.getString("profile_name", "").orEmpty().isNotBlank()

    fun profile(): Profile = Profile(
        prefs.getString("profile_name", "").orEmpty(),
        prefs.getString("profile_craft", "").orEmpty(),
        prefs.getString("profile_location", "").orEmpty(),
        prefs.getString("profile_mobile", "").orEmpty()
    )

    fun saveProfile(profile: Profile) {
        prefs.edit()
            .putString("profile_name", profile.name)
            .putString("profile_craft", profile.craft)
            .putString("profile_location", profile.location)
            .putString("profile_mobile", profile.mobile)
            .apply()
    }

    fun syncState(): String = "Saved locally. Pending optional online sync."

    fun products(): List<Product> {
        val array = JSONArray(prefs.getString("products", "[]"))
        val items = mutableListOf<Product>()
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            items.add(
                Product(
                    item.optString("name"),
                    item.optString("price"),
                    item.optString("material"),
                    item.optString("imageUri"),
                    item.optLong("createdAt"),
                    item.optString("syncLabel")
                )
            )
        }
        return items.sortedByDescending { it.createdAt }
    }

    fun addProduct(product: Product) {
        val array = JSONArray(prefs.getString("products", "[]"))
        array.put(JSONObject().apply {
            put("name", product.name)
            put("price", product.price)
            put("material", product.material)
            put("imageUri", product.imageUri)
            put("createdAt", product.createdAt)
            put("syncLabel", product.syncLabel)
        })
        prefs.edit().putString("products", array.toString()).apply()
    }

    fun deleteProduct(product: Product) {
        val array = JSONArray(prefs.getString("products", "[]"))
        val newArray = JSONArray()
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            if (item.optLong("createdAt") != product.createdAt) {
                newArray.put(item)
            }
        }
        prefs.edit().putString("products", newArray.toString()).apply()
    }
}
