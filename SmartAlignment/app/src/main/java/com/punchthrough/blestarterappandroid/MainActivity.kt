/*
 * Copyright 2019 Punch Through Design LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.punchthrough.blestarterappandroid

//import kotlinx.android.synthetic.main.activity_main.scan_results_recycler_view
import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.jjoe64.graphview.DefaultLabelFormatter
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.punchthrough.blestarterappandroid.ble.ConnectionEventListener
import com.punchthrough.blestarterappandroid.ble.ConnectionManager
import com.punchthrough.blestarterappandroid.ble.MyConnectionManager
import kotlinx.android.synthetic.main.activity_main.scan_button
import kotlinx.android.synthetic.main.activity_main.disconnect
import kotlinx.android.synthetic.main.activity_main.device_address
import kotlinx.android.synthetic.main.activity_main.device_name
import kotlinx.android.synthetic.main.activity_main.mGraphX
//import kotlinx.android.synthetic.main.activity_main.mGraphY
//import kotlinx.android.synthetic.main.activity_main.mGraphZ
import kotlinx.android.synthetic.main.activity_main.signal_strength
import org.jetbrains.anko.alert
import timber.log.Timber
import java.text.NumberFormat

private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val LOCATION_PERMISSION_REQUEST_CODE = 2

class MainActivity : AppCompatActivity() {

    /*******************************************
     * Properties
     *******************************************/

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private var isScanning = false
        set(value) {
            field = value
            runOnUiThread { scan_button.text = if (value) "Stop Scan" else "Start Scan" }
        }
    private var isConnected = false
        set(value) {
            field = value
            runOnUiThread {
                if (!value) resetConnectionDisplay()
            }
        }

    private var device: BluetoothDevice? = null

    private val scanResults = mutableListOf<ScanResult>()
    private val scanResultAdapter: ScanResultAdapter by lazy {
        ScanResultAdapter(scanResults) { result ->
            if (isScanning) {
                stopBleScan()
            }
            with(result.device) {
                Timber.w("Connecting to $address")
                ConnectionManager.connect(this, this@MainActivity)
            }
        }
    }

    private val isLocationPermissionGranted
        get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    /*******************************************
     * Activity function overrides
     *******************************************/

    private fun initGraphRT(mGraph: GraphView, mSeries :LineGraphSeries<DataPoint>){
        mGraph.viewport.isXAxisBoundsManual = true
        mGraph.getViewport().setMinX(0.0)
        mGraph.getViewport().setMaxX(10.0)

        mGraph.viewport.isYAxisBoundsManual = true
        mGraph.viewport.setMinY(-45.0)
        mGraph.viewport.setMaxY(45.0)
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

        mGraph.gridLabelRenderer.verticalAxisTitle = "Pitch Angle\u00B0"
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        scan_button.setOnClickListener { if (isScanning) stopBleScan() else startBleScan() }
        disconnect.setOnClickListener {
            if (device != null) {
                MyConnectionManager.teardownConnection(device!!)
                resetConnectionDisplay()
            }
        }

        initGraphRT(mGraphX, mSeriesXaccel)
//        initGraphRT(mGraphY, mSeriesYaccel)
//        initGraphRT(mGraphZ, mSeriesZaccel)
        //setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        ConnectionManager.registerListener(connectionEventListener)
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ENABLE_BLUETOOTH_REQUEST_CODE -> {
                if (resultCode != Activity.RESULT_OK) {
                    promptEnableBluetooth()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_DENIED) {
                    requestLocationPermission()
                } else {
                    startBleScan()
                }
            }
        }
    }

    fun resetConnectionDisplay() {
        device_name.text = "No Device Detected"
        device_address.text = "MAC: XX:XX:XX:XX:XX"
        signal_strength.text = ""
    }

    /*******************************************
     * Private functions
     *******************************************/

    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
        }
    }

    private fun startBleScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isLocationPermissionGranted) {
            requestLocationPermission()
        } else {
            scanResults.clear()
            scanResultAdapter.notifyDataSetChanged()
            bleScanner.startScan(null, scanSettings, scanCallback)
            isScanning = true
        }
    }

    private fun stopBleScan() {
        bleScanner.stopScan(scanCallback)
        isScanning = false
    }

    private fun requestLocationPermission() {
        if (isLocationPermissionGranted) {
            return
        }
        runOnUiThread {
            alert {
                title = "Location permission required"
                message = "Starting from Android M (6.0), the system requires apps to be granted " +
                    "location access in order to scan for BLE devices."
                isCancelable = false
                positiveButton(android.R.string.ok) {
                    requestPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                }
            }.show()
        }
    }

//    private fun setupRecyclerView() {
//        scan_results_recycler_view.apply {
//            adapter = scanResultAdapter
//            layoutManager = LinearLayoutManager(
//                this@MainActivity,
//                RecyclerView.VERTICAL,
//                false
//            )
//            isNestedScrollingEnabled = false
//        }
//
//        val animator = scan_results_recycler_view.itemAnimator
//        if (animator is SimpleItemAnimator) {
//            animator.supportsChangeAnimations = false
//        }
//    }

    /*******************************************
     * Callback bodies
     *******************************************/

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (result.device.name == "SmartAlignment") {
                device_name.text = result.device.name
                device_address.text = result.device.address
                signal_strength.text = result.rssi.toString()+"dBm"
                stopBleScan()
                device = result.device
                MyConnectionManager.connect(result.device, this@MainActivity)
                isConnected = true
                //scanResults.add(result)
            }
//            val indexQuery = scanResults.indexOfFirst { it.device.address == result.device.address }
//            if (indexQuery != -1) { // A scan result already exists with the same address
//                scanResults[indexQuery] = result
//                scanResultAdapter.notifyItemChanged(indexQuery)
//            } else {
//                with(result.device) {
//                    Timber.i("Found BLE device! Name: ${name ?: "Unnamed"}, address: $address")
//                }
//                scanResults.add(result)
//                scanResultAdapter.notifyItemInserted(scanResults.size - 1)
//            }
        }

        override fun onScanFailed(errorCode: Int) {
            Timber.e("onScanFailed: code $errorCode")
        }
    }

    private val connectionEventListener by lazy {
        ConnectionEventListener().apply {
            onConnectionSetupComplete = { gatt ->
                Intent(this@MainActivity, BleOperationsActivity::class.java).also {
                    it.putExtra(BluetoothDevice.EXTRA_DEVICE, gatt.device)
                    startActivity(it)
                }
                ConnectionManager.unregisterListener(this)
            }
            onDisconnect = {
                runOnUiThread {
                    alert {
                        title = "Disconnected"
                        message = "Disconnected or unable to connect to device."
                        positiveButton("OK") {}
                    }.show()
                }
            }
        }
    }

    /*******************************************
     * Extension functions
     *******************************************/

    private fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun Activity.requestPermission(permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
    }
}
