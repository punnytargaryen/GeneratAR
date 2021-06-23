package com.example.generatar

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.generatar.Imagecaptureupload
import kotlinx.android.synthetic.main.activity_camer.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*
import java.util.*

class Imagecaptureupload : AppCompatActivity(), UploadRequestBody.UploadCallBack {
    var selectedimage: ImageView? = null
    var currentPhotoPath: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_imagecaptureupload)
        var CameraBtn: Button? = null
        var GalleryBtn: Button? = null
        var uploadBtn: Button? = null
        selectedimage = findViewById(R.id.imageView)
        CameraBtn = findViewById(R.id.cameraBtn)
        GalleryBtn = findViewById(R.id.galleryBtn)
        CameraBtn.setOnClickListener(View.OnClickListener { askCameraPermission() })
        GalleryBtn.setOnClickListener(View.OnClickListener {
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(gallery, GALLERY_REQ_CODE)
        })
    }

    private fun askCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERM_CODE)
        } else {
            dispatchTakePictureIntent()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permission: Array<String>, grantResults: IntArray) {
        if (requestCode == All_PERMS_CODE) {
            Log.d("TAG", "VerifyingPermission : Asking for permission ")
            val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            // index 0 = camera, index 1 = readStorage , index 2 = write Storage
            if (ContextCompat.checkSelfPermission(this.applicationContext, permissions[0]) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this.applicationContext, permissions[1]) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this.applicationContext, permissions[2]) == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent()
            } else {
                ActivityCompat.requestPermissions(this@Imagecaptureupload, permissions, All_PERMS_CODE)
            }
        } else {
            Toast.makeText(this, "Camera Permission is required to use camera", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CAMERA_REQ_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val f = File(currentPhotoPath)
                selectedimage!!.setImageURI(Uri.fromFile(f))
                Log.d("tag", "Absolute Url of image is " + Uri.fromFile(f))
                val bitmapDrawable = selectedimage!!.drawable as BitmapDrawable
                val bitmap = bitmapDrawable.bitmap
                saveimage(bitmap)
                uploadCameraImage(f)
            }
        }
        if (requestCode == GALLERY_REQ_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val contentUri = data!!.data
                selectedimage!!.setImageURI(contentUri)
               uploadImage(contentUri!!)
            }
        }
    }

    private fun saveimage(bitmap: Bitmap) {
        val fos: OutputStream
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = contentResolver
                val contentValues = ContentValues()
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "project" + ".jpg")
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "testfolder")
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = resolver.openOutputStream(Objects.requireNonNull(imageUri)!!)!!
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                Objects.requireNonNull<OutputStream?>(fos)
                Toast.makeText(this, "Image Saved", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Image Not Saved \n" + e.message, Toast.LENGTH_SHORT).show()
        }
    }

    @kotlin.jvm.Throws(IOException::class)
    private fun createImageFile(): File { // Create an image file name
        val imageFileName = "project_capture"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        //File storageDir = new File(Environment.getExternalStorageDirectory() + Environment.DIRECTORY_PICTURES);
        val image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",  /* suffix */
                storageDir /* directory */
        )
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.absolutePath
        return image
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(packageManager) != null) { // Create the File where the photo should go
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) { // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                val photoURI = FileProvider.getUriForFile(this,
                        "com.example.generatar.fileprovider",
                        photoFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, CAMERA_REQ_CODE)
            }
        }
    }

    private fun uploadImage(selectedimage: Uri) {
        if(selectedimage == null){
            layout_root.snackbar("Select an Image First")
            return
        }

        val parcelFileDescriptor = contentResolver.openFileDescriptor(selectedimage!!, "r", null) ?: return

        val file = File(cacheDir, contentResolver.getfilename(selectedimage!!))
        val inputStream = FileInputStream(parcelFileDescriptor.fileDescriptor)
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)

        progress_bar.progress = 0
        val body = UploadRequestBody(file, "image", this)

        MyAPI().uploadImage(
                MultipartBody.Part.createFormData("image", file.name, body),
                //RequestBody.create(MediaType.parse("multipart/form-data"), "Image For Generation")
        ).enqueue(object: Callback<UploadResponse> {
            override fun onResponse(call: Call<UploadResponse>, response: Response<UploadResponse>) {
                progress_bar.progress = 100
                layout_root.snackbar(response.body()?.message.toString())
            }

            override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                layout_root.snackbar(t.message!!)
            }

        })
    }

    private fun uploadCameraImage(file: File) {
        if(selectedimage == null){
            layout_root.snackbar("Select an Image First")
            return
        }



        progress_bar.progress = 0
        val body = UploadRequestBody(file, "image", this)

        MyAPI().uploadImage(
                MultipartBody.Part.createFormData("image", file.name, body),
                //RequestBody.create(MediaType.parse("multipart/form-data"), "Image For Generation")
        ).enqueue(object: Callback<UploadResponse> {
            override fun onResponse(call: Call<UploadResponse>, response: Response<UploadResponse>) {
                progress_bar.progress = 100
                layout_root.snackbar(response.body()?.message.toString())
            }

            override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                layout_root.snackbar(t.message!!)
            }

        })
    }

    override fun onProgressUpdate(percentage: Int) {
        progress_bar.progress = percentage

    }

    companion object {
        const val CAMERA_PERM_CODE = 101
        const val CAMERA_REQ_CODE = 102
        const val All_PERMS_CODE = 1101
        const val GALLERY_REQ_CODE = 105
    }
}