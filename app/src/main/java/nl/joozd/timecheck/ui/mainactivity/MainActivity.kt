package nl.joozd.timecheck.ui.mainactivity

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import nl.joozd.timecheck.R
import nl.joozd.timecheck.databinding.ActivityMainBinding
import nl.joozd.timecheck.databinding.DialogAboutBinding
import nl.joozd.timecheck.databinding.DialogLookupBinding
import nl.joozd.timecheck.databinding.DialogShowTimestampBinding
import nl.joozd.timecheck.tools.FeedbackEvents.TimeStampEvents


class MainActivity : AppCompatActivity() {
    private val viewModel: MainActivityViewModel by viewModels()
    private val activity = this

    private var _toggleWordsMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityMainBinding.inflate(layoutInflater).apply{
            setSupportActionBar(toolbar)

            viewModel.timestampCode2.observe(activity){
                codeText2.text = it
            }

            viewModel.timestampTime.observe(activity){
                timeText.text = it
            }

            viewModel.showingWords.observe(activity){
                _toggleWordsMenuItem?.icon = if (it) ContextCompat.getDrawable(activity, R.drawable.numeric) else ContextCompat.getDrawable(activity, R.drawable.alphabetical)
            }

            viewModel.waitingReason.observe(activity){
                waitingReasonText.text = getString(when(it){
                    MainActivityViewModel.WAITING_FOR_SERVER -> R.string.waiting_for_server
                    MainActivityViewModel.NO_INTERNET -> R.string.no_internet
                    MainActivityViewModel.STARTING_UP -> R.string.starting_up // don't think this will be shown but might as well put it here
                    else -> R.string.unknown_error
                })
            }

            viewModel.loading.observe(activity){ loading ->
                val waiting = if (loading) View.VISIBLE else View.INVISIBLE
                val result = if (loading) View.INVISIBLE else View.VISIBLE
                    loadingCircle.visibility = waiting
                    waitingReasonText.visibility = waiting
                    codeText2.visibility = result
            }

            viewModel.feedbackEvent.observe(activity){
                when (it.getEvent()){
                    TimeStampEvents.CODE_RECEIVED ->{
                        buildTimeShowerDialog(viewModel.foundCode, viewModel.foundTime).show()
                    }
                    TimeStampEvents.SHOW_ABOUT -> {
                        buildAboutDialog(viewModel.aboutText).show()
                    }
                    TimeStampEvents.ERROR -> toast (it.getString() ?: getString(R.string.unknown_error))
                    TimeStampEvents.CODE_COPIED -> toast (R.string.code_copied)
                }
            }
            refreshButton.setOnClickListener { viewModel.refreshClicked() }

            codeText2.setOnClickListener {
                viewModel.toggleUseWords()
            }

            setContentView(root)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        _toggleWordsMenuItem = menu.findItem(R.id.menu_toggle_words)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.menu_about -> { viewModel.aboutClicked(); true}
        R.id.menu_toggle_words -> { viewModel.toggleUseWords(); true }
        R.id.menu_lookup_time -> { buildLookupDialog().show(); true }
        R.id.menu_copy -> { viewModel.copyCode(); true }
        else -> false
    }

    /**
     * Get a fresh code every time this is opened
     */
    override fun onResume() {
        super.onResume()
        viewModel.refreshIfTooOld()
    }

    @SuppressLint("InflateParams")
    private fun buildLookupDialog() = AlertDialog.Builder(this).create().apply{
        DialogLookupBinding.bind(layoutInflater.inflate(R.layout.dialog_lookup, null)).apply{
            cancelButton.setOnClickListener { dismiss() }
            okButton.setOnClickListener { viewModel.checkCode(inputField.text.toString()) }
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            setView(root)
        }
    }

    @SuppressLint("InflateParams")
    private fun buildTimeShowerDialog(code: String, timeString: String) = AlertDialog.Builder(this).create().apply{
        DialogShowTimestampBinding.bind(layoutInflater.inflate(R.layout.dialog_show_timestamp, null)).apply{
            dialogHeader.text = code
            foundTimeTextview.text = timeString
            okButton.setOnClickListener { dismiss() }
            setView(root)
        }
    }

    @SuppressLint("InflateParams")
    private fun buildAboutDialog(text: String) = AlertDialog.Builder(this).create().apply{
        DialogAboutBinding.bind(layoutInflater.inflate(R.layout.dialog_about, null)).apply {
            aboutTextview.movementMethod = LinkMovementMethod.getInstance()
            aboutTextview.text = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT)
            okButton.setOnClickListener { dismiss() }
            setView(root)
        }

    }

    private fun toast(text: CharSequence, long: Boolean = false) = Toast.makeText(this, text, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    private fun toast(textRes: Int, long: Boolean = false) = Toast.makeText(this, getString(textRes), if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()


}