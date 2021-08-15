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
import java.text.NumberFormat
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var thisSeries: LineGraphSeries<DataPoint>
    private lateinit var mSensorManager: SensorManager
    private lateinit var mSensor: Sensor
    private lateinit var mSensorG: Sensor

    private val sizeN: Int = 5
    private val sizeM: Int = 5
    private val sizeL: Int = 20

    var slidingWindow = FloatArray(sizeN) { 0.0f }
    var detectWindow = FloatArray(sizeM) {0.0f}
    var activationWindow = FloatArray(sizeL) {0.0f}
    private val linearAcceleration: Array<Float> = arrayOf(0.0f,0.0f,0.0f)

    private var iter: Int = 0
    private var detectCounter: Int = 0
    private var activated: Boolean = false
    private val magThresh: Int = 15


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        //mSensorG =  (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)) -> returns null for BLUE Studio X8 HD
        mSensor = if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        } else { null!! } // Sorry, there are no accelerometers on your device.
        mSensorManager.registerListener(this, mSensor, 10000)

        thisSeries = LineGraphSeries()
        val mGraph = findViewById<GraphView>(R.id.mGraph)
        initGraphRT(mGraph, thisSeries)
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
        linearAcceleration[2] = event.values[2] // mostly ignored; this direction accounts for gravity

        val xval = System.currentTimeMillis()/1000.toDouble()//graphLastXValue += 0.1
        val mag: Float = magnitude(linearAcceleration)
        val mean: Float = mean(linearAcceleration)

        addToWindow(mag-mean, slidingWindow)
        addToWindow(movingAVG(iter), detectWindow)
        addToWindow(if (positiveSlopeThresh()) 0.0f else 1.0f, activationWindow)
        thisSeries.appendData(DataPoint(xval, movingAVG(iter).toDouble()), true, 105)

        activityStart()
        if (activated) {
            updateTextView(R.id.activation, "Activity Started")
            clearWindow(detectWindow)
        }
        if (positiveSlopeThresh() && activated) {
            detectCounter =  increment(detectCounter)
            updateTextView(R.id.counter, "counter $detectCounter/10")
            clearWindow(detectWindow)
        }
        if (iter<sizeN) iter++
//        Log.i("Kevin","$iter")
    }


    private fun initGraphRT(mGraph: GraphView, mSeries :LineGraphSeries<DataPoint>){

        mGraph.viewport.isXAxisBoundsManual = true
        //mGraph.getViewport().setMinX(0.0)
        //mGraph.getViewport().setMaxX(4.0)

        mGraph.viewport.isYAxisBoundsManual = true
        mGraph.viewport.setMinY(0.0)
        mGraph.viewport.setMaxY(20.0)
        mGraph.gridLabelRenderer.setLabelVerticalWidth(100)

        // first mSeries is a line
        mSeries.isDrawDataPoints = false
        mSeries.isDrawBackground = false
        mGraph.addSeries(mSeries)
        setLabelsFormat(mGraph)
    }

    /* Formatting the plot*/
    private fun setLabelsFormat(mGraph: GraphView) {

        val nfX = NumberFormat.getInstance()
        nfX.maximumFractionDigits = 2
        nfX.maximumIntegerDigits = 1

        val nfY = NumberFormat.getInstance()
        nfY.maximumFractionDigits = 0
        nfY.maximumIntegerDigits = 2

        mGraph.gridLabelRenderer.verticalAxisTitle = "m/s\u00B2"
        mGraph.gridLabelRenderer.horizontalAxisTitle = "Time"

        mGraph.gridLabelRenderer.labelFormatter = object : DefaultLabelFormatter(nfX,nfY) {
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
        mSensorManager.registerListener(this, mSensor, 10000)
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this)
        Log.d("tag","onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        mSensorManager.unregisterListener(this)
    }

    private fun addToWindow(x:Float, window: FloatArray) {
        // Shift everything one to the left
        for (i in 1 until window.size) {
            window[i - 1] = window[i]
        }
        // Add the new data point
        window[window.size - 1] = x
    }

    private fun magnitude(linearAcceleration:Array<Float>): Float{
        return sqrt(linearAcceleration[0].pow(2) + linearAcceleration[1].pow(2))  // + linearAcceleration[2].pow(2)
    }

    private fun mean(linearAcceleration: Array<Float>): Float {
        return (linearAcceleration[0] + linearAcceleration[1])/2 // + linearAcceleration[2]
    }

    private fun movingAVG(i:Int): Float {
        var sum: Float = 0.0f
        if (i<sizeN) {
            for (j in 0..i) sum += slidingWindow[j]
            return sum/i
        } else {
            return slidingWindow.sum()/slidingWindow.size
        }
    }

    private fun positiveSlopeThresh(): Boolean {
        // Conditions:
        //    1. Rising slope for last 5 samples (0.05 seconds)
        //    2. Cross 15 normalized magnitude (m/s^2)
        var cond: Boolean = true
        for (i in 0 until (detectWindow.size-1)) {
            cond = cond && (detectWindow[i+1]>detectWindow[i])
        }
        cond = cond && (detectWindow.minOrNull()!! <magThresh)
        cond = cond && (detectWindow.maxOrNull()!! >magThresh)
        return cond
    }

    private fun clearWindow(window: FloatArray) {
        for (i in window.indices) window[i] = 0.0f
    }

    private fun increment(counter:Int): Int {
        if (counter+1 > 10) return 1
        else return counter+1
    }

    private fun activityStart() {
        return activationWindow.sum() >= 2
    }

    fun updateTextView(passedID:Int, str:String?) {
        val textView = findViewById<View>(passedID) as TextView
        textView.text = str
    }
}




