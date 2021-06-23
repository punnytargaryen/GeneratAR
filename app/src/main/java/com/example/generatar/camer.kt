package com.example.generatar

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import cn.pedant.SweetAlert.SweetAlertDialog
import kotlinx.android.synthetic.main.activity_camer.*
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class camer : AppCompatActivity(), UploadRequestBody.UploadCallBack {

    private var selectedimage: Uri? = null
    val loadingdialog = LoadingDialog(this@camer)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camer)


        image_view.setOnClickListener {
            openImagechooser()
        }

        //camera.setOnClickListener {
        //    layout_root.snackbar("clicked")
         //    askCameraPermission()

        //}

        button_upload.setOnClickListener {
            uploadImage()
        }

        generate.setOnClickListener {
            loadingdialog.startloadingDialog()



            val client = OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.MINUTES)
                    .writeTimeout(5, TimeUnit.MINUTES)
                    .readTimeout(5, TimeUnit.MINUTES)
                    .build()
            val request = Request.Builder()
                    .url("http://bbe10b844e46.ngrok.io/").build()
            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    Log.e("tag", "The error", e)
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    loadingdialog.dismissdialog()
                    layout_root.snackbar("Success")
                    val intent = Intent(this@camer, MainActivity::class.java)
                    startActivity(intent)


                }
            })
            /*Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
                override fun run() {
                    loadingdialog.dismissdialog()
                    //pDialog.dismiss()
                }
            }, 50000)

            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this@camer, MainActivity::class.java)
                startActivity(intent)
            }, 120000)*/



        }
    }

    private fun askCameraPermission() {
        Toast.makeText(this, "1", Toast.LENGTH_SHORT).show()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "2", Toast.LENGTH_SHORT).show()
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERM_CODE)
        } else {
            Toast.makeText(this, "3", Toast.LENGTH_SHORT).show()
            dispatchTakePictureIntent()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == All_PERMS_CODE) {
            Log.d("TAG", "VerifyingPermission : Asking for permission ")
            val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            // index 0 = camera, index 1 = readStorage , index 2 = write Storage
            if (ContextCompat.checkSelfPermission(this.applicationContext, permissions[0]) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this.applicationContext, permissions[1]) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this.applicationContext, permissions[2]) == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent()
            } else {
                ActivityCompat.requestPermissions(this@camer, permissions, All_PERMS_CODE)
            }
        } else {
            Toast.makeText(this, "Camera Permission is required to use camera", Toast.LENGTH_SHORT).show()
        }
    }




    private fun uploadImage() {
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
        ).enqueue(object : Callback<UploadResponse> {
            override fun onResponse(call: Call<UploadResponse>, response: Response<UploadResponse>) {
                progress_bar.progress = 100
                layout_root.snackbar("Image Successfully Uploaded")
            }

            override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                progress_bar.progress = 100
                layout_root.snackbar("Uploaded")
            }

        })
    }

    private fun openImagechooser() {
        Intent(Intent.ACTION_PICK).also {
            it.type = "image/*"
            val mimeTypes = arrayOf("image/jpeg", "image/png")
            it.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            startActivityForResult(it, REQ_CODE_IMG_PICKER)
        }
    }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == CAMERA_REQ_CODE) {
                if (resultCode == Activity.RESULT_OK) {
                    val f = File(currentPhotoPath)
                    image_view.setImageURI(Uri.fromFile(f))
                    Log.d("tag", "Absolute Url of image is " + Uri.fromFile(f))
                    val bitmapDrawable = image_view!!.drawable as BitmapDrawable
                     val bitmap = bitmapDrawable.bitmap
                      saveimage(bitmap)
                }
            }
            if (requestCode == REQ_CODE_IMG_PICKER) {
                if (resultCode == Activity.RESULT_OK) {
                    selectedimage = data?.data
                    image_view.setImageURI(selectedimage)
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
                var uristring: String
                uristring = imageUri.toString()
                Log.d("sav", "Absolute Url of image is " + uristring)
                selectedimage = imageUri
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Image Not Saved \n" + e.message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onProgressUpdate(percentage: Int) {
        progress_bar.progress = percentage

    }

    lateinit var currentPhotoPath: String

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
                "JPEG_${timeStamp}_", /* prefix */
                ".jpg", /* suffix */
                storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
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
                startActivityForResult(takePictureIntent, Imagecaptureupload.CAMERA_REQ_CODE)
            }
        }
    }



    companion object{
        private const val REQ_CODE_IMG_PICKER = 2
        const val CAMERA_PERM_CODE = 101
        const val CAMERA_REQ_CODE = 102
        const val All_PERMS_CODE = 1101
    }
    }


