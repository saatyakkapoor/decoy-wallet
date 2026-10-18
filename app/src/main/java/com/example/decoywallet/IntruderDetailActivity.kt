package com.example.decoywallet

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.decoywallet.data.AlertRepository
import com.example.decoywallet.databinding.ActivityIntruderDetailBinding
import com.example.decoywallet.model.Intruder
import kotlinx.coroutines.launch

class IntruderDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIntruderDetailBinding
    private var current: Intruder? = null

    companion object {
        const val EXTRA_ID = "intruder_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntruderDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnMap.setOnClickListener { openMap() }

        val id = intent.getStringExtra(EXTRA_ID)
        if (id.isNullOrEmpty()) {
            finish()
            return
        }

        lifecycleScope.launch {
            val item = AlertRepository.getById(id)
            if (item == null) {
                finish()
                return@launch
            }
            current = item
            bind(item)
        }
    }

    private fun bind(item: Intruder) {
        bindRow(binding.rowTime.root, R.drawable.ic_time, getString(R.string.timestamp), item.timestamp)
        bindRow(
            binding.rowLocation.root, R.drawable.ic_location,
            getString(R.string.location), "${item.location}\n${item.coordinates}"
        )
        bindRow(binding.rowDevice.root, R.drawable.ic_phone_android, getString(R.string.device_info), item.device)
        bindRow(
            binding.rowAttempts.root, R.drawable.ic_warning,
            getString(R.string.failed_attempts), item.failedAttempts.toString()
        )
        bindRow(binding.rowChannel.root, R.drawable.ic_email, getString(R.string.alert_sent_via), item.alertChannel)

        val photo = item.photoBase64
        if (!photo.isNullOrEmpty()) {
            val bytes = Base64.decode(photo, Base64.NO_WRAP)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) {
                binding.snapshotImage.setImageBitmap(bitmap)
                binding.snapshotImage.visibility = View.VISIBLE
            } else {
                binding.snapshotImage.visibility = View.GONE
                binding.snapshotHint.text = getString(R.string.snapshot_unavailable)
            }
        } else {
            binding.snapshotImage.visibility = View.GONE
            binding.snapshotHint.text = getString(R.string.snapshot_unavailable)
        }
    }

    private fun openMap() {
        val item = current
        if (item?.latitude == null || item.longitude == null) {
            Toast.makeText(this, "No location for this alert", Toast.LENGTH_SHORT).show()
            return
        }
        val label = Uri.encode(item.location.ifBlank { "Intruder location" })
        val geo = Uri.parse("geo:${item.latitude},${item.longitude}?q=${item.latitude},${item.longitude}($label)")
        val intent = Intent(Intent.ACTION_VIEW, geo)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=${item.latitude},${item.longitude}")
                )
            )
        }
    }

    private fun bindRow(row: View, icon: Int, label: String, value: String) {
        row.findViewById<ImageView>(R.id.detailIcon).setImageResource(icon)
        row.findViewById<TextView>(R.id.detailLabel).text = label
        row.findViewById<TextView>(R.id.detailValue).text = value
    }
}
