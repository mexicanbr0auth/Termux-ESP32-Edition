package dev.termux.esp32bridge;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final String ACTION_USB_PERMISSION = "dev.termux.esp32bridge.USB_PERMISSION";
    private static final int TCP_PORT = 3333;
    private final ExecutorService workers = Executors.newCachedThreadPool();
    private UsbManager usbManager;
    private UsbSerialPort serialPort;
    private UsbDeviceConnection usbConnection;
    private SerialInputOutputManager ioManager;
    private ServerSocket serverSocket;
    private volatile Socket client;
    private volatile boolean running;
    private TextView status;

    private final BroadcastReceiver permissionReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!ACTION_USB_PERMISSION.equals(intent.getAction())) return;
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) && device != null) {
                openDevice(device);
            } else {
                log("USB permission denied. Tap Connect again and approve Android's dialog.");
            }
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 24, 24, 24);
        TextView title = new TextView(this);
        title.setText("Termux ESP32 USB Bridge");
        title.setTextSize(22);
        layout.addView(title);
        TextView help = new TextView(this);
        help.setText("Connect the ESP32 via OTG. This app requests USB permission and exposes a localhost TCP serial stream at 127.0.0.1:3333 (115200 baud, 8N1). For flashing, manually enter the ESP32-C3 bootloader: hold BOOT, tap RESET, release BOOT.");
        layout.addView(help);
        Button connect = new Button(this);
        connect.setText("Find and connect USB serial");
        connect.setOnClickListener(v -> discover());
        layout.addView(connect);
        Button stop = new Button(this);
        stop.setText("Stop bridge");
        stop.setOnClickListener(v -> stopBridge("Bridge stopped by user."));
        layout.addView(stop);
        status = new TextView(this);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(status);
        layout.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(layout);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(permissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(permissionReceiver, filter);
        log("Ready. Connect ESP32 by OTG, then tap the connect button.");
    }

    private void discover() {
        List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (drivers.isEmpty()) {
            log("No supported USB-serial device found. Check OTG, data cable, board power and USB host support.");
            return;
        }
        UsbSerialDriver driver = drivers.get(0);
        UsbDevice device = driver.getDevice();
        log("Found USB VID=0x" + Integer.toHexString(device.getVendorId()) + " PID=0x"
            + Integer.toHexString(device.getProductId()) + ", ports=" + driver.getPorts().size());
        if (usbManager.hasPermission(device)) openDevice(device);
        else {
            Intent intent = new Intent(ACTION_USB_PERMISSION).setPackage(getPackageName());
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            usbManager.requestPermission(device, PendingIntent.getBroadcast(this, 0, intent, flags));
            log("Waiting for Android USB permission...");
        }
    }

    private void openDevice(UsbDevice device) {
        try {
            UsbSerialDriver selected = null;
            for (UsbSerialDriver driver : UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)) {
                if (driver.getDevice().getDeviceId() == device.getDeviceId()) { selected = driver; break; }
            }
            if (selected == null) throw new IOException("USB serial driver no longer available.");
            usbConnection = usbManager.openDevice(device);
            if (usbConnection == null) throw new IOException("Android could not open USB device.");
            serialPort = selected.getPorts().get(0);
            serialPort.open(usbConnection);
            serialPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            ioManager = new SerialInputOutputManager(serialPort, new SerialInputOutputManager.Listener() {
                @Override public void onNewData(byte[] data) {
                    Socket active = client;
                    if (active == null || active.isClosed()) return;
                    try {
                        OutputStream out = active.getOutputStream();
                        out.write(data);
                        out.flush();
                    } catch (IOException e) { log("TCP client disconnected."); }
                }
                @Override public void onRunError(Exception e) { log("USB serial error: " + e.getMessage()); }
            });
            ioManager.start();
            running = true;
            serverSocket = new ServerSocket(TCP_PORT, 1, InetAddress.getByName("127.0.0.1"));
            log("Connected at 115200 8N1. Listening on 127.0.0.1:" + TCP_PORT);
            workers.submit(this::acceptClients);
        } catch (Exception e) {
            log("Connection failed: " + e.getMessage());
            stopBridge("Bridge closed after connection failure.");
        }
    }

    private void acceptClients() {
        while (running) {
            try {
                Socket next = serverSocket.accept();
                Socket old = client;
                if (old != null) try { old.close(); } catch (IOException ignored) { }
                client = next;
                log("Termux client connected on localhost.");
                workers.submit(() -> pumpToUsb(next));
            } catch (IOException e) {
                if (running) log("TCP server error: " + e.getMessage());
                return;
            }
        }
    }

    private void pumpToUsb(Socket socket) {
        byte[] buffer = new byte[4096];
        try {
            InputStream in = socket.getInputStream();
            int count;
            while (running && (count = in.read(buffer)) >= 0) {
                if (count > 0 && serialPort != null) serialPort.write(java.util.Arrays.copyOf(buffer, count), 2000);
            }
        } catch (Exception e) {
            if (running) log("Termux stream closed: " + e.getMessage());
        } finally {
            if (client == socket) client = null;
            try { socket.close(); } catch (IOException ignored) { }
        }
    }

    private void stopBridge(String message) {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) { }
        try { if (client != null) client.close(); } catch (IOException ignored) { }
        try { if (ioManager != null) ioManager.stop(); } catch (Exception ignored) { }
        try { if (serialPort != null) serialPort.close(); } catch (Exception ignored) { }
        try { if (usbConnection != null) usbConnection.close(); } catch (Exception ignored) { }
        serverSocket = null; client = null; ioManager = null; serialPort = null; usbConnection = null;
        log(message);
    }

    private void log(String message) {
        runOnUiThread(() -> status.append("\n" + message));
    }

    @Override protected void onDestroy() {
        stopBridge("Activity closed.");
        try { unregisterReceiver(permissionReceiver); } catch (Exception ignored) { }
        workers.shutdownNow();
        super.onDestroy();
    }
}
