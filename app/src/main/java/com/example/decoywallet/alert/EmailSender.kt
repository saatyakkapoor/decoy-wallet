package com.example.decoywallet.alert

import android.util.Log
import com.example.decoywallet.BuildConfig
import java.io.File
import java.util.Properties
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

/** Brevo SMTP — From must be the verified sender: Decoy Wallet <decoy@paperpool.in> */
object EmailSender {

    private const val TAG = "EmailSender"
    private const val MAX_ATTACH_BYTES = 18L * 1024 * 1024

    private val fromEmail: String
        get() = BuildConfig.SMTP_FROM.ifBlank { "decoy@paperpool.in" }

    private val fromName: String
        get() = BuildConfig.SMTP_FROM_NAME.ifBlank { "Decoy Wallet" }

    fun sendIntruderAlert(
        toEmail: String,
        subject: String,
        body: String,
        attachments: List<File> = emptyList()
    ): Boolean {
        val to = toEmail.trim()
        if (to.isBlank() || !to.contains("@")) {
            Log.e(TAG, "No valid alert email configured")
            return false
        }
        val host = BuildConfig.SMTP_HOST.ifBlank { "smtp-relay.brevo.com" }
        val port = BuildConfig.SMTP_PORT.ifBlank { "587" }
        val user = BuildConfig.SMTP_USER
        val pass = BuildConfig.SMTP_PASS
        if (user.isBlank() || pass.isBlank()) {
            Log.e(TAG, "SMTP_USER / SMTP_PASS missing")
            return false
        }

        return runCatching {
            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.starttls.required", "true")
                put("mail.smtp.host", host)
                put("mail.smtp.port", port)
                put("mail.smtp.ssl.protocols", "TLSv1.2")
                put("mail.smtp.connectiontimeout", "20000")
                put("mail.smtp.timeout", "90000")
                put("mail.smtp.writetimeout", "90000")
                // Envelope / identity for Brevo verified sender
                put("mail.smtp.from", fromEmail)
            }
            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication =
                    PasswordAuthentication(user, pass)
            })

            val batches = batchAttachments(attachments)
            if (batches.isEmpty()) {
                sendOne(session, to, subject, body, emptyList())
            } else {
                batches.forEachIndexed { i, files ->
                    val sub = if (batches.size == 1) subject
                    else "$subject (part ${i + 1}/${batches.size})"
                    sendOne(session, to, sub, body, files)
                }
            }
            Log.i(TAG, "Sent OK from=$fromEmail to=$to attachments=${attachments.size}")
            true
        }.onFailure {
            Log.e(TAG, "Send failed: ${it.javaClass.simpleName}: ${it.message}", it)
        }.getOrDefault(false)
    }

    private fun sendOne(
        session: Session,
        to: String,
        subject: String,
        body: String,
        files: List<File>
    ) {
        val message = MimeMessage(session)
        message.setFrom(InternetAddress(fromEmail, fromName))
        message.setReplyTo(arrayOf(InternetAddress(fromEmail, fromName)))
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
        message.subject = subject
        message.setHeader("X-Mailer", "DecoyWallet")

        val multipart = MimeMultipart()
        val textPart = MimeBodyPart()
        textPart.setText(body, "utf-8")
        multipart.addBodyPart(textPart)

        files.forEach { file ->
            if (!file.exists() || file.length() == 0L) return@forEach
            val part = MimeBodyPart()
            part.dataHandler = DataHandler(FileDataSource(file))
            part.fileName = file.name
            multipart.addBodyPart(part)
        }
        message.setContent(multipart)
        Transport.send(message)
    }

    private fun batchAttachments(files: List<File>): List<List<File>> {
        val existing = files.filter { it.exists() && it.length() > 0 }
        if (existing.isEmpty()) return emptyList()
        val batches = mutableListOf<MutableList<File>>()
        var current = mutableListOf<File>()
        var size = 0L
        existing.forEach { f ->
            if (size + f.length() > MAX_ATTACH_BYTES && current.isNotEmpty()) {
                batches.add(current)
                current = mutableListOf()
                size = 0L
            }
            current.add(f)
            size += f.length()
        }
        if (current.isNotEmpty()) batches.add(current)
        return batches
    }
}
