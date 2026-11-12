package com.omerek.guitarlearn.led

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import kotlin.concurrent.thread

/** Kartla konuşan bir bağlantı (USB ya da Bluetooth). Satır tabanlı metin protokolü taşır. */
internal interface LedLink {
    /** Engelleyici yazma; başarısızsa false. */
    fun write(data: ByteArray): Boolean
    fun startReading(onLine: (String) -> Unit)
    fun close()
}

/** En küçük CDC-ACM (USB seri port) sürücüsü: veri arayüzündeki bulk uçlarla yazar/okur. */
internal class CdcPort(
    val device: UsbDevice,
    private val conn: UsbDeviceConnection,
    private val interfaces: List<UsbInterface>,
    private val out: UsbEndpoint,
    private val input: UsbEndpoint?,
) : LedLink {
    @Volatile private var open = true

    override fun write(data: ByteArray): Boolean {
        var offset = 0
        while (offset < data.size) {
            val n = minOf(out.maxPacketSize, data.size - offset)
            val chunk = data.copyOfRange(offset, offset + n)
            val sent = conn.bulkTransfer(out, chunk, n, 500)
            if (sent < 0) return false
            offset += sent
        }
        return true
    }

    override fun startReading(onLine: (String) -> Unit) {
        val ep = input ?: return
        thread(name = "led-usb-read", isDaemon = true) {
            val buf = ByteArray(ep.maxPacketSize.coerceAtLeast(64))
            val line = StringBuilder()
            while (open) {
                val n = try { conn.bulkTransfer(ep, buf, buf.size, 300) } catch (e: Exception) { -1 }
                if (n <= 0) continue
                for (i in 0 until n) {
                    val ch = buf[i].toInt().toChar()
                    if (ch == '\n') { onLine(line.toString()); line.clear() }
                    else if (line.length < 200) line.append(ch)
                }
            }
        }
    }

    override fun close() {
        open = false
        interfaces.forEach { runCatching { conn.releaseInterface(it) } }
        runCatching { conn.close() }
    }

    companion object {
        fun hasCdc(d: UsbDevice): Boolean =
            (0 until d.interfaceCount).any { d.getInterface(it).interfaceClass == UsbConstants.USB_CLASS_CDC_DATA }

        fun open(usb: UsbManager, device: UsbDevice): CdcPort? {
            val all = (0 until device.interfaceCount).map { device.getInterface(it) }
            val data = all.firstOrNull { it.interfaceClass == UsbConstants.USB_CLASS_CDC_DATA } ?: return null
            val control = all.firstOrNull { it.interfaceClass == UsbConstants.USB_CLASS_COMM }
            var out: UsbEndpoint? = null
            var input: UsbEndpoint? = null
            for (i in 0 until data.endpointCount) {
                val ep = data.getEndpoint(i)
                if (ep.type != UsbConstants.USB_ENDPOINT_XFER_BULK) continue
                if (ep.direction == UsbConstants.USB_DIR_OUT) out = ep else input = ep
            }
            if (out == null) return null
            val conn = usb.openDevice(device) ?: return null
            val claimed = listOfNotNull(control, data).filter { conn.claimInterface(it, true) }
            if (data !in claimed) { conn.close(); return null }
            control?.let {
                // 115200 8N1 (1200 baud kullanılmaz: ESP32'yi yükleme moduna sokar). Yalnızca DTR açılır;
                // RTS dokunulmaz, çünkü ESP32-S3'ün yerleşik USB'sinde RTS kartı yeniden başlatabilir.
                val lineCoding = byteArrayOf(0x00, 0xC2.toByte(), 0x01, 0x00, 0, 0, 8)
                conn.controlTransfer(0x21, 0x20, 0, it.id, lineCoding, lineCoding.size, 500)
                conn.controlTransfer(0x21, 0x22, 0x01, it.id, null, 0, 500)
            }
            return CdcPort(device, conn, claimed, out, input)
        }
    }
}
