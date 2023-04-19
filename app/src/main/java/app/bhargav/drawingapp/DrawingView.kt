package app.bhargav.drawingapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context ,attrs : AttributeSet) : View(context , attrs) {

    private var mDrawPath : CustomPath ? = null  // Variable of customPath inner class to use further
    private var  mCanvasBitmap : Bitmap?= null // an instance of bitmap
    private var mDrawPaint:Paint? =null // the paint class holds the style and color info
    private var mCanvasPaint :Paint?=null //an instanse of canvas paint
    private var mBrushSize : Float = 0.toFloat() // varible for brush size
    private var color = Color.BLACK
    private var canvas : Canvas?=null  //  Canvas class holds the drawing view
    private var mPaths = ArrayList<CustomPath>() // var to store the drawed thing

    private val mUndoPath = ArrayList<CustomPath>()   // crearting undo variable

    init {
        setUpDrawing()
    }
    // function to undo
    fun onClickUndo(){
        //checking whether path is there or not
        if(mPaths.size>0){
            mUndoPath.add(mPaths.removeAt(mPaths.size-1)) // removing the last written

            invalidate() // internally calls onDraw method
        }
    }

// To setup drawing page
    private fun setUpDrawing() {
       // TODO("Not yet implemented")
        mDrawPaint= Paint()
        mDrawPath = CustomPath(color,mBrushSize)
        mDrawPaint!!.color=color
        mDrawPaint!!.style= Paint.Style.STROKE
        mDrawPaint!!.strokeJoin=Paint.Join.ROUND
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
    //    mBrushSize =20.toFloat()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap= Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        canvas= Canvas(mCanvasBitmap!!)
    }
// Change canvas to canvas?if fails
    override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    canvas.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)

    for(path in mPaths){
        mDrawPaint!!.strokeWidth= path.brushThickness
        mDrawPaint!!.color= path.color
        canvas.drawPath(path, mDrawPaint!!)
    }
    if (!mDrawPath!!.isEmpty) {
        mDrawPaint!!.strokeWidth= mDrawPath!!.brushThickness
        mDrawPaint!!.color= mDrawPath!!.color
        canvas.drawPath(mDrawPath!!, mDrawPaint!!)
    }
}

    // For touch event in X and Y direction
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y


        when(event?.action){
            MotionEvent.ACTION_DOWN->{
                mDrawPath!!.color=color
                mDrawPath!!.brushThickness=mBrushSize

                mDrawPath!!.reset()
                if (touchY != null) {
                    if (touchX != null) {
                        mDrawPath!!.moveTo(touchX ,touchY)
                    }
                }
            }
            MotionEvent.ACTION_MOVE->{
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.lineTo(touchX,touchY)
                    }
                }
            }
            MotionEvent.ACTION_UP->{
                mPaths.add(mDrawPath!!)
                mDrawPath= CustomPath(color,mBrushSize)
            }
            else -> return false
        }

        invalidate()
        return true

    }

    // Setting the color
fun setColor(newColor : String){
    // Selecting the color
    color =  Color.parseColor(newColor)
    mDrawPaint!!.color = color
}

    // Setting the brush size
    fun setSizeForBrush(newSize : Float){
        //Select the brush size
        mBrushSize= TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,newSize,resources.displayMetrics)
        mDrawPaint!!.strokeWidth = mBrushSize
    }

    internal inner class CustomPath(var color :Int ,var brushThickness:Float) : Path(){

    }
}