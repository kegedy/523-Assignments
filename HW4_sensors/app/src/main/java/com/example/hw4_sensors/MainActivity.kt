package com.example.hw4_sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import com.jjoe64.graphview.DefaultLabelFormatter
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import java.lang.Math.pow
import java.text.NumberFormat
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private  lateinit var mSeriesXaccel: LineGraphSeries<DataPoint>
    private lateinit var mSeriesYaccel: LineGraphSeries<DataPoint>
    private lateinit var mSeriesZaccel: LineGraphSeries<DataPoint>

    private lateinit var mSensorManager: SensorManager
    private lateinit var mSensor: Sensor
    private lateinit var mSensorG: Sensor

    private val linearAcceleration: Array<Float> = arrayOf(0.0f,0.0f,0.0f)
    var slidingWindow = FloatArray(5) { 0.0f }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        //mSensorG =  (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE))
        mSensor = if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        } else {
            // Sorry, there are no accelerometers on your device.
            null!!
        }

        mSensorManager.registerListener(this, mSensor, 40000)

        mSeriesXaccel = LineGraphSeries()
        mSeriesYaccel = LineGraphSeries()
        mSeriesZaccel = LineGraphSeries()
        val mGraphX = findViewById<GraphView>(R.id.mGraphX)
        val mGraphY = findViewById<GraphView>(R.id.mGraphY)
        val mGraphZ = findViewById<GraphView>(R.id.mGraphZ)
        initGraphRT(mGraphX, mSeriesXaccel)
        initGraphRT(mGraphY, mSeriesYaccel)
        initGraphRT(mGraphZ, mSeriesZaccel)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do something here if sensor accuracy changes.
    }

    override fun onSensorChanged(event: SensorEvent) {
        // In this example, alpha is calculated as t / (t + dT),
        // where t is the low-pass filter's time-constant and
        // dT is the event delivery rate.
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER)
            return

        /*
             * It is not necessary to get accelerometer events at a very high
             * rate, by using a slower rate (SENSOR_DELAY_UI), we get an
             * automatic low-pass filter, which "extracts" the gravity component
             * of the acceleration. As an added benefit, we use less power and
             * CPU resources.
       */


        linearAcceleration[0] = event.values[0]
        linearAcceleration[1] = event.values[1]
        linearAcceleration[2] = event.values[2]


        val xval = System.currentTimeMillis()/1000.toDouble()//graphLastXValue += 0.1
        mSeriesXaccel.appendData(DataPoint(xval, linearAcceleration[0].toDouble()), true, 50)
        mSeriesYaccel.appendData(DataPoint(xval, linearAcceleration[1].toDouble()), true, 50)
        mSeriesZaccel.appendData(DataPoint(xval, linearAcceleration[2].toDouble()), true, 50)
        var mag: Float = sqrt(linearAcceleration[0].pow(2) + linearAcceleration[1].pow(2) + linearAcceleration[2].pow(2))
        addToWindow(mag)
    }


    private fun initGraphRT(mGraph: GraphView, mSeriesXaccel :LineGraphSeries<DataPoint>){

        mGraph.viewport.isXAxisBoundsManual = true
        //mGraph.getViewport().setMinX(0.0)
        //mGraph.getViewport().setMaxX(4.0)
        mGraph.viewport.isYAxisBoundsManual = true;


        mGraph.viewport.setMinY(0.0);
        mGraph.viewport.setMaxY(10.0);
        mGraph.gridLabelRenderer.setLabelVerticalWidth(100)

        // first mSeries is a line
        mSeriesXaccel.isDrawDataPoints = false
        mSeriesXaccel.isDrawBackground = false
        mGraph.addSeries(mSeriesXaccel)
        setLabelsFormat(mGraph,1,2)

    }

    /* Formatting the plot*/
    private fun setLabelsFormat(mGraph: GraphView, maxInt:Int, maxFraction:Int){
        val nf = NumberFormat.getInstance()
        nf.maximumFractionDigits = maxFraction
        nf.maximumIntegerDigits = maxInt

        mGraph.gridLabelRenderer.verticalAxisTitle = "m/s\u00B2"
        mGraph.gridLabelRenderer.horizontalAxisTitle = "Time"

        mGraph.gridLabelRenderer.labelFormatter = object : DefaultLabelFormatter(nf,nf) {
            override fun formatLabel(value: Double, isValueX: Boolean): String {
                return if (isValueX) {
                    super.formatLabel(value, isValueX)+ "s"
                } else {
                    super.formatLabel(value, isValueX)
                }
            }
        }
    }

    override fun onResume() {
        Log.d("tag","onResume")
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        Log.d("tag","onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        mSensorManager.unregisterListener(this)
    }

    private fun addToWindow(x: Float) {
        // Shift everything one to the left
        for (i in 1 until slidingWindow.size) {
            slidingWindow[i - 1] = slidingWindow[i]
        }
        // Add the new data point
        slidingWindow[slidingWindow.size - 1] = x
    }

    fun updateTextView(passedID:Int, str:String?) {
        val textView = findViewById<View>(passedID) as TextView
        textView.text = str
    }

}



