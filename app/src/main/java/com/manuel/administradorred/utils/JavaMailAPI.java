package com.manuel.administradorred.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.manuel.administradorred.R;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class JavaMailAPI extends AsyncTask<Void, Integer, Void> {
    @SuppressLint("StaticFieldLeak")
    private final Context context;
    private final String email;
    private final String subject;
    private final String message;
    @SuppressLint("StaticFieldLeak")
    private final ProgressBar progressBar;
    private final AlertDialog alertDialog;

    public JavaMailAPI(Context context, String email, String subject, String message, ProgressBar progressBar, AlertDialog alertDialog) {
        this.context = context;
        this.email = email;
        this.subject = subject;
        this.message = message;
        this.progressBar = progressBar;
        this.alertDialog = alertDialog;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        progressBar.setVisibility(View.GONE);
        alertDialog.dismiss();
        Toast.makeText(context, context.getString(R.string.email_sent_successfully), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCancelled(Void unused) {
        super.onCancelled(unused);
        alertDialog.dismiss();
        Toast.makeText(context, context.getString(R.string.error_sending_email), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected Void doInBackground(Void... params) {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.socketFactory.port", "465");
        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.port", "465");
        Session session = Session.getDefaultInstance(properties, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(Constants.EMAIL, Constants.PASSWORD);
            }
        });
        try {
            MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress(null, email));
            mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(Constants.EMAIL));
            mimeMessage.setSubject(subject);
            mimeMessage.setText(message);
            Transport.send(mimeMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}