package com.omerek.guitarlearn.led

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import java.util.UUID
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * ESP32-S3 kartına Bluetooth Low Energy ile bağlanır. Kart "Nordic UART" servisini sunar:
 * telefon RX karakteristiğine yazar, kart TX karakteristiğinden bildirim (notify) gönderir.
 * USB ile aynı satır tabanlı protokol taşınır.
 */
@SuppressLint("MissingPermission") // İzinler LedController'da denetlenir
internal class BleLink private constructor(
    private val onReady: (BleLink) -> Unit,
    private val onLost: (BleLink) -> Unit,
) : LedLink {
    companion object {
        val SERVICE: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        private val RX: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e") // telefon -> kart
        private val TX: UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e") // kart -> telefon
        private val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        /**
         * Bağlantıyı başlatır. Hazır olunca [onReady], bağlantı düşerse [onLost] çağrılır.
         * @param autoConnect true ise kart menzile girince Android kendiliğinden bağlanır (daha yavaş).
         */
        fun connect(
            context: Context,
            device: BluetoothDevice,
            autoConnect: Boolean,
            onReady: (BleLink) -> Unit,
            onLost: (BleLink) -> Unit,
        ): BleLink {
            val link = BleLink(onReady, onLost)
            link.gatt = device.connectGatt(context, autoConnect, link.callback, BluetoothDevice.TRANSPORT_LE)
            return link
        }
    }

    val address: String? get() = gatt?.device?.address

    @Volatile private var gatt: BluetoothGatt? = null
    @Volatile private var rx: BluetoothGattCharacteristic? = null
    @Volatile private var payload = 20 // MTU - 3; MTU büyütülünce artar
    @Volatile private var closed = false
    @Volatile private var ready = false
    private val writeDone = Semaphore(0)
    @Volatile private var writeOk = false
    @Volatile private var lineHandler: ((String) -> Unit)? = null
    private val line = StringBuilder()

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> g.discoverServices()
                BluetoothProfile.STATE_DISCONNECTED -> lost()
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val service = g.getService(SERVICE)
            if (status != BluetoothGatt.GATT_SUCCESS || service == null) { lost(); return }
            rx = service.getCharacteristic(RX)
            if (!g.requestMtu(185)) enableNotifications(g)
        }

        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) payload = (mtu - 3).coerceIn(20, 182)
            enableNotifications(g)
        }

        override fun onDescriptorWrite(g: BluetoothGatt, d: BluetoothGattDescriptor, status: Int) {
            if (d.uuid == CCCD && !ready) {
                ready = true
                onReady(this@BleLink)
            }
        }

        override fun onCharacteristicWrite(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) {
            writeOk = status == BluetoothGatt.GATT_SUCCESS
            writeDone.release()
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray) {
            received(value)
        }

        @Deprecated("Android 12 ve öncesi için")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT < 33) received(c.value ?: return)
        }
    }

    @Suppress("DEPRECATION")
    private fun enableNotifications(g: BluetoothGatt) {
        val tx = g.getService(SERVICE)?.getCharacteristic(TX)
        val d = tx?.getDescriptor(CCCD)
        if (tx == null || d == null) { lost(); return }
        g.setCharacteristicNotification(tx, true)
        val value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        if (Build.VERSION.SDK_INT >= 33) g.writeDescriptor(d, value)
        else { d.value = value; g.writeDescriptor(d) }
    }

    private fun received(bytes: ByteArray) {
        synchronized(line) {
            for (b in bytes) {
                val ch = b.toInt().toChar()
                if (ch == '\n') { lineHandler?.invoke(line.toString().trimEnd('\r')); line.clear() }
                else if (line.length < 200) line.append(ch)
            }
        }
    }

    private fun lost() {
        if (closed) return
        closed = true
        writeDone.release()
        runCatching { gatt?.close() }
        onLost(this)
    }

    @Suppress("DEPRECATION")
    override fun write(data: ByteArray): Boolean {
        val g = gatt ?: return false
        val c = rx ?: return false
        if (closed || !ready) return false
        var offset = 0
        while (offset < data.size) {
            val chunk = data.copyOfRange(offset, minOf(data.size, offset + payload))
            writeDone.drainPermits()
            val started = if (Build.VERSION.SDK_INT >= 33) {
                g.writeCharacteristic(c, chunk, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothGatt.GATT_SUCCESS
            } else {
                c.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                c.value = chunk
                g.writeCharacteristic(c)
            }
            if (!started) return false
            // Bir sonraki parçayı göndermeden önce kartın onayını bekle
            if (!writeDone.tryAcquire(2, TimeUnit.SECONDS) || !writeOk || closed) return false
            offset += chunk.size
        }
        return true
    }

    override fun startReading(onLine: (String) -> Unit) {
        lineHandler = onLine
    }

    override fun close() {
        closed = true
        writeDone.release()
        runCatching { gatt?.disconnect() }
        runCatching { gatt?.close() }
    }
}
