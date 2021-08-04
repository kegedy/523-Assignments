
package edu.uw.eep523.summer2021.takepictures
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.ArrayList



class MainActivity : AppCompatActivity() {

    private var isLandScape: Boolean = false
    private var imageUri: Uri? = null
    private var imageUri2: Uri? = null

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
            imageUri2 = it.getParcelable(KEY_IMAGE_URI2)
        }

        // IMAGE PLACEHOLDERS
        refresh()

        // CLICK LISTENERS
        val btnCamera = findViewById<Button>(R.id.btn_camera)
        btnCamera.setOnClickListener { startCameraIntentForResult(btnCamera) }
        val btnGallery = findViewById<Button>(R.id.btn_gallery)
        btnGallery.setOnClickListener { startChooseImageIntentForResult(btnGallery) }
        val btnCamera2 = findViewById<Button>(R.id.btn_camera2)
        btnCamera2.setOnClickListener { startCameraIntentForResult(btnCamera2) }
        val btnGallery2 = findViewById<Button>(R.id.btn_gallery2)
        btnGallery2.setOnClickListener { startChooseImageIntentForResult(btnGallery2) }
        val startOver = findViewById<Button>(R.id.startOver)
        startOver.setOnClickListener { refresh() }
        val cropToSquare = findViewById<Button>(R.id.modify)
        cropToSquare.setOnClickListener { modifyImages() }
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

    private fun refreshUserImages() {
        val pane = findViewById<ImageView>(R.id.previewPane)
        pane.setBackgroundResource(R.drawable.ic_launcher_background)
        pane.setImageResource(R.drawable.ic_launcher_foreground)
        val pane2 = findViewById<ImageView>(R.id.previewPane2)
        pane2.setBackgroundResource(R.drawable.ic_launcher_background)
        pane2.setImageResource(R.drawable.ic_launcher_foreground)
    }

    private fun refreshmodifyImages() {
        val pane3 = findViewById<ImageView>(R.id.previewPane3)
        pane3.setBackgroundResource(R.drawable.ic_launcher_background)
        pane3.setImageResource(R.drawable.ic_launcher_foreground)
        val pane4 = findViewById<ImageView>(R.id.previewPane4)
        pane4.setBackgroundResource(R.drawable.ic_launcher_background)
        pane4.setImageResource(R.drawable.ic_launcher_foreground)
    }

    private fun refresh() {
        refreshUserImages()
        refreshmodifyImages()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        with(outState) {
            putParcelable(KEY_IMAGE_URI, imageUri)
            putParcelable(KEY_IMAGE_URI2, imageUri2)
        }
    }

    private fun startCameraIntentForResult(view:View) {
        // Clean up last time's image
//        refreshUserImages()
        imageUri = null
        imageUri2 = null

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        if (view.id == R.id.btn_camera) { // SET IMAGE #1
            takePictureIntent.resolveActivity(packageManager)?.let {
                val values = ContentValues()
                values.put(MediaStore.Images.Media.TITLE, "New Picture")
                values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
                imageUri =
                    contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
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
        val previewPane = findViewById<ImageView>(R.id.previewPane)
        val previewPane2 = findViewById<ImageView>(R.id.previewPane2)

        super.onActivityResult(requestCode, resultCode, data)
        Log.i("Kevin",requestCode.toString())
        Log.i("Kevin",resultCode.toString())
        if (resultCode != Activity.RESULT_OK) {
            refresh()
            return
        }
        when (requestCode) {
            REQUEST_IMAGE_CAPTURE -> {
                previewPane?.setBackgroundColor(0)
                tryReloadImage(findViewById(R.id.previewPane),imageUri)
            }
            REQUEST_IMAGE_CAPTURE2 -> {
                previewPane2?.setBackgroundColor(0)
                tryReloadImage(findViewById(R.id.previewPane2),imageUri2)
            }
            REQUEST_CHOOSE_IMAGE -> {
                previewPane?.setBackgroundColor(0)
                imageUri = data!!.data
                tryReloadImage(findViewById(R.id.previewPane),imageUri)
            }
            REQUEST_CHOOSE_IMAGE2 -> {
                previewPane2?.setBackgroundColor(0)
                imageUri2 = data!!.data
                tryReloadImage(findViewById(R.id.previewPane2),imageUri2)
            }
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
                val source = ImageDecoder.createSource(contentResolver, passedImageUri!!)
                ImageDecoder.decodeBitmap(source)
            }
        } catch (e: IOException) { }
        return imageBitmap
    }

    private fun tryReloadImage(previewPane: ImageView, passedImageUri: Uri?) {
        val imageBitmap = detectInImage(passedImageUri)
        try {
            previewPane?.setImageBitmap(imageBitmap)
        } catch (e: IOException) { }
    }

    private fun cropToSquare(previewPane: ImageView, bitmap: Bitmap) {
        //something
        val width  = bitmap.width
        val height = bitmap.height
        val newWidth = if (height > width) width else height
        val newHeight = if (height > width) height - ( height - width) else height
        var cropW = (width - height) / 2
        cropW = if (cropW < 0) 0 else cropW
        var cropH = (height - width) / 2
        cropH = if (cropH < 0) 0 else cropH
        val cropImg = Bitmap.createBitmap(bitmap, cropW, cropH, newWidth, newHeight)
        previewPane?.setImageBitmap(cropImg)
    }


    private fun modifyImages() {
        val previewPane3 = findViewById<ImageView>(R.id.previewPane3)
        val previewPane4 = findViewById<ImageView>(R.id.previewPane4)
        val imageBitmap = detectInImage(imageUri)
        val imageBitmap2 = detectInImage(imageUri2)
        if (imageBitmap != null) {
            cropToSquare(previewPane3,imageBitmap)
        }
        if (imageBitmap2 != null) {
            cropToSquare(previewPane4,imageBitmap2)
        }
    }

    companion object {
        private const val KEY_IMAGE_URI = "edu.uw.eep523.takepicture.KEY_IMAGE_URI_1"
        private const val KEY_IMAGE_URI2 = "edu.uw.eep523.takepicture.KEY_IMAGE_URI_2"
        private const val REQUEST_IMAGE_CAPTURE = 1001
        private const val REQUEST_CHOOSE_IMAGE = 1002
        private const val REQUEST_IMAGE_CAPTURE2 = 1003
        private const val REQUEST_CHOOSE_IMAGE2 = 1004
        private const val PERMISSION_REQUESTS = 1
    }
}
