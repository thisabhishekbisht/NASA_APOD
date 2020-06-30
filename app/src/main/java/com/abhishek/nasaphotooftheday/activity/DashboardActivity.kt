package com.abhishek.nasaphotooftheday.activity

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.*
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.abhishek.nasaphotooftheday.R
import com.abhishek.nasaphotooftheday.model.APODModel
import com.abhishek.nasaphotooftheday.model.APODWithDateModel
import com.abhishek.nasaphotooftheday.retrofit.RetrofitApiInterface
import com.abhishek.nasaphotooftheday.retrofit.RetrofitClient
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import dmax.dialog.SpotsDialog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*


class DashboardActivity : AppCompatActivity() {
    var videoView: VideoView? = null
    var zoomView: ImageView? = null
    var calender: ImageView? = null
    var tv_title: TextView? = null
    var tv_discription: TextView? = null
    var imageView_APOD: ImageView? = null

    var mediaType_Image: Boolean = true
    var isImageFitToScreen: Boolean = false
    lateinit var layout_first: View
    lateinit var layout_second: View

    lateinit var layout_third: View

    //
    lateinit var dialog: AlertDialog
    var isFirstEntry: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        init()

        setListners()
        getApiData()
    }

    private fun setListners() {

        // set listner for calender

        calender?.setOnClickListener {
            // get current date data and set it on calender innstance
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val dpd = DatePickerDialog(
                this,
                DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->

                    // convert date into required format
                    converToSpecificFormat(dayOfMonth, monthOfYear, year)

                },
                year,
                month,
                day
            )

            dpd.show()
        }

        // set listner for zoom or full screen vedio view

        zoomView?.setOnClickListener {

            // if media type is image
            if (mediaType_Image == true) {
                // zoom image to full screen

                // check if already zoomed
                if (isImageFitToScreen) {
                    // already zoomed
                    isImageFitToScreen = false
                    layout_first?.visibility = View.VISIBLE
                    layout_second?.visibility = View.VISIBLE
                    imageView_APOD?.setLayoutParams(
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    )
                    imageView_APOD?.setAdjustViewBounds(true)
                } else {
                    // not zoomed
                    isImageFitToScreen = true
                    // set view gone for full screen

                    layout_first?.visibility = View.GONE
                    layout_second?.visibility = View.GONE
                    //
                    layout_third.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )

                    imageView_APOD?.setLayoutParams(
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                    )
                    imageView_APOD?.setScaleType(ImageView.ScaleType.FIT_XY)
                }
            }
            // else is vedio
            else {

                layout_third.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                val metrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(metrics)
                val params =
                    videoView!!.layoutParams as LinearLayout.LayoutParams
                params.width = metrics.widthPixels
                params.height = metrics.heightPixels
                params.leftMargin = 0
                videoView!!.layoutParams = params

            }

        }
    }

    private fun converToSpecificFormat(day: Int, monthOfYear: Int, year: Int) {

        val calendar = Calendar.getInstance()
        calendar[year, monthOfYear] = day

        val format = SimpleDateFormat("YYYY-MM-dd")
        val strDate = format.format(calendar.time)


        // call second api with corresponding date

//day
        getApiDataWithDate(strDate)

    }

    private fun getApiData() {

        if (isOnline(this)) {

            try {
                // show progress dialouge while api recieve data from remote server
                showProgressDialouge()

                val retrofitApiInterface: RetrofitApiInterface? =
                    RetrofitClient.buildService(RetrofitApiInterface::class.java)

                val call: Call<APODModel> =
                    retrofitApiInterface?.GetAPODResponse(getString(R.string.api_key))!!

                call.enqueue(object : Callback<APODModel> {
                    override fun onResponse(call: Call<APODModel>, response: Response<APODModel>) {
                        if (response.isSuccessful) {

                            //
                            isFirstEntry = false
                            // hide dialouge when sucessfull response
                            hideProgressDialouge()
                            var title: String = response.body()?.title!!
                            var discription: String = response.body()?.explanation!!
                            var mediaType: String = response.body()?.mediaType!!
                            var hdurl: String = response.body()?.hdurl!!
                            var url: String = response.body()?.url!!

                            // set recieved data to fileds

                            tv_title?.setText(title)
                            tv_discription?.setText(discription)

                            if (mediaType.equals("image")) {
                                // download image using library and show it in image view

                                // ser image resource
                                zoomView?.setImageResource(R.drawable.ic_baseline_zoom_in_24)

                                imageView_APOD?.let {
                                  loadImageIntoView(hdurl)
                                }

                            } else {
                                 // set image resource
                                zoomView?.setImageResource(R.drawable.ic_baseline_play_arrow_24)
                                //
                                mediaType_Image = false
                                // hide image view and show vedio player

                                imageView_APOD?.visibility = View.GONE

                                videoView?.visibility = View.VISIBLE

                                // play vedio
                                playVedioWithUri(hdurl)
                            }


                            Toast.makeText(this@DashboardActivity, "Sucessful", Toast.LENGTH_LONG)
                                .show()


                        } else {
                            // hide dialouge
                            hideProgressDialouge()
                            Toast.makeText(
                                this@DashboardActivity,
                                "Something went wrong, Please try again",
                                Toast.LENGTH_SHORT
                            )
                        }
                    }

                    override fun onFailure(call: Call<APODModel>, t: Throwable) {
                        // hide dialouge when onFailure response
                        hideProgressDialouge()
                        Toast.makeText(this@DashboardActivity, "${t.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
                })
            } catch (e: Exception) {
                hideProgressDialouge()
                Toast.makeText(this@DashboardActivity, "${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            showInternetDialouge()
            Toast.makeText(
                this@DashboardActivity,
                "Please check your internet connection , then try again",
                Toast.LENGTH_LONG
            ).show()
        }

    }

    private fun loadImageIntoView(hdurl: String) {
        imageView_APOD?.let {
            Glide.with(this@DashboardActivity).load(hdurl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.drawable.placeholder) //5
                .error(R.drawable.error_placeholder) //6
                .fallback(R.drawable.placeholder) //7
                .into(it)
        }
    }

    private fun showProgressDialouge() {
        dialog = SpotsDialog.Builder().setContext(this).build()
        dialog.show()
    }

    override fun onBackPressed() {

        if (isImageFitToScreen) {
            // it is already zoomed so
            layout_third.layoutParams =
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 2.0f)


            imageView_APOD?.setLayoutParams(
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            )
            imageView_APOD?.setAdjustViewBounds(true)
            isImageFitToScreen = false
            layout_first?.visibility = View.VISIBLE
            // set weight
            layout_first.layoutParams =
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f)

            layout_second?.visibility = View.VISIBLE

            // set weight
            layout_second.layoutParams =
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f)
        } else {
            // do nothing
            super.onBackPressed()
        }

    }

    private fun getApiDataWithDate(date: String) {

        if (isOnline(this)) {


            try {
                // show progress dialouge
                showProgressDialouge()

                val retrofitApiInterface: RetrofitApiInterface? =
                    RetrofitClient.buildService(RetrofitApiInterface::class.java)

                val call: Call<APODWithDateModel> =
                    retrofitApiInterface?.GetAPODResponseWithDate(
                        getString(R.string.api_key),
                        date
                    )!!


                call.enqueue(object : Callback<APODWithDateModel> {
                    override fun onResponse(
                        call: Call<APODWithDateModel>,
                        response: Response<APODWithDateModel>
                    ) {
                        if (response.isSuccessful) {

                            // set progress bar gone
                            hideProgressDialouge()

                            var title: String = response.body()?.title!!

                            Toast.makeText(
                                this@DashboardActivity,
                                "Data is being updated for Given Date " + title,
                                Toast.LENGTH_LONG
                            ).show()
                            var discription: String = response.body()?.explanation!!
                            var mediaType: String = response.body()?.mediaType!!
                            var hdurl: String = response.body()?.hdurl!!
                            var url: String = response.body()?.url!!

                            // set recieved data to fileds

                            tv_title?.setText(title)
                            tv_discription?.setText(discription)

                            if (mediaType.equals("image")) {
                                mediaType_Image = true
                                // download image using library and show it in image view

                              loadImageIntoView(hdurl)

                            } else {

                                mediaType_Image = false
                                // hide image view and show vedio player

                                imageView_APOD?.visibility = View.GONE

                                videoView?.visibility = View.VISIBLE

                                // play vedio
                                playVedioWithUri(hdurl)
                            }
                        } else {
                            // hide dilaouge
                            hideProgressDialouge()
                            Toast.makeText(
                                this@DashboardActivity,
                                "Error with Response",
                                Toast.LENGTH_LONG
                            )
                        }
                    }

                    override fun onFailure(call: Call<APODWithDateModel>, t: Throwable) {
                        Toast.makeText(this@DashboardActivity, "${t.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
                })

            } catch (e: Exception) {
                Toast.makeText(this@DashboardActivity, "${e.message}", Toast.LENGTH_SHORT).show()
            }

        } else {
            showInternetDialouge()
            Toast.makeText(
                this@DashboardActivity,
                "Please check your internet connection , then try again",
                Toast.LENGTH_LONG
            ).show()
        }

    }

    private fun hideProgressDialouge() {
        dialog?.dismiss()
    }

    private fun showInternetDialouge() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setMessage("Please check your internet connection , then try again!")
            .setCancelable(false)
            .setPositiveButton("OK", DialogInterface.OnClickListener { dialog, id ->
                //do things
                if (isFirstEntry) {
                    getApiData()

                }
            })
        val alert: AlertDialog = builder.create()
        alert.show()
    }


    private fun init() {
        imageView_APOD = findViewById(R.id.imageView_APOD)
        videoView = findViewById(R.id.videoView)
        calender = findViewById(R.id.view_Calender)
        zoomView = findViewById(R.id.iv_Zoom)
        // textview
        tv_title = findViewById(R.id.tv_title)
        tv_discription = findViewById(R.id.tv_discription)

        tv_discription?.setMovementMethod(ScrollingMovementMethod())
        // linear layout
        layout_first = findViewById(R.id.layout_first)
        layout_second = findViewById(R.id.layout_second)
        layout_third = findViewById(R.id.layout_third)

    }

    fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                    return true
                }
            }
        }
        return false
    }

    private fun playVedioWithUri(s: String) {
        //Creating MediaController
        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        //specify the location of media file

        //Setting MediaController and URI, then starting the videoView
        videoView?.setMediaController(mediaController)
        videoView?.setVideoURI(Uri.parse(s))
        videoView?.requestFocus()
        videoView?.start()
    }
}

