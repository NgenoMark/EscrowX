package mobile.project.escrowx.dash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mobile.project.escrowx.auth.LoginActivity
import mobile.project.escrowx.auth.SessionManager
import mobile.project.escrowx.ui.theme.EscrowXTheme
import mobile.project.escrowx.ui.theme.ThemePreferenceManager

class RiderDashboardActvity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent {
			EscrowXTheme(
				darkTheme = ThemePreferenceManager.isDarkModeEnabled(this),
				dynamicColor = false
			) {
				RiderDashboardScreen(
					onLogout = {
						SessionManager(this).clearSession()
						startActivity(Intent(this, LoginActivity::class.java).apply {
							flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
						})
						finishAffinity()
					}
				)
			}
		}
	}
}

@Composable
private fun RiderDashboardScreen(onLogout: () -> Unit) {
	val colorScheme = MaterialTheme.colorScheme
	Surface(
		modifier = Modifier.fillMaxSize(),
		color = colorScheme.background
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(24.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center
		) {
			Icon(
				imageVector = Icons.Default.DirectionsBike,
				contentDescription = null,
				tint = colorScheme.primary
			)
			Text(
				text = "Rider Dashboard",
				style = MaterialTheme.typography.headlineSmall,
				color = colorScheme.onBackground,
				modifier = Modifier.padding(top = 12.dp)
			)
			Text(
				text = "Login successful. Rider workspace is ready.",
				fontSize = 14.sp,
				color = colorScheme.onSurfaceVariant,
				modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
			)

			Button(onClick = onLogout) {
				Row(verticalAlignment = Alignment.CenterVertically) {
					Icon(Icons.Default.Logout, contentDescription = null)
					Text(text = " Logout")
				}
			}
		}
	}
}
