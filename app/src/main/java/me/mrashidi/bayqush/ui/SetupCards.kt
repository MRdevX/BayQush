package me.mrashidi.bayqush.ui

import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.mrashidi.bayqush.R
import me.mrashidi.bayqush.SenderRow
import me.mrashidi.bayqush.sameSender

private val CardShape = RoundedCornerShape(16.dp)
private val PressEase = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

@Composable
internal fun SetupIntro() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.setup_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.setup_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun SmsPermissionBanner(onGrant: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Sms,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = stringResource(R.string.sms_needed),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            PrimaryButton(onClick = onGrant, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.grant_sms))
            }
        }
    }
}

@Composable
internal fun StatusCard(
    batteryOk: Boolean,
    chatId: String,
    forwardAll: Boolean,
    selectedCount: Int,
    onSendTest: () -> Unit,
) {
    val title = if (batteryOk) {
        stringResource(R.string.status_forwarding_on)
    } else {
        stringResource(R.string.status_background_restricted)
    }
    val filterLabel = if (forwardAll) {
        stringResource(R.string.status_everyone)
    } else if (selectedCount == 1) {
        stringResource(R.string.status_selected_one)
    } else {
        stringResource(R.string.status_selected_count, selectedCount)
    }
    val subtitle = if (batteryOk) {
        "${truncateChatId(chatId)} · $filterLabel"
    } else {
        stringResource(R.string.status_background_restricted_sub)
    }
    val icon = if (batteryOk) Icons.Filled.CheckCircle else Icons.Filled.BatteryAlert
    val iconTint = if (batteryOk) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            OutlinedButton(
                onClick = onSendTest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                    Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.send_test))
            }
        }
    }
}

@Composable
internal fun TelegramCard(
    editing: Boolean,
    token: String,
    chatId: String,
    tokenError: Boolean,
    chatIdError: Boolean,
    savePrimary: Boolean,
    onTokenChange: (String) -> Unit,
    onChatIdChange: (String) -> Unit,
    onSave: () -> Unit,
    onEdit: () -> Unit,
    onHelp: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Column {
        SectionLabel(stringResource(R.string.section_telegram))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (editing) {
                    OutlinedTextField(
                        value = token,
                        onValueChange = onTokenChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.bot_token)) },
                        supportingText = {
                            Text(
                                if (tokenError) {
                                    stringResource(R.string.error_token_required)
                                } else {
                                    stringResource(R.string.bot_token_support)
                                },
                            )
                        },
                        isError = tokenError,
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next,
                        ),
                    )
                    OutlinedTextField(
                        value = chatId,
                        onValueChange = onChatIdChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.chat_id)) },
                        supportingText = {
                            Text(
                                if (chatIdError) {
                                    stringResource(R.string.error_chat_id_required)
                                } else {
                                    stringResource(R.string.chat_id_support)
                                },
                            )
                        },
                        isError = chatIdError,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                onSave()
                            },
                        ),
                    )
                    TextButton(onClick = onHelp) {
                        Text(stringResource(R.string.how_to_get_these))
                    }
                    if (savePrimary) {
                        PrimaryButton(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.save))
                        }
                    } else {
                        OutlinedButton(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.save))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.telegram_connected),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(R.string.telegram_connected_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = onEdit) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(stringResource(R.string.edit))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ForwardingCard(
    forwardAll: Boolean,
    selectedCount: Int,
    readOk: Boolean,
    newSender: String,
    senderRows: List<SenderRow>,
    selectedSenders: Set<String>,
    onNewSenderChange: (String) -> Unit,
    onAddSender: () -> Unit,
    onForwardAllChange: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onToggleSender: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.section_forwarding),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = onRefresh, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = stringResource(R.string.refresh_senders_cd),
                )
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = forwardAll,
                            onClick = { onForwardAllChange(true) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        ) {
                            Text(stringResource(R.string.forward_everyone))
                        }
                        SegmentedButton(
                            selected = !forwardAll,
                            onClick = { onForwardAllChange(false) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        ) {
                            Text(stringResource(R.string.forward_selected))
                        }
                    }
                    if (!forwardAll) {
                        Text(
                            text = stringResource(R.string.selected_count, selectedCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = newSender,
                                onValueChange = onNewSenderChange,
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(R.string.add_number)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Done,
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        onAddSender()
                                        focusManager.clearFocus()
                                    },
                                ),
                            )
                            IconButton(onClick = onAddSender) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = stringResource(R.string.add_number_cd),
                                )
                            }
                        }
                        if (!readOk) {
                            Text(
                                text = stringResource(R.string.grant_sms_for_senders),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else if (senderRows.isEmpty()) {
                            Text(
                                text = stringResource(R.string.empty_inbox),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (!forwardAll) {
                    senderRows.forEachIndexed { index, row ->
                        val checked = selectedSenders.any { sameSender(it, row.address) }
                        if (index == 0) {
                            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                        }
                        SenderRowItem(
                            row = row,
                            checked = checked,
                            onToggle = { onToggleSender(row.address) },
                        )
                        if (index != senderRows.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SenderRowItem(
    row: SenderRow,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Text(text = row.address, style = MaterialTheme.typography.titleMedium)
            if (row.count > 0) {
                Text(
                    text = stringResource(
                        R.string.sender_inbox_line,
                        row.count,
                        row.lastBody,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
internal fun BackgroundCard(
    batteryOk: Boolean,
    allowPrimary: Boolean,
    onAllowBackground: () -> Unit,
) {
    val samsung = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
    Column {
        SectionLabel(stringResource(R.string.section_background))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (batteryOk) {
                            Icons.Filled.CheckCircle
                        } else {
                            Icons.Filled.BatteryAlert
                        },
                        contentDescription = null,
                        tint = if (batteryOk) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.size(24.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(
                                if (batteryOk) {
                                    R.string.background_allowed
                                } else {
                                    R.string.background_restricted
                                },
                            ),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(
                                if (batteryOk) {
                                    R.string.background_allowed_sub
                                } else {
                                    R.string.background_restricted_sub
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (batteryOk) {
                    if (samsung) {
                        Text(
                            text = stringResource(R.string.samsung_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onAllowBackground) {
                        Text(stringResource(R.string.background_settings))
                    }
                } else if (allowPrimary) {
                    PrimaryButton(onClick = onAllowBackground, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.allow_background))
                    }
                } else {
                    OutlinedButton(onClick = onAllowBackground, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.allow_background))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TelegramHelpSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = stringResource(R.string.help_title),
                style = MaterialTheme.typography.titleLarge,
            )
            HelpStep(1, R.string.help_step_1_title, R.string.help_step_1_body)
            HorizontalDivider()
            HelpStep(2, R.string.help_step_2_title, R.string.help_step_2_body)
            HorizontalDivider()
            HelpStep(3, R.string.help_step_3_title, R.string.help_step_3_body)
        }
    }
}

@Composable
private fun HelpStep(number: Int, title: Int, body: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = rememberReduceMotion()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) 0.98f else 1f,
        animationSpec = tween(durationMillis = 160, easing = PressEase),
        label = "pressScale",
    )
    Button(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        enabled = enabled,
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
private fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}

private fun truncateChatId(chatId: String): String =
    if (chatId.length <= 16) chatId else chatId.take(8) + "…"
