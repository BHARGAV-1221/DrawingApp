package app.bhargav.drawingapp

import android.Manifest.permission
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.AlertDialog
import android.app.Dialog
import android.app.appsearch.SetSchemaRequest.READ_EXTERNAL_STORAGE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.ImageReader
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private  var drawingView : DrawingView?= null
    private var mImageBtnCurrentPaint :ImageButton ?= null
var customProgressDailog : Dialog?= null
    // opening gallery and picking the image form gallery so we need launcher so creating
    val openGalleryLauncher : ActivityResultLauncher<Intent> =    // creating activity result launcher
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()){   // registering the activity result
                result ->  // inbuild key for the launcher
                //checking result is ok and not null
                if(result.resultCode == RESULT_OK && result.data != null){
                    val imageBackgrond : ImageView = findViewById(R.id.backgroud_img)
                    imageBackgrond.setImageURI(result.data?.data) // getting the image in URI form so declaring result.data?.data
                }
    }


     val requestPermission:ActivityResultLauncher<Array<String>> =
         registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
             permissions ->
             permissions.entries.forEach{
                 val permissionName = it.key
                 val isGranted = it.value

                 if(isGranted){
                    Toast.makeText(
                        this@MainActivity,"Permission Granted",
                        Toast.LENGTH_LONG)
                        .show()
                     val pickIntent = Intent(Intent.ACTION_PICK,
                         MediaStore.Images.Media.EXTERNAL_CONTENT_URI) // to pick image from gallery from an intent intent is nothing but new screen
                     openGalleryLauncher.launch(pickIntent)
                 }else{
                     if(permissionName == permission.READ_EXTERNAL_STORAGE){
                         Toast.makeText(this@MainActivity,"Read external storage permission not granted",Toast.LENGTH_LONG).show()
                     }
                 }
             }
         }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView=findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(20.toFloat())
        val linearLayoutPaintColor = findViewById<LinearLayout>(R.id.ll_paint_colors)
        mImageBtnCurrentPaint =linearLayoutPaintColor[1]as ImageButton
        mImageBtnCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.pallet_pressedl)
        )

        val removeBackground : ImageButton = findViewById(R.id.removebackground)
        removeBackground.setOnClickListener {
           removeBackgroundImage()
        }

            // Declaring brush image button
         val brush : ImageButton = findViewById(R.id.brush)
        brush.setOnClickListener {
            showBrushSizeChooserDialog() // calling brushsize method
        }

        // Declaring undo image button
        val undo : ImageButton = findViewById(R.id.undo)
        undo.setOnClickListener {
drawingView?.onClickUndo()  // calling undo method
        }
        // Declaring gallery image button
        val gallery :ImageButton = findViewById(R.id.gallery)
       gallery.setOnClickListener {
    requestStoragePermission() // calling request storage method
             }
      // Declaring save Image button
        val save :ImageButton = findViewById(R.id.save)
        save.setOnClickListener {
            // chekcing the readstroge methnod
           if(readStorageAllowed()){
               showProgressDialog()

               // coroutine things
               lifecycleScope.launch {
                   val flDrawingView :FrameLayout= findViewById(R.id.frame_drawing_view_container)  // getting the drawed image in one variable

                   saveBitmap(getBitmapFromView(flDrawingView)) // assing getting drawed image to the save method
               }
           }
        }
    }


    private fun removeBackgroundImage(){
        val imageBackgrond : ImageView = findViewById(R.id.backgroud_img)
        imageBackgrond.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.white))
    }


    // to show brush dailog
    private fun showBrushSizeChooserDialog(){
        val brushDailog = Dialog(this)
        brushDailog.setContentView(R.layout.dailog_brush_size)
        brushDailog.setTitle("Brush Size")
// to set sizes for brush
        val verySmallbtn:ImageButton = brushDailog.findViewById(R.id.verysmall)
        verySmallbtn.setOnClickListener {
            drawingView?.setSizeForBrush(5.toFloat())
            brushDailog.dismiss()
        }
        val smallbtn:ImageButton = brushDailog.findViewById(R.id.small)
        smallbtn.setOnClickListener{
            drawingView?.setSizeForBrush(10.toFloat())
            brushDailog.dismiss()
        }
        val medBtn:ImageButton = brushDailog.findViewById(R.id.medium)
        medBtn.setOnClickListener{
            drawingView?.setSizeForBrush(20.toFloat())
            brushDailog.dismiss()
        }
        val largeBtn:ImageButton = brushDailog.findViewById(R.id.large)
        largeBtn.setOnClickListener{
            drawingView?.setSizeForBrush(30.toFloat())
            brushDailog.dismiss()
        }
        brushDailog.show()
    }


    // For pick a paint color
    fun paintClicked(view:View){
        if(view !== mImageBtnCurrentPaint){
            val imageBtn= view as ImageButton
            val colorTag = imageBtn.tag.toString()  // getting tag which is present in xml using tag attribute
            drawingView!!.setColor(colorTag)

            imageBtn.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_pressedl)
            //assigning pillet pressed to imagebtn
            )
            mImageBtnCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_normal)
                //pressed btn is in pressed other one is in normal
            )

            mImageBtnCurrentPaint= view
        }
    }


    private fun readStorageAllowed():Boolean{
        var result = ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE)  // Checking whether read external storgae is granted or not
    return result== PackageManager.PERMISSION_GRANTED  // check its granted or not if its granted it will return true

    }
    // Requesting for permission
    private fun requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(
               this,
           android.Manifest.permission.READ_EXTERNAL_STORAGE )
        ){
            showRationalDialog("Kids Drawing App","Drawing app"+"needs to access your external storage")
        }else{
      //      showRationalDialog("Kids Drawing App","Drawing app"+"needs to access your external storage")
           requestPermission.launch(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,
           android.Manifest.permission.WRITE_EXTERNAL_STORAGE))



        }
    }

