package com.manuel.administradorred.about

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import androidx.appcompat.app.AppCompatActivity
import com.manuel.administradorred.R
import com.manuel.administradorred.databinding.ActivityAboutBinding
import com.manuel.administradorred.package_service.MainActivity

class AboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            binding.tvPostComments.text = Html.fromHtml(
                "<u>${getString(R.string.post_comments)}</u>",
                Html.FROM_HTML_MODE_LEGACY
            )
        }
        binding.tvPostComments.setOnClickListener {
            SendEmailFragment().show(
                supportFragmentManager,
                SendEmailFragment::class.java.simpleName
            )
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}