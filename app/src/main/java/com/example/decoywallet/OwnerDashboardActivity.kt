package com.example.decoywallet

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.decoywallet.BuildConfig
import com.example.decoywallet.adapter.IntruderAdapter
import com.example.decoywallet.data.AlertRepository
import com.example.decoywallet.databinding.ActivityOwnerDashboardBinding
import com.example.decoywallet.model.Intruder
import com.example.decoywallet.prefs.SettingsStore
import com.google.firebase.firestore.ListenerRegistration

/** The real owner view: security status + the live list of intruder alerts. */
class OwnerDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOwnerDashboardBinding
    private var registration: ListenerRegistration? = null
    private val alerts = mutableListOf<Intruder>()
    private lateinit var adapter: IntruderAdapter
    private val settings by lazy { SettingsStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOwnerDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = IntruderAdapter(alerts) { intruder ->
            startActivity(
                Intent(this, IntruderDetailActivity::class.java)
                    .putExtra(IntruderDetailActivity.EXTRA_ID, intruder.id)
            )
        }
        binding.recyclerIntruders.layoutManager = LinearLayoutManager(this)
        binding.recyclerIntruders.adapter = adapter

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        if (intent.getBooleanExtra(EXTRA_FIRST_RUN, false)) {
            Toast.makeText(this, "MPIN set. This is your Security Center.", Toast.LENGTH_LONG).show()
        }

        val email = settings.alertEmail.ifBlank { BuildConfig.ALERT_EMAIL }
        if (email.isBlank()) {
            Toast.makeText(
                this,
                "Set Alert email in Settings — otherwise emails won't send",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onStart() {
        super.onStart()
        registration = AlertRepository.listen(settings.deviceId) { list ->
            alerts.clear()
            alerts.addAll(list)
            adapter.notifyDataSetChanged()
            render()
        }
    }

    override fun onStop() {
        super.onStop()
        registration?.remove()
        registration = null
    }

    private fun render() {
        binding.statAttempts.text = alerts.size.toString()
        binding.statAlerts.text = alerts.count { it.alertChannel.isNotBlank() }.toString()
        binding.emptyState.visibility = if (alerts.isEmpty()) View.VISIBLE else View.GONE
    }

    companion object {
        const val EXTRA_FIRST_RUN = "extra_first_run"
    }
}
