package uk.co.scrying

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import uk.co.scrying.ui.ScryingApp

class MainActivity : ComponentActivity() {
    private val viewModel: ScryingViewModel by viewModels { ScryingViewModel.factory(application) }
    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result -> viewModel.onPermissionsResult(result, result.any { (permission, granted) -> !granted && !shouldShowRequestPermissionRationale(permission) }) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MapLibre.getInstance(this)
        lifecycleScope.launch { viewModel.permissionRequests.collect { request -> if (request.openSettings) startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) else permissionRequest.launch(request.permissions.toTypedArray()) } }
        lifecycleScope.launch { viewModel.shareRequests.collect { text -> startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Share environmental reading")) } }
        setContent { ScryingApp(viewModel) }
    }
    override fun onDestroy() { viewModel.stopAll(); super.onDestroy() }
}
