package com.autobox.autoboxlauncher.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autobox.autoboxlauncher.AppTheme
import com.autobox.autoboxlauncher.MainViewModel
import com.autobox.autoboxlauncher.R
import com.autobox.autoboxlauncher.UnitSystem

@Composable
fun SettingsScreen(viewModel: MainViewModel, onExit: () -> Unit = {}) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Button(
                    onClick = onExit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB71C1C)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 0.dp),
                    modifier = Modifier.height(52.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PowerSettingsNew,
                        contentDescription = stringResource(R.string.exit_button),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.exit_button),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        }

        item {
            SettingsCard(
                icon = Icons.Rounded.Palette,
                title = stringResource(R.string.settings_theme)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CarThemeButton(
                        label = stringResource(R.string.theme_light),
                        isSelected = viewModel.theme.value == AppTheme.LIGHT,
                        modifier = Modifier.weight(1f)
                    ) { viewModel.setTheme(AppTheme.LIGHT) }
                    CarThemeButton(
                        label = stringResource(R.string.theme_dark),
                        isSelected = viewModel.theme.value == AppTheme.DARK,
                        modifier = Modifier.weight(1f)
                    ) { viewModel.setTheme(AppTheme.DARK) }
                    CarThemeButton(
                        label = stringResource(R.string.theme_auto),
                        isSelected = viewModel.theme.value == AppTheme.AUTO,
                        modifier = Modifier.weight(1f)
                    ) { viewModel.setTheme(AppTheme.AUTO) }
                }
            }
        }

        item {
            SettingsCard(
                icon = Icons.Rounded.VolumeUp,
                title = stringResource(R.string.settings_volume)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VolumeDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        modifier = Modifier.size(22.dp)
                    )
                    Slider(
                        value = viewModel.volume.value,
                        onValueChange = { viewModel.setVolume(it) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                        )
                    )
                    Icon(
                        imageVector = Icons.Rounded.VolumeUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        item {
            SettingsCard(
                icon = Icons.Rounded.BrightnessHigh,
                title = stringResource(R.string.settings_brightness)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BrightnessLow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        modifier = Modifier.size(22.dp)
                    )
                    Slider(
                        value = viewModel.brightness.value,
                        onValueChange = { viewModel.setBrightness(it) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                        )
                    )
                    Icon(
                        imageVector = Icons.Rounded.BrightnessHigh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        item {
            SettingsCard(
                icon = Icons.Rounded.Speed,
                title = stringResource(R.string.settings_units)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CarThemeButton(
                        label = stringResource(R.string.units_metric),
                        isSelected = viewModel.unitSystem.value == UnitSystem.METRIC,
                        modifier = Modifier.weight(1f)
                    ) { viewModel.setUnitSystem(UnitSystem.METRIC) }
                    CarThemeButton(
                        label = stringResource(R.string.units_imperial),
                        isSelected = viewModel.unitSystem.value == UnitSystem.IMPERIAL,
                        modifier = Modifier.weight(1f)
                    ) { viewModel.setUnitSystem(UnitSystem.IMPERIAL) }
                }
            }
        }

        item {
            SettingsCard(
                icon = Icons.Rounded.LibraryMusic,
                title = stringResource(R.string.settings_media_app)
            ) {
                MediaAppDropdown(viewModel = viewModel)
            }
        }

        item {
            SettingsCard(
                icon = Icons.Rounded.CameraAlt,
                title = stringResource(R.string.settings_sign_recognition)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.settings_sign_recognition_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                    Switch(
                        checked = viewModel.signRecognitionEnabled.value,
                        onCheckedChange = { viewModel.setSignRecognitionEnabled(it) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaAppDropdown(viewModel: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val apps = viewModel.installedMediaApps.value
    val selectedApp = viewModel.selectedMediaApp.value
    val notSelectedLabel = stringResource(R.string.media_app_not_selected)
    val noAppsLabel = stringResource(R.string.media_app_no_apps)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedApp?.name ?: notSelectedLabel,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (apps.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = noAppsLabel,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    },
                    onClick = { expanded = false },
                    enabled = false
                )
            } else {
                apps.forEach { app ->
                    val isSelected = app.packageName == selectedApp?.packageName
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = app.name,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            viewModel.setMediaApp(app)
                            expanded = false
                        },
                        trailingIcon = if (isSelected) {
                            { Icon(Icons.Rounded.Check, contentDescription = null) }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
            content()
        }
    }
}

@Composable
fun CarThemeButton(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                          else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
