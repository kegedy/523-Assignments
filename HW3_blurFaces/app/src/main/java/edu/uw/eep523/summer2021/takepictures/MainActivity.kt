
package edu.uw.eep523.summer2021.takepictures
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.set
import com.divyanshu.draw.widget.DrawView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import java.io.IOException
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {
    // https://stackoverflow.com/questions/55909804/duplicate-class-android-support-v4-app-inotificationsidechannel-found-in-modules
    // https://stackoverflow.com/questions/13119582/immutable-bitmap-crash-error

    private var isLandScape: Boolean = false
    // set #1
    private var imageUri: Uri? = null
    private var imageUriPrev: Uri? = null
    private var isModified: Boolean = false
    private var face: Face? = null
    // set #2
    private var imageUri2: Uri? = null
    private var imageUri2Prev: Uri? = null
    private var isModified2: Boolean = false
    private var face2: Face? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        isLandScape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        getRuntimePermissions()
        if (!allPermissionsGranted()) {
            getRuntimePermissions()
        }

        savedInstanceState?.let {
            imageUri = it.getParcelable(KEY_IMAGE_URI)
            imageUriPrev = it.getParcelable(KEY_IMAGE_URI_PREV)
            imageUri2 = it.getParcelable(KEY_IMAGE_URI2)
            imageUri2Prev = it.getParcelable(KEY_IMAGE_URI2_PREV)
            isModified = it.getBoolean("IS_MODIFIED_1", true)
            isModified2 = it.getBoolean("IS_MODIFIED_2", true)
        }

        // IMAGE PLACEHOLDERS
        placeHolders()

        // CLICK LISTENERS
        val btnCamera = findViewById<Button>(R.id.btn_camera)
        btnCamera.setOnClickListener { startCameraIntentForResult(btnCamera) }
        val btnGallery = findViewById<Button>(R.id.btn_gallery)
        btnGallery.setOnClickListener { startChooseImageIntentForResult(btnGallery) }
        val btnCamera2 = findViewById<Button>(R.id.btn_camera2)
        btnCamera2.setOnClickListener { startCameraIntentForResult(btnCamera2) }
        val btnGallery2 = findViewById<Button>(R.id.btn_gallery2)
        btnGallery2.setOnClickListener { startChooseImageIntentForResult(btnGallery2) }
        val swap = findViewById<Button>(R.id.swap)
        swap.setOnClickListener { faceSwapWrapper() } // update
        val blur = findViewById<Button>(R.id.blur)
        blur.setOnClickListener { bitmapBlurWrapper() } // update
        val clear = findViewById<Button>(R.id.clear)
        clear.setOnClickListener { imageRefresh() }
        val draw3 = findViewById<DrawView>(R.id.previewPane3)
        draw3.setOnTouchListener { _, event ->
            draw3.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
                isModified=true
            }
            true
        }
        val draw4 = findViewById<DrawView>(R.id.previewPane4)
        draw4.setOnTouchListener { _, event ->
            draw4.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
                isModified2=true
            }
            true
        }
    }

    private fun getRequiredPermissions(): Array<String?> {
        return try {
            val info = this.packageManager
                .getPackageInfo(this.packageName, PackageManager.GET_PERMISSIONS)
            val ps = info.requestedPermissions
            if (ps != null && ps.isNotEmpty()) {
                ps
            } else {
                arrayOfNulls(0)
            }
        } catch (e: Exception) {
            arrayOfNulls(0)
        }
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in getRequiredPermissions()) {
            permission?.let {
                if (!isPermissionGranted(this, it)) {
                    return false
                }
            }
        }
        return true
    }

    private fun getRuntimePermissions() {
        val allNeededPermissions = ArrayList<String>()
        for (permission in getRequiredPermissions()) {
            permission?.let {
                if (!isPermissionGranted(this, it)) {
                    allNeededPermissions.add(permission)
                }
            }
        }

        if (allNeededPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this, allNeededPermissions.toTypedArray(), PERMISSION_REQUESTS)
        }
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        return false
    }

    private fun placeHolders() {
        val pane = findViewById<ImageView>(R.id.previewPane)
        pane.setBackgroundResource(R.drawable.ic_launcher_background)
        pane.setImageResource(R.drawable.ic_launcher_foreground)
        val pane2 = findViewById<ImageView>(R.id.previewPane2)
        pane2.setBackgroundResource(R.drawable.ic_launcher_background)
        pane2.setImageResource(R.drawable.ic_launcher_foreground)
        val draw3 = findViewById<DrawView>(R.id.previewPane3)
        draw3.setBackgroundResource(R.drawable.ic_launcher_background)
        val draw4 = findViewById<DrawView>(R.id.previewPane4)
        draw4.setBackgroundResource(R.drawable.ic_launcher_background)
        imageRefresh()
    }

    private fun imageRefresh() {
        val pane = findViewById<ImageView>(R.id.previewPane)
        val pane2 = findViewById<ImageView>(R.id.previewPane2)
        val draw3 = findViewById<DrawView>(R.id.previewPane3)
        val draw4 = findViewById<DrawView>(R.id.previewPane4)

        if (imageUri != null) {
            tryReloadImage(pane,imageUri)
        }
        if (imageUri2 != null) {
            tryReloadImage(pane2,imageUri2)
        }
        if (isModified) {
            draw3?.clearCanvas()
            if (imageUriPrev != null) tryReloadDraw(draw3, imageUriPrev)
            isModified = false
        }
        if (isModified2) {
            draw4?.clearCanvas()
            if (imageUri2Prev != null) tryReloadDraw(draw4, imageUri2Prev)
            isModified2 = false
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        with(outState) {
            putParcelable(KEY_IMAGE_URI, imageUri)
            putParcelable(KEY_IMAGE_URI_PREV, imageUriPrev)
            putParcelable(KEY_IMAGE_URI2, imageUri2)
            putParcelable(KEY_IMAGE_URI2_PREV, imageUri2Prev)
            putBoolean("IS_MODIFIED_1", true)
            putBoolean("IS_MODIFIED_2", true)
        }
    }

    private fun startCameraIntentForResult(view:View) {
        // Clean up last time's image
        // imageUri = null
        // imageUri2 = null
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        if (view.id == R.id.btn_camera) { // SET IMAGE #1
            takePictureIntent.resolveActivity(packageManager)?.let {
                val values = ContentValues()
                values.put(MediaStore.Images.Media.TITLE, "New Picture")
                values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
                imageUri =
                    contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                imageUriPrev = imageUri
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        } else { // SET IMAGE #2
            takePictureIntent.resolveActivity(packageManager)?.let {
                val values = ContentValues()
                values.put(MediaStore.Images.Media.TITLE, "New Picture")
                values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
                imageUri2 =
                    contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                imageUri2Prev = imageUri2
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri2)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE2)
            }
        }
    }

    private fun startChooseImageIntentForResult(view:View) {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        if (view.id == R.id.btn_gallery) { // SET IMAGE #1
            startActivityForResult(
                Intent.createChooser(intent, "Select Picture"),
                REQUEST_CHOOSE_IMAGE
            )
        } else { // SET IMAGE #2
            startActivityForResult(
                Intent.createChooser(intent, "Select Picture"),
                REQUEST_CHOOSE_IMAGE2
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val pane = findViewById<ImageView>(R.id.previewPane)
        val pane2 = findViewById<ImageView>(R.id.previewPane2)
        val draw3 = findViewById<DrawView>(R.id.previewPane3)
        val draw4 = findViewById<DrawView>(R.id.previewPane4)

        super.onActivityResult(requestCode, resultCode, data)
        Log.i("Kevin","requestCode: $requestCode")
        Log.i("Kevin","resultCode: $resultCode")
        if (resultCode != Activity.RESULT_OK) {
            return
        }

        when (requestCode) {
            REQUEST_IMAGE_CAPTURE -> {
                tryReloadImage(pane,imageUri)
                tryReloadDraw(draw3,imageUri)
            }
            REQUEST_IMAGE_CAPTURE2 -> {
                tryReloadImage(pane2,imageUri2)
                tryReloadDraw(draw4,imageUri2)
            }
            REQUEST_CHOOSE_IMAGE -> {
                imageUri = data!!.data
                imageUriPrev = imageUri
                tryReloadImage(pane,imageUri)
                tryReloadDraw(draw3,imageUri)
            }
            REQUEST_CHOOSE_IMAGE2 -> {
                imageUri2 = data!!.data
                imageUri2Prev = imageUri2
                tryReloadImage(pane2,imageUri2)
                tryReloadDraw(draw4,imageUri2)
            }
//            REQUEST_BLUR_FACE -> {
//                // something
//            }
//            REQUEST_SWAP_FACE -> {
//                // something
//            }
        }

    }

    private fun detectInImage(passedImageUri: Uri?): Bitmap? {
        var imageBitmap: Bitmap? = null
        try {
            if (passedImageUri == null) {
                return imageBitmap
            }
            imageBitmap = if (Build.VERSION.SDK_INT < 29) {
                MediaStore.Images.Media.getBitmap(contentResolver, passedImageUri)
            } else {
                val source = ImageDecoder.createSource(contentResolver, passedImageUri)
                ImageDecoder.decodeBitmap(source)
            }
        } catch (e: IOException) { }
        return imageBitmap
    }

    private fun tryReloadImage(view: ImageView, passedImageUri: Uri?) {
        view.setBackgroundColor(0)
        val imageBitmap = detectInImage(passedImageUri)
        try {
            view?.setImageBitmap(scaleBitmapDown(imageBitmap!!,600))
        } catch (e: IOException) { }
    }

    private fun tryReloadDraw(draw: DrawView, passedImageUri: Uri?) {
        draw.setBackgroundColor(0)
        val imageBitmap = detectInImage(passedImageUri)
        try {
            draw.background= BitmapDrawable(resources, scaleBitmapDown(imageBitmap!!,600))
        } catch (e: IOException) { }
    }

    private fun bitmapMutable(passedImageUri: Uri): Bitmap? {
        val imageBitmap = detectInImage(passedImageUri)
        var bitmapMutable: Bitmap? = null
        bitmapMutable = imageBitmap?.let{ Bitmap.createBitmap(it, 0, 0, it.width, it.height) }
        bitmapMutable = bitmapMutable?.copy(Bitmap.Config.ARGB_8888, true)
        return bitmapMutable
    }

    // source: https://firebase.google.com/docs/ml/android/recognize-landmarks
    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        var resizedWidth = maxDimension
        var resizedHeight = maxDimension
        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension
            resizedWidth =
                (resizedHeight * originalWidth.toFloat() / originalHeight.toFloat()).toInt()
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension
            resizedHeight =
                (resizedWidth * originalHeight.toFloat() / originalWidth.toFloat()).toInt()
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension
            resizedWidth = maxDimension
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false)
    }

    private fun bitmapSwap(passedFace: Face, passedFace2: Face) {
        Log.i("Kevin","SUCCESSSSSS!!!!!")
        var imageBitmap: Bitmap? = imageUri?.let { bitmapMutable(it)!! }
        var imageBitmap2: Bitmap? = imageUri2?.let { bitmapMutable(it)!! }
        val draw3 = findViewById<DrawView>(R.id.previewPane3)
        val draw4 = findViewById<DrawView>(R.id.previewPane4)

        // passedFace
        val passedFaceX = passedFace.boundingBox.centerX()
        val passedFaceY = passedFace.boundingBox.centerY()
        var passedFaceWidth = passedFace.boundingBox.width()
        var passedFaceHeight = passedFace.boundingBox.height()
        if (passedFaceWidth > passedFaceHeight) passedFaceHeight = passedFaceWidth
        else passedFaceWidth = passedFaceHeight

        // passedFace2
        val passedFace2X = passedFace2.boundingBox.centerX()
        val passedFace2Y = passedFace2.boundingBox.centerY()
        var passedFace2Width = passedFace2.boundingBox.width()
        var passedFace2Height = passedFace2.boundingBox.height()
        if (passedFace2Width > passedFace2Height) passedFace2Height = passedFace2Width
        else passedFace2Width = passedFace2Height

        // create bitmaps of faces
        var tmpBitmap = imageBitmap?.let {
            Bitmap.createBitmap(it,
                passedFaceX - (passedFaceWidth /2),
                passedFaceY - (passedFaceHeight /2),
                passedFaceWidth, passedFaceHeight)}
        var tmpBitmap2 = imageBitmap2?.let {
            Bitmap.createBitmap(it,
                passedFace2X - (passedFace2Width /2),
                passedFace2Y - (passedFace2Height /2),
                passedFace2Width, passedFace2Height)}

        // face must be scaled to fit other image
        tmpBitmap = scaleBitmapDown(tmpBitmap!!, passedFace2Width)
        tmpBitmap2 = scaleBitmapDown(tmpBitmap2!!, passedFaceWidth)

        // replace imageBitmap pixels
        var size: Int = tmpBitmap2!!.width * tmpBitmap2.height
        var arr = IntArray(size){0}
        tmpBitmap2.getPixels(arr,
            0,
            passedFaceWidth,
            0,
            0,
            passedFaceWidth,
            passedFaceHeight
        )
        imageBitmap!!.setPixels(arr,
            0,
            passedFaceWidth,
            passedFaceX - (passedFaceWidth /2),
            passedFaceY - (passedFaceHeight /2),
            passedFaceWidth,
            passedFaceHeight
        )
        isModified = true

        // replace imageBitmap2 pixels
        size = tmpBitmap!!.width * tmpBitmap!!.height
        arr = IntArray(size){0}
        tmpBitmap.getPixels(arr,
            0,
            passedFace2Width,
            0,
            0,
            passedFace2Width,
            passedFace2Height
        )
        imageBitmap2!!.setPixels(arr,
            0,
            passedFace2Width,
            passedFace2X - (passedFace2Width /2),
            passedFace2Y - (passedFace2Height /2),
            passedFace2Width,
            passedFace2Height
        )
        isModified2 = true

        // Update drawView
        draw3.background = BitmapDrawable(resources, imageBitmap)
        draw4.background = BitmapDrawable(resources, imageBitmap2)

        // recycle
//        imageBitmap.recycle()
//        imageBitmap2.recycle()
    }

    private fun faceSwapWrapper() {
        Log.i("Kevin","Begin faceSwapWrapper")
        if (imageUriPrev != null && imageUri2Prev != null) {
            faceDetection()
        }
    }

    private fun faceDetection() {
        val inputImage = imageUriPrev?.let{ InputImage.fromBitmap(detectInImage(it), 0) }
        val inputImage2 = imageUri2Prev?.let{ InputImage.fromBitmap(detectInImage(it), 0) }

        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE).build()
        val detector = FaceDetection.getClient(highAccuracyOpts)

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                Log.i("Kevin","inputImage detector: success")
                if (faces.isNotEmpty()) face = faces[0]
                if (face != null && face2 != null) bitmapSwap(face!!, face2!!)
            }.addOnFailureListener { e -> // Task failed with an exception
                e.printStackTrace()
                Log.i("Kevin","inputImage detector: failure")
            }
        detector.process(inputImage2)
            .addOnSuccessListener { faces ->
                Log.i("Kevin","inputImage2 detector: success")
                if (faces.isNotEmpty()) face2 = faces[0]
                if (face != null && face2 != null) bitmapSwap(face!!, face2!!)
            }.addOnFailureListener { e -> // Task failed with an exception
                e.printStackTrace()
                Log.i("Kevin","inputImage2 detector: failure")
            }
    }

    private fun bitmapBlurWrapper() {
        Log.i("Kevin","Begin bitmapBlurWrapper")
        val draw3 = findViewById<DrawView>(R.id.previewPane3)
        val draw4 = findViewById<DrawView>(R.id.previewPane4)
        // java.lang.IllegalArgumentException: Hardware bitmaps are always immutable
        val bitmap: Bitmap? = imageUri?.let{ bitmapMutable(it)!! }
        val bitmap2: Bitmap? = imageUri2?.let{ bitmapMutable(it)!! }

        if (imageUri != null) {
            val drawBitmap = bitmap?.let {bitmapBlur(it, 1F,100)}
            draw3?.background = BitmapDrawable(resources, drawBitmap)
            isModified = true
        }
        if (imageUri2 != null) {
            val drawBitmap2 = bitmap2?.let {bitmapBlur(it, 1F,100)}
            draw4?.background = BitmapDrawable(resources, drawBitmap2)
            isModified2 = true
        }
        // recycle
//        bitmap?.recycle()
//        bitmap2?.recycle()
    }

    private fun bitmapBlur(sentBitmap: Bitmap, scale: Float, radius: Int): Bitmap? {
        var sentBitmap = sentBitmap
        val width = (sentBitmap.width * scale).roundToInt()
        val height = (sentBitmap.height * scale).roundToInt()
        sentBitmap = Bitmap.createScaledBitmap(sentBitmap, width, height, false)
        val bitmap = sentBitmap.copy(sentBitmap.config, true)
        if (radius < 1) {
            return null
        }
        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        Log.e("pix", w.toString() + " " + h + " " + pix.size)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)
        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1
        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        val vmin = IntArray(w.coerceAtLeast(h))
        var divsum = div + 1 shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        i = 0
        while (i < 256 * divsum) {
            dv[i] = i / divsum
            i++
        }
        yi = 0
        var yw: Int = yi
        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int
        y = 0
        while (y < h) {
            bsum = 0
            gsum = bsum
            rsum = gsum
            boutsum = rsum
            goutsum = boutsum
            routsum = goutsum
            binsum = routsum
            ginsum = binsum
            rinsum = ginsum
            i = -radius
            while (i <= radius) {
                p = pix[yi + wm.coerceAtMost(i.coerceAtLeast(0))]
                sir = stack[i + radius]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff
                rbs = r1 - abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                i++
            }
            stackpointer = radius
            x = 0
            while (x < w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (y == 0) {
                    vmin[x] = (x + radius + 1).coerceAtMost(wm)
                }
                p = pix[yw + vmin[x]]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi++
                x++
            }
            yw += w
            y++
        }
        x = 0
        while (x < w) {
            bsum = 0
            gsum = bsum
            rsum = gsum
            boutsum = rsum
            goutsum = boutsum
            routsum = goutsum
            binsum = routsum
            ginsum = binsum
            rinsum = ginsum
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = 0.coerceAtLeast(yp) + x
                sir = stack[i + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - abs(i)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (i < hm) {
                    yp += w
                }
                i++
            }
            yi = x
            stackpointer = radius
            y = 0
            while (y < h) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = -0x1000000 and pix[yi] or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (x == 0) {
                    vmin[y] = (y + r1).coerceAtMost(hm) * w
                }
                p = x + vmin[y]
                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi += w
                y++
            }
            x++
        }
        Log.e("pix", w.toString() + " " + h + " " + pix.size)
        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }

    companion object {
        private const val KEY_IMAGE_URI = "edu.uw.eep523.takepicture.KEY_IMAGE_URI_1"
        private const val KEY_IMAGE_URI_PREV = "edu.uw.eep523.takepicture.KEY_IMAGE_URI_1_PREV"
        private const val KEY_IMAGE_URI2 = "edu.uw.eep523.takepicture.KEY_IMAGE_URI_2"
        private const val KEY_IMAGE_URI2_PREV = "edu.uw.eep523.takepicture.KEY_IMAGE_URI_2_PREV"
        private const val IS_MODIFIED_1 = false
        private const val IS_MODIFIED_2 = false
        private const val REQUEST_IMAGE_CAPTURE = 1001
        private const val REQUEST_CHOOSE_IMAGE = 1002
        private const val REQUEST_IMAGE_CAPTURE2 = 1003
        private const val REQUEST_CHOOSE_IMAGE2 = 1004
        private const val REQUEST_SWAP_FACE = 1005
        private const val REQUEST_BLUR_FACE = 1006
        private const val PERMISSION_REQUESTS = 1
    }
}
