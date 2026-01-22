package ro.pub.cs.systems.eim.practicaltest02v8

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import android.util.Log

class ServerThread(private val port: Int) : Thread() {

    override fun run() {
        try {
            val serverSocket = ServerSocket(port)
            Log.d("SERVER", "Server started on port $port")
            while (!isInterrupted) {
                val socket = serverSocket.accept()
                CommunicationThread(socket).start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("SERVER", "Server error: ${e.message}")
        }
    }
}

class CommunicationThread(private val socket: Socket) : Thread() {

    override fun run() {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)

            val url = reader.readLine()
            Log.d("SERVER", "Received URL: $url")

            if (url == null || url.isEmpty()) {
                writer.println("ERROR: empty URL")
                socket.close()
                return
            }

            // LOGICA FIREWALL
            if (url.contains("bad")) {
                writer.println("URL blocked by firewall")

                Log.d("SERVER","URL blocked by firewall")
            } else {
                Log.d("SERVER", "URL allowed, fetching content")
                try {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"

                    val body = connection.inputStream
                        .bufferedReader()
                        .use { it.readText() }

                    Log.d("SERVER", "Content fetched successfully")
                    writer.println(body)
                } catch (e: Exception) {
                    writer.println("ERROR: cannot access URL")
                    Log.e("SERVER", "Error accessing URL: ${e.message}")
                }
            }

            socket.close()

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("SERVER", "Communication error: ${e.message}")
        }
    }
}

class ClientThread(
    private val address: String,
    private val port: Int,
    private val url: String,
    private val resultView: TextView
) : Thread() {

    override fun run() {
        try {
            val socket = Socket(address, port)
            val writer = PrintWriter(socket.getOutputStream(), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            writer.println(url)

            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line).append("\n")
            }

            resultView.post {
                resultView.text = response.toString()
            }

            socket.close()

        } catch (e: Exception) {
            e.printStackTrace()
            resultView.post {
                resultView.text = "Client error"
            }
        }
    }
}



class PracticalTest02v8MainActivity : AppCompatActivity() {

    private lateinit var serverPort: EditText
    private lateinit var startServerButton: Button

    private lateinit var clientAddress: EditText
    private lateinit var clientPort: EditText
    private lateinit var inputUrl: EditText
    private lateinit var sendButton: Button

    private lateinit var resultView: TextView

    private var serverThread: ServerThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_practical_text02v8_main)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
        serverPort = findViewById(R.id.port_server)
        startServerButton = findViewById(R.id.connectButton)

        clientAddress = findViewById(R.id.adresa_client)
        clientPort = findViewById(R.id.port_client)
        inputUrl = findViewById(R.id.input)
        sendButton = findViewById(R.id.send)
        resultView = findViewById(R.id.result)

        /* START SERVER */
        startServerButton.setOnClickListener {
            val portStr = serverPort.text.toString()
            if (portStr.isEmpty()) {
                Toast.makeText(this, "Introduceti portul serverului!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val port = portStr.toInt()
            serverThread = ServerThread(port)
            serverThread?.start()

            //Toast.makeText(this, "Server pornit pe portul $port", Toast.LENGTH_SHORT).show()

        }

        /* SEND REQUEST */
        sendButton.setOnClickListener {
            val address = clientAddress.text.toString()
            val portStr = clientPort.text.toString()
            val url = inputUrl.text.toString()

            if (address.isEmpty() || portStr.isEmpty() || url.isEmpty()) {
                Toast.makeText(this, "Completeaza toate campurile!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val port = portStr.toInt()
            ClientThread(address, port, url, resultView).start()
        }



    }
}