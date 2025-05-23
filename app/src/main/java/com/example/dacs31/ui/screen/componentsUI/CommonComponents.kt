package com.example.dacs31.ui.screen.componentsUI

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.dacs31.R
import com.example.dacs31.data.AuthRepository
import com.example.dacs31.data.User
import kotlinx.coroutines.launch


@Composable
fun TopControlBar(
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 32.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* Trong suốt với sự kiện chạm ở khu vực không có nút */ }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = Color(255, 241, 177),
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

//            Row(
//                horizontalArrangement = Arrangement.spacedBy(18.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                IconButton(
//                    onClick = { /* Xử lý tìm kiếm */ },
//                    modifier = Modifier
//                        .size(32.dp)
//                        .background(
//                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
//                            shape = RoundedCornerShape(8.dp)
//                        )
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.Search,
//                        contentDescription = "Search",
//                        tint = Color.Black,
//                        modifier = Modifier.size(20.dp)
//                    )
//                }

//                IconButton(
//                    onClick = { /* Xử lý thông báo */ },
//                    modifier = Modifier
//                        .size(32.dp)
//                        .background(
//                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
//                            shape = RoundedCornerShape(8.dp)
//                        )
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.Notifications,
//                        contentDescription = "Notifications",
//                        tint = Color.Black,
//                        modifier = Modifier.size(20.dp)
//                    )
//                }
//            }
        }
    }
}



@Composable
fun BottomControlBar(
    navController: NavController,
    showConnectButton: Boolean = false,
    isConnected: Boolean = false,
    onConnectClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* Trong suốt với sự kiện chạm ở khu vực không có nút */ }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Nút Connect/Disconnect (chỉ hiển thị nếu showConnectButton = true)
        if (showConnectButton) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = onConnectClick,
                    modifier = Modifier
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) Color.Red else Color.Black
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Power,
                            contentDescription = "Connect",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isConnected) "Disconnect" else "Connect",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        BottomNavigationBar(
            navController = navController,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        )
    }
}

@Composable
fun BottomNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        "Home" to Icons.Default.Home,
        "Favourite" to Icons.Default.FavoriteBorder,
        "Wallet" to null,
        "Offer" to Icons.Default.LocalOffer,
        "Profile" to Icons.Default.Person
    )

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* Trong suốt với sự kiện chạm ở khu vực không có nút */ }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom
        ) {
            items.forEach { (title, icon) ->
                val route = title.lowercase()
                val isSelected = currentRoute == route

                if (title == "Wallet") {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .offset(y = 0.dp)
                            .clickable {
                                if (currentRoute != route) {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_wallet_hexagon),
                            contentDescription = title,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(110.dp)
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            if (currentRoute != route) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = icon!!,
                            contentDescription = title,
                            tint = if (isSelected) Color(0xFFFFB800) else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = title,
                            color = if (isSelected) Color(0xFFFFB800) else Color.Gray,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}