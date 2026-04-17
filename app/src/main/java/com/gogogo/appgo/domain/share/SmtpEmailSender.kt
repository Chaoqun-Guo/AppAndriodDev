package com.gogogo.appgo.domain.share

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import javax.net.ssl.SSLSocketFactory

object SmtpEmailSender {
    fun send(
        host: String,
        port: Int,
        useTls: Boolean,
        username: String,
        password: String,
        fromEmail: String,
        toEmails: List<String>,
        subject: String,
        body: String,
    ): Result<Unit> = runCatching {
        val socket: Socket = if (useTls) {
            (SSLSocketFactory.getDefault() as SSLSocketFactory).createSocket(host, port).apply { soTimeout = 15_000 }
        } else {
            Socket(host, port).apply { soTimeout = 15_000 }
        }
        var reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
        var writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))

        fun expect(code: Int): String {
            val line = readResponse(reader)
            val actual = line.take(3).toIntOrNull() ?: -1
            if (actual != code) error("SMTP $actual: $line")
            return line
        }

        fun cmd(command: String) {
            writer.write(command)
            writer.write("\r\n")
            writer.flush()
        }

        expect(220)
        cmd("EHLO appgo")
        expect(250)

        cmd("AUTH LOGIN")
        expect(334)
        cmd(Base64.getEncoder().encodeToString(username.toByteArray(StandardCharsets.UTF_8)))
        expect(334)
        cmd(Base64.getEncoder().encodeToString(password.toByteArray(StandardCharsets.UTF_8)))
        expect(235)

        cmd("MAIL FROM:<$fromEmail>")
        expect(250)
        toEmails.forEach { email ->
            cmd("RCPT TO:<$email>")
            val line = readResponse(reader)
            val status = line.take(3).toIntOrNull() ?: -1
            if (status != 250 && status != 251) error("收件人拒绝 $email: $line")
        }

        cmd("DATA")
        expect(354)

        val nowText = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).format(Date())
        val safeBody = body.replace("\r\n", "\n").replace("\r", "\n").lines().joinToString("\r\n") { line ->
            if (line.startsWith(".")) ".$line" else line
        }
        writer.write("Date: $nowText\r\n")
        writer.write("From: <$fromEmail>\r\n")
        writer.write("To: ${toEmails.joinToString(",")}\r\n")
        writer.write("Subject: $subject\r\n")
        writer.write("MIME-Version: 1.0\r\n")
        writer.write("Content-Type: text/plain; charset=UTF-8\r\n")
        writer.write("Content-Transfer-Encoding: 8bit\r\n")
        writer.write("\r\n")
        writer.write(safeBody)
        writer.write("\r\n.\r\n")
        writer.flush()
        expect(250)
        cmd("QUIT")
        readResponse(reader)
        socket.close()
    }

    private fun readResponse(reader: BufferedReader): String {
        var line = reader.readLine() ?: error("SMTP 连接中断")
        val code = line.take(3)
        while (line.length >= 4 && line.startsWith(code) && line[3] == '-') {
            line = reader.readLine() ?: line
        }
        return line
    }
}
