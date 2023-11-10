package com.arbolesyazilim.arduinobluetoothcontroller


import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.arbolesyazilim.arduinobluetoothcontroller.databinding.ActivityMainBinding
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    private var bluetoothPermission = false
    private lateinit var binding: ActivityMainBinding
    private var bluetoothSocket: BluetoothSocket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.btDevicesText.setOnClickListener {
            val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
            val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
            if (bluetoothAdapter == null) {
                // Device doesn't support Bluetooth
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    blueToothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                } else {
                    blueToothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_ADMIN)
                }
            }
        }
        binding.btSendButton.setOnClickListener {
            binding.sendingDataText.visibility = View.VISIBLE
        }
    }

    private val blueToothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
            val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
            bluetoothPermission = true
            if (bluetoothAdapter?.isEnabled == false) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                btActivityResultLauncher.launch(enableBtIntent)
            } else {
                scanBT()
            }
        }
    }

    private val btActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            scanBT()
        }
    }

    @SuppressLint("MissingPermission")
    private fun scanBT() {
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
        val builder = AlertDialog.Builder(this@MainActivity)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.scan_bt, null)
        builder.setCancelable(false)
        builder.setView(dialogView)
        val btList = dialogView.findViewById<ListView>(R.id.btList)
        val dialog = builder.create()
        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter?.bondedDevices as Set<BluetoothDevice>
        val ADAhere: SimpleAdapter
        var data: MutableList<Map<String?, Any?>?>? = null
        data = ArrayList()
        if (pairedDevices.isNotEmpty()) {
            val dataNum1: MutableMap<String?, Any?> = HashMap()
            dataNum1["A"] = ""
            dataNum1["B"] = ""
            data.add(dataNum1)
            for (device in pairedDevices) {
                val dataNum: MutableMap<String?, Any?> = HashMap()
                dataNum["A"] = device.name
                dataNum["B"] = device.address
                data.add(dataNum)
            }
            val fromWhere = arrayOf("A")
            val viewWhere = intArrayOf(R.id.itemName)
            ADAhere = SimpleAdapter(this@MainActivity, data, R.layout.item_list, fromWhere, viewWhere)
            btList.adapter = ADAhere
            ADAhere.notifyDataSetChanged()
            btList.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, position, l ->
                val string = ADAhere.getItem(position) as HashMap<String, String>
                val deviceName = string["A"]
                binding.deviceName.text = deviceName

                // Bağlantıyı kurma işlemi
                val selectedDeviceAddress = string["B"]
                val selectedDevice = bluetoothAdapter.getRemoteDevice(selectedDeviceAddress)
                try {
                    bluetoothSocket = selectedDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                    bluetoothSocket?.connect()
                    Toast.makeText(this, "Bağlantı başarılı.", Toast.LENGTH_SHORT).show()
                } catch (e: IOException) {
                    Toast.makeText(this, "Bağlantı hatası: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
        } else {
            val value = "No devices found"
            Toast.makeText(this, value, Toast.LENGTH_LONG).show()
            return
        }
        dialog.show()
        binding.btSendButton.setOnClickListener {
            if (bluetoothSocket != null) {
                try {
                    val outputStream = bluetoothSocket?.outputStream
                    val editText = binding.editText
                    val message = editText.text.toString()
                    // Veriyi göstermek için TextView
                    binding.textView.text = message
                    outputStream?.write(message.toByteArray())
                    editText.text.clear() // Gönderdikten sonra EditText'i temizle
                } catch (e: IOException) {
                    // Veri gönderme hatası
                    e.printStackTrace()
                }
            }
        }





    }}
