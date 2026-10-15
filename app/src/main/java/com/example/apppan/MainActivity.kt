
package com.example.apppan

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {

    private val servidor = "http://127.0.0.1:8080"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val txtTelefono = findViewById<EditText>(R.id.txtTelefono)
        val btnConsultar = findViewById<Button>(R.id.btnConsultar)
        val lblResultado = findViewById<TextView>(R.id.lblResultado)

        btnConsultar.setOnClickListener {

            val telefono = txtTelefono.text.toString().trim()

            if (telefono.length != 8 || !telefono.all { it.isDigit() }) {

                Toast.makeText(
                    this,
                    "Ingrese un telefono valido de 8 digitos",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                lblResultado.text = "Consultando informacion..."
                btnConsultar.isEnabled = false

                Thread {

                    try {

                        val numero = URLEncoder.encode(telefono, "UTF-8")

                        val urlPuntos =
                            "$servidor/clientes/puntos?telefono=$numero"

                        val urlCanjes =
                            "$servidor/clientes/canjes?telefono=$numero"

                        val respuestaPuntos = consultarApi(urlPuntos)
                        val respuestaCanjes = consultarApi(urlCanjes)

                        val datosPuntos = JSONObject(respuestaPuntos)
                        val datosCanjes = JSONObject(respuestaCanjes)

                        val nombre = datosPuntos.optString("nombre")
                        val puntos = datosPuntos.optInt("puntosDisponibles")

                        val historialPuntos =
                            datosPuntos.optJSONArray("historialPuntos")

                        val historialCanjes =
                            datosCanjes.optJSONArray("historialCanjes")

                        if (nombre.isBlank()) {
                            throw Exception("No se encontro el cliente")
                        }

                        val resultado = StringBuilder()

                        resultado.append("CLIENTE: $nombre\n")
                        resultado.append("TELEFONO: $telefono\n")
                        resultado.append("PUNTOS DISPONIBLES: $puntos\n\n")

                        resultado.append("HISTORIAL DE PUNTOS\n")
                        resultado.append("-----------------------------\n")

                        if (historialPuntos != null &&
                            historialPuntos.length() > 0) {

                            for (i in 0 until historialPuntos.length()) {

                                val movimiento =
                                    historialPuntos.getJSONObject(i)

                                val fecha = movimiento.optString("fecha")
                                val tipo = movimiento.optString("tipo")
                                val cantidad = movimiento.optInt("puntos")

                                resultado.append(
                                    "$fecha\n$tipo: $cantidad puntos\n\n"
                                )
                            }

                        } else {

                            resultado.append("Sin movimientos\n\n")
                        }

                        resultado.append("HISTORIAL DE CANJES\n")
                        resultado.append("-----------------------------\n")

                        if (historialCanjes != null &&
                            historialCanjes.length() > 0) {

                            for (i in 0 until historialCanjes.length()) {

                                val canje = historialCanjes.getJSONObject(i)

                                val fecha = canje.optString("fecha")
                                val recompensa =
                                    canje.optString("recompensa")

                                val utilizados =
                                    canje.optInt("puntosUtilizados")

                                resultado.append(
                                    "$fecha\n$recompensa\n" +
                                            "Puntos utilizados: $utilizados\n\n"
                                )
                            }

                        } else {

                            resultado.append("Sin canjes realizados")
                        }

                        runOnUiThread {
                            lblResultado.text = resultado.toString()
                            btnConsultar.isEnabled = true
                        }

                    } catch (e: Exception) {

                        runOnUiThread {

                            lblResultado.text =
                                "No se pudo consultar la informacion.\n" +
                                        "${e.message}"

                            btnConsultar.isEnabled = true
                        }
                    }

                }.start()
            }
        }
    }

    private fun consultarApi(direccion: String): String {

        val url = URL(direccion)

        val conexion =
            url.openConnection() as HttpURLConnection

        try {

            conexion.requestMethod = "GET"
            conexion.connectTimeout = 10000
            conexion.readTimeout = 10000

            val codigo = conexion.responseCode

            if (codigo != HttpURLConnection.HTTP_OK) {

                throw Exception("Error HTTP: $codigo")
            }

            return conexion.inputStream.bufferedReader().use {
                it.readText()
            }

        } finally {

            conexion.disconnect()
        }
    }
}
