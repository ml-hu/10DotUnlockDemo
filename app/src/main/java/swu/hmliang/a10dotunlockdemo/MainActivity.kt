package swu.hmliang.a10dotunlockdemo

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import androidx.core.graphics.contains
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.StringBuilder

class MainActivity : AppCompatActivity() {
    //1.先声明后创建
    //用数组保存9个圆点的对象 用于滑动过程中进行遍历 -> onCreate()
    //private var dots:Array<ImageView>? =null

    //请求图片或者视频的请求码
    private val  REQUEST_IMAGE_CODE = 123
    private val  REQUEST_VIDEO_CODE = 124

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //dots = arrayOf(sDot1,sDot2,sDot3,sDot4,sDot5,sDot6,sDot7,sDot8,sDot9)

        //获取密码
        SharedPreferenceUtil.getInstance(this).getPassword().also {
            if (it == null){
                mAlert.text = "请设置密码图案"
            }else{
                mAlert.text = "请解锁密码图案"
                orgPassword = it
            }
        }
        //给头像添加点击事件
        mHeader.setOnClickListener {
            //从相机/相册获取一张图片
            Intent().apply {
                action = Intent.ACTION_PICK
                type = "image/*"
                startActivityForResult(this,REQUEST_IMAGE_CODE)
            }
        }
        //获取头像
        File(filesDir,"header.jpg").also {
            if (it.exists()){
                BitmapFactory.decodeFile("${filesDir.path}/header.jpg").also {image ->
                    mHeader.setImageBitmap(image)
                }
            }
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            REQUEST_IMAGE_CODE -> {
                //图片
                //判断用户是否取消操作
                if(resultCode != Activity.RESULT_CANCELED){
                    //获取图片
                    val uri = data?.data
                    uri?.let {
                        //对IO操作尽量使用use
                        contentResolver.openInputStream(uri).use {
                            //Bitmap
                            BitmapFactory.decodeStream(it).also {image ->
                                //显示图片
                                mHeader.setImageBitmap(image)
                                //把图片缓存起来
                                val file =  File(filesDir,"header.jpg")
                                FileOutputStream(file).also {fos ->
                                    image.compress(Bitmap.CompressFormat.JPEG,50,fos)

                                }

                            }
                        }

                    }



                }
            }

            REQUEST_VIDEO_CODE -> {
                //视频
            }
        }
    }
    //2.懒加载
    private val dots:Array<ImageView>? by lazy {
        arrayOf(sDot1,sDot2,sDot3,sDot4,sDot5,sDot6,sDot7,sDot8,sDot9)
    }

    //将触摸点坐标转化为相对位置的坐标
    //使用懒加载获取屏幕的状态栏和标题栏的高度
    //使用懒加载确保只被调用一次

    private val barHeight:Int by lazy {
        //获取屏幕的宽度
        val display = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(display)

        //获取操作区域的尺寸
        val drawingRect = Rect()
        window.findViewById<ViewGroup>(Window.ID_ANDROID_CONTENT).getDrawingRect(drawingRect)

        display.heightPixels-drawingRect.height()
    }

    private fun convertTouchLocationToContainer(event:MotionEvent):Point{
        return Point().apply {
            //x坐标 = 触摸点的x-容器的x
            x = (event.x - mContainer.x).toInt()
            //y坐标 = 触摸点的y - 状态栏高度 - 容器的y
            y = (event.y-barHeight-mContainer.y).toInt()

        }
    }

    //查询触摸点是否在某个圆点上
    private fun findViewContainsPoint(point: Point):ImageView?{
        dots?.forEach { dotsview ->
            //p
            getRect(dotsview).also {
                if (it.contains(point.x,point.y)){
                    return dotsview
            }

            }
        }
        return null
    }

    //获取视图对应的Rect
    private fun getRect(v:ImageView) = Rect(v.left,v.top,v.right,v.bottom)

    //保存当前被点亮的视图
    private val allSelectedViews = mutableListOf<ImageView>()

    //用于记录滑动过程中的轨迹
    private val password = StringBuilder()
    //记录原始密码
    private var orgPassword:String? = null
    //记录第一次设置的密码
    private var firstPassword:String? = null

    //保存所有线的tag值
    private val alllinesTags = arrayOf(
        12,23,45,56,78,89,/*6条横线*/
    14,25,36,47,58,69,/*6条竖线*/
    24,35,57,68,15,26,48,59/*斜线*/
    )

    //记录最后被点亮的圆点对象
    private var lastSelectedView:ImageView? = null


    //监听触摸事件
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        //将触摸点的坐标转化到mContainer上
        val locacation:Point = convertTouchLocationToContainer(event!!)

        //判断是否在操作区域内
        if (!((locacation.x >= 0 && locacation.x <= mContainer.width)&&
            (locacation.y >= 0 && locacation.y <= mContainer.height))){
            return true
        }

        when(event?.action){
            MotionEvent.ACTION_DOWN -> {

                findViewContainsPoint(locacation).also {
                    hightlightView(it)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                findViewContainsPoint(locacation).also {
                    hightlightView(it)
                }
            }
            MotionEvent.ACTION_UP -> {
                //判断是不是第一次
                if(orgPassword== null){
                    //是不是设置密码的第一次
                    if(firstPassword == null){
                        //记录第一次的密码
                        firstPassword = password.toString()
                        //提示输入第二次
                        mAlert.text = "请确认密码图案"
                    }else{
                        //确认密码
                        comparePassword(firstPassword!!,password.toString())
                    }
                }else{
                    //确认密码
                    comparePassword(firstPassword!!,password.toString())
                }
                SharedPreferenceUtil.getInstance(this).deletePassword()
                reset()
            }
        }
        return true

    }

    //判断两次密码是否相同
    private fun comparePassword(first:String,second:String){
        if(first == second){
            //两次密码一致
            mAlert.text = "设置密码成功"
            //保存密码
            SharedPreferenceUtil.getInstance(this).savePassword(firstPassword!!)
        }else{
            mAlert.text = "两次密码不一样 请重新设置密码"
            firstPassword = null
        }
    }

    //点亮视图
    private fun hightlightView(v:ImageView?){
        if (v !=null && v.visibility == View.INVISIBLE){
            //判断这个点是不是第一个点
            if (lastSelectedView == null){
                //第一个点 只需要点亮 并且保存
                highlightDot(v)

            }else{
                //在滑动的时候已经点亮过其他点了
                //获取上一个点和这个点之间的tag值
                val previous:Int = (lastSelectedView?.tag as String).toInt()
                val current: Int = (v.tag as String).toInt()
                val lineTag = if (previous > current) current*10+previous else previous*10+current

                //判断是否有这条线
                if(alllinesTags.contains(lineTag)){
                    //点亮这个点
                    highlightDot(v)
                    //点亮这个线
                    mContainer.findViewWithTag<ImageView>(lineTag.toString()).apply {
                        visibility = View.VISIBLE
                        allSelectedViews.add(this)
                    }
                }
            }


        }

    }

    //点亮一个点
    private fun highlightDot(v:ImageView){
        //点亮这个点
        v.visibility = View.VISIBLE
        allSelectedViews.add(v)
        password.append(v.tag)
        //当前点亮的就是下一个点亮的上一个点
        lastSelectedView = v

    }

    //还原操作
    private fun reset(){
        //遍历保存点亮点的数组
        for(item in allSelectedViews){
            item.visibility = View.INVISIBLE
        }

        //清空
        allSelectedViews.clear()
        lastSelectedView = null

        Log.v("hmlnb",password.toString())
        password.clear()


    }



}