// to show dialog
    private fun showRationalDialog(title : String, message : String){
        val builder:AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("cancel"){
                dialog ,_ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    // COnverting image to bitmap to store in the device
    private fun getBitmapFromView(view: View):Bitmap{
        val returnBitmap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)  // craeting bitmap from view
        val canvas= Canvas(returnBitmap)  //assigning bitmap to Canvas
        val bgBackground = view.background
// validating whwther some background is there or not
if(bgBackground != null){
    bgBackground.draw(canvas)
}else{
    canvas.drawColor(Color.WHITE)
}

        view.draw(canvas) // converting view to bitmap
        return returnBitmap
    }


    private suspend fun saveBitmap(mBitmap:Bitmap?):String{
        var result = ""

        // coroutine android functionality which runs in background thread not in UI thread
       withContext(Dispatchers.IO){
           // checking bitmap is null or not
           if(mBitmap != null){
               try {
                   val bytes = ByteArrayOutputStream()    // getting images in byte
                   mBitmap.compress(Bitmap.CompressFormat.PNG,90,bytes) // compressing the image

                   // Storing the image in file
                   val fileInput = File(externalCacheDir?.absoluteFile.toString() +
                   File.separator + "Drawing_App" + System.currentTimeMillis()/1000+".png"
                   )
                   val fileOutput = FileOutputStream(fileInput)
                   fileOutput.write(bytes.toByteArray())
                   fileOutput.close()

                   result= fileInput.absolutePath

                   // to run on UI thread
                   runOnUiThread{
                       cancleProgressDialog()
                       if(result.isNotEmpty()){
                           Toast.makeText(this@MainActivity,
                               "File Saved Sucessfully :$result",
                               Toast.LENGTH_SHORT).show()
                         //  shareImage(result)  // if need to share enble this line it will share when you click on save button
                       }else{
                           Toast.makeText(this@MainActivity,
                               "Something went wrong",
                               Toast.LENGTH_SHORT).show()
                       }
                   }

               }catch (e : java.lang.Exception){
                   result = ""
                   e.printStackTrace()
               }
           }
       }
        return result
    }

    // Method used the create custom progress dialog

    private fun showProgressDialog(){
        customProgressDailog = Dialog(this@MainActivity)

        customProgressDailog?.setContentView(R.layout.customdialog)

        customProgressDailog?.show()   // to display dialog in screen

    }
    // Method used to cancle progress dialog
    private fun cancleProgressDialog(){
        if(customProgressDailog!= null){
            customProgressDailog?.dismiss()
            customProgressDailog = null
        }
    }


    private fun shareImage(result: String){
        MediaScannerConnection.scanFile(this,arrayOf(result),null){
            path,uri->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND    // property of intent
            shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
            shareIntent.type ="image/png/jpg"
            startActivity(Intent.createChooser(shareIntent,"share"))
        }
    }
}
