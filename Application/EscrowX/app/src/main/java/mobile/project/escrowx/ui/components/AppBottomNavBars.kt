package mobile.project.escrowx.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class BuyerNavItem(val index: Int) {
    Home(0),
    Transactions(1),
    Profile(2)
}

enum class SellerNavItem(val index: Int) {
    Home(0),
    Transactions(1),
    Profile(2)
}

@Composable
fun BuyerNavBar(
    selectedIndex: Int,
    onItemSelected: (BuyerNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.height(75.dp),
        containerColor = Color.White
    ) {
        val items = listOf(
            BuyerNavItem.Home to ("Home" to Icons.Default.Home),
            BuyerNavItem.Transactions to ("Transactions" to Icons.Default.AccountBalanceWallet),
            BuyerNavItem.Profile to ("Profile" to Icons.Default.Person)
        )

        items.forEach { (item, ui) ->
            val (label, icon) = ui
            val selected = selectedIndex == item.index
            NavigationBarItem(
                selected = selected,
                onClick = { onItemSelected(item) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF00236F),
                    selectedTextColor = Color(0xFF00236F),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color(0xFFE7EEFE)
                )
            )
        }
    }
}

@Composable
fun SellerNavBar(
    selectedIndex: Int,
    onItemSelected: (SellerNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.height(80.dp),
        containerColor = Color(0xFFF9F9FF),
        tonalElevation = 0.dp
    ) {
        val items = listOf(
            SellerNavItem.Home to ("Home" to Icons.Default.Home),
            SellerNavItem.Transactions to ("Transactions" to Icons.Default.AccountBalanceWallet),
            SellerNavItem.Profile to ("Profile" to Icons.Default.Person)
        )

        items.forEach { (item, ui) ->
            val (label, icon) = ui
            val selected = selectedIndex == item.index
            NavigationBarItem(
                selected = selected,
                onClick = { onItemSelected(item) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF00236F),
                    selectedTextColor = Color(0xFF00236F),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color(0xFFE7EEFE)
                )
            )
        }
    }
}
