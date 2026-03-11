package com.autobox.autoboxlauncher.ui

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog
import android.telecom.TelecomManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.autobox.autoboxlauncher.R
import com.autobox.autoboxlauncher.call.CallRepository
import com.autobox.autoboxlauncher.call.CallState
import com.autobox.autoboxlauncher.call.Contact
import com.autobox.autoboxlauncher.call.ContactsRepository
import com.autobox.autoboxlauncher.call.RecentCall
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

private val GreenCall = Color(0xFF2E7D32)
private val RedCall   = Color(0xFFC62828)
private val MissedColor = Color(0xFFC62828)

private enum class DialerTab { CONTACTS, RECENT, DIALPAD }


@Composable
fun CallScreen() {
    val callState by CallRepository.callState.collectAsState()

    when (val state = callState) {
        is CallState.Idle     -> DialerView()
        is CallState.Incoming -> IncomingCallContent(state)
        is CallState.Active   -> ActiveCallContent(state)
    }
}


@Composable
private fun DialerView() {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(DialerTab.CONTACTS) }
    var searchQuery by remember { mutableStateOf("") }

    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var recents  by remember { mutableStateOf<List<RecentCall>>(emptyList()) }
    var loading  by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        contacts = ContactsRepository.loadContacts(context)
        recents  = ContactsRepository.loadRecentCalls(context)
        loading  = false
    }

    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TabButton(
                label = stringResource(R.string.call_tab_contacts),
                icon = Icons.Rounded.Contacts,
                selected = activeTab == DialerTab.CONTACTS
            ) { activeTab = DialerTab.CONTACTS; searchQuery = "" }

            TabButton(
                label = stringResource(R.string.call_tab_recent),
                icon = Icons.Rounded.History,
                selected = activeTab == DialerTab.RECENT
            ) { activeTab = DialerTab.RECENT; searchQuery = "" }

            TabButton(
                label = stringResource(R.string.call_tab_dialpad),
                icon = Icons.Rounded.Dialpad,
                selected = activeTab == DialerTab.DIALPAD
            ) { activeTab = DialerTab.DIALPAD; searchQuery = "" }

            if (activeTab != DialerTab.DIALPAD) {
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (activeTab == DialerTab.CONTACTS)
                                stringResource(R.string.call_search_contacts_hint)
                            else
                                stringResource(R.string.call_search_recent_hint),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

        when (activeTab) {
            DialerTab.CONTACTS -> ContactsTab(
                contacts = contacts,
                query = searchQuery,
                loading = loading,
                onCall = { placeCall(context, it) }
            )
            DialerTab.RECENT -> RecentCallsTab(
                recents = recents,
                query = searchQuery,
                loading = loading,
                onCall = { placeCall(context, it) }
            )
            DialerTab.DIALPAD -> DialpadTab(
                onCall = { placeCall(context, it) }
            )
        }
    }
}


@Composable
private fun ContactsTab(
    contacts: List<Contact>,
    query: String,
    loading: Boolean,
    onCall: (String) -> Unit,
) {
    val filtered = remember(contacts, query) {
        if (query.isBlank()) contacts
        else contacts.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.numbers.any { n -> n.contains(query) }
        }
    }

    when {
        loading -> CenteredProgress()
        filtered.isEmpty() -> CenteredHint(
            icon = Icons.Rounded.PersonOff,
            text = if (query.isBlank()) stringResource(R.string.call_no_contacts)
                   else stringResource(R.string.call_no_results, query)
        )
        else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filtered, key = { it.id }) { contact ->
                ContactRow(contact = contact, onCall = onCall)
                HorizontalDivider(
                    modifier = Modifier.padding(start = 80.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                )
            }
        }
    }
}

@Composable
private fun ContactRow(contact: Contact, onCall: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val primaryNumber = contact.numbers.first()

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ContactAvatar(name = contact.name, size = 48)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = primaryNumber,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (contact.numbers.size > 1) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(
                        text = "+${contact.numbers.size - 1}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(
                onClick = { onCall(primaryNumber) },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(GreenCall.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Call,
                    contentDescription = stringResource(R.string.call_action_call),
                    tint = GreenCall,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        if (expanded) {
            contact.numbers.drop(1).forEach { number ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 78.dp, end = 16.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = number,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    IconButton(
                        onClick = { onCall(number) },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(GreenCall.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = stringResource(R.string.call_action_call),
                            tint = GreenCall,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun RecentCallsTab(
    recents: List<RecentCall>,
    query: String,
    loading: Boolean,
    onCall: (String) -> Unit,
) {
    val filtered = remember(recents, query) {
        if (query.isBlank()) recents
        else recents.filter {
            it.number.contains(query) ||
            it.name?.contains(query, ignoreCase = true) == true
        }
    }

    when {
        loading -> CenteredProgress()
        filtered.isEmpty() -> CenteredHint(
            icon = Icons.Rounded.HistoryToggleOff,
            text = if (query.isBlank()) stringResource(R.string.call_no_recent)
                   else stringResource(R.string.call_no_results, query)
        )
        else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filtered) { call ->
                RecentCallRow(call = call, onCall = onCall)
                HorizontalDivider(
                    modifier = Modifier.padding(start = 80.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                )
            }
        }
    }
}

@Composable
private fun RecentCallRow(call: RecentCall, onCall: (String) -> Unit) {
    val isMissed = call.type == CallLog.Calls.MISSED_TYPE
    val typeIcon = when (call.type) {
        CallLog.Calls.INCOMING_TYPE -> Icons.Rounded.CallReceived
        CallLog.Calls.OUTGOING_TYPE -> Icons.Rounded.CallMade
        CallLog.Calls.MISSED_TYPE   -> Icons.Rounded.CallMissed
        else                        -> Icons.Rounded.Call
    }
    val typeColor = if (isMissed) MissedColor
                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = typeIcon,
                contentDescription = null,
                tint = typeColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = call.name ?: call.number,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = if (isMissed) MissedColor else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (call.name != null) {
                    Text(
                        text = call.number,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "·",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
                val context = LocalContext.current
                Text(
                    text = formatCallDate(call.dateMs, context),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }
        }

        IconButton(
            onClick = { onCall(call.number) },
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(GreenCall.copy(alpha = 0.12f))
        ) {
            Icon(
                imageVector = Icons.Rounded.Call,
                contentDescription = stringResource(R.string.call_action_callback),
                tint = GreenCall,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}


@Composable
private fun DialpadTab(onCall: (String) -> Unit) {
    var number by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = number.ifEmpty { stringResource(R.string.call_enter_number) },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Light,
                        color = if (number.isEmpty())
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (number.isNotEmpty()) {
                        IconButton(onClick = { number = number.dropLast(1) }) {
                            Icon(
                                imageVector = Icons.Rounded.Backspace,
                                contentDescription = stringResource(R.string.call_action_delete),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { if (number.isNotEmpty()) onCall(number) },
                enabled = number.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenCall),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.call_action_call), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        DialPad(
            modifier = Modifier.weight(1f),
            onKey = { if (number.length < 16) number += it }
        )
    }
}

@Composable
private fun DialPad(modifier: Modifier = Modifier, onKey: (String) -> Unit) {
    val d2 = stringResource(R.string.dialpad_2)
    val d3 = stringResource(R.string.dialpad_3)
    val d4 = stringResource(R.string.dialpad_4)
    val d5 = stringResource(R.string.dialpad_5)
    val d6 = stringResource(R.string.dialpad_6)
    val d7 = stringResource(R.string.dialpad_7)
    val d8 = stringResource(R.string.dialpad_8)
    val d9 = stringResource(R.string.dialpad_9)
    val rows = listOf(
        listOf("1" to "", "2" to d2, "3" to d3),
        listOf("4" to d4, "5" to d5, "6" to d6),
        listOf("7" to d7, "8" to d8, "9" to d9),
        listOf("*" to "", "0" to "+", "#" to ""),
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (digit, sub) ->
                    DialKey(digit = digit, sub = sub, modifier = Modifier.weight(1f)) { onKey(digit) }
                }
            }
        }
    }
}

@Composable
private fun DialKey(
    digit: String,
    sub: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = digit, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            if (sub.isNotEmpty()) {
                Text(
                    text = sub,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}


@Composable
private fun IncomingCallContent(state: CallState.Incoming) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Text(
                text = stringResource(R.string.call_incoming),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
            )

            CallerInfo(name = state.callerName, number = state.callerNumber)

            Row(
                horizontalArrangement = Arrangement.spacedBy(72.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CallActionButton(
                    icon = Icons.Rounded.CallEnd,
                    label = stringResource(R.string.call_decline),
                    color = RedCall,
                    sizeDp = 80
                ) { CallRepository.decline() }

                CallActionButton(
                    icon = Icons.Rounded.Call,
                    label = stringResource(R.string.call_answer),
                    color = GreenCall,
                    sizeDp = 80
                ) { CallRepository.answer() }
            }
        }
    }
}


@Composable
private fun ActiveCallContent(state: CallState.Active) {
    var elapsedSec by remember { mutableIntStateOf(0) }
    var isMuted    by remember { mutableStateOf(false) }
    var isSpeaker  by remember { mutableStateOf(false) }

    LaunchedEffect(state.startTimeMs) {
        while (true) {
            elapsedSec = ((System.currentTimeMillis() - state.startTimeMs) / 1000).toInt()
            delay(1_000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Text(
                text = formatCallDuration(elapsedSec),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            CallerInfo(name = state.callerName, number = state.callerNumber)

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                ToggleCallButton(
                    icon = if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                    label = if (isMuted) stringResource(R.string.call_mute_on)
                            else stringResource(R.string.call_mute_off),
                    active = isMuted
                ) {
                    isMuted = !isMuted
                    CallRepository.setMuted(isMuted)
                }
                ToggleCallButton(
                    icon = Icons.Rounded.VolumeUp,
                    label = if (isSpeaker) stringResource(R.string.call_audio_earpiece)
                            else stringResource(R.string.call_audio_speaker),
                    active = isSpeaker
                ) {
                    isSpeaker = !isSpeaker
                    CallRepository.setSpeakerOn(isSpeaker)
                }
            }

            CallActionButton(
                icon = Icons.Rounded.CallEnd,
                label = stringResource(R.string.call_end),
                color = RedCall,
                sizeDp = 80
            ) { CallRepository.hangUp() }
        }
    }
}


@Composable
private fun TabButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary
                             else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                           else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (selected) 3.dp else 0.dp),
        modifier = Modifier.height(44.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
fun ContactAvatar(name: String, size: Int) {
    val initials = name.trim().split(Regex("\\s+"))
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
    val bgColor = avatarColor(name)

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials.ifEmpty { "?" },
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size * 0.35f).sp
        )
    }
}

@Composable
private fun CallerInfo(name: String?, number: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (name != null) {
            ContactAvatar(name = name, size = 80)
        } else {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = name ?: number ?: stringResource(R.string.call_unknown_caller),
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        if (name != null && number != null) {
            Text(
                text = number,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun CallActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    sizeDp: Int,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.size(sizeDp.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = color),
            contentPadding = PaddingValues(0.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size((sizeDp * 0.45f).dp)
            )
        }
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
        )
    }
}

@Composable
private fun ToggleCallButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                 else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CenteredProgress() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CenteredHint(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}


private fun formatCallDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

private fun formatCallDate(ms: Long, context: Context): String {
    val diff = System.currentTimeMillis() - ms
    return when {
        diff < 60_000 -> context.getString(R.string.call_time_just_now)
        diff < 3_600_000 -> context.getString(R.string.call_time_minutes_ago, diff / 60_000)
        diff < 86_400_000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))
        diff < 7 * 86_400_000L -> SimpleDateFormat("EEE HH:mm", Locale.getDefault()).format(Date(ms))
        else -> SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(ms))
    }
}

private fun placeCall(context: Context, number: String) {
    val tm = context.getSystemService(TelecomManager::class.java)
    val uri = Uri.fromParts("tel", number, null)
    try {
        tm.placeCall(uri, Bundle())
    } catch (_: SecurityException) { }
}

private val avatarColors = listOf(
    Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFF6A1B9A),
    Color(0xFFAD1457), Color(0xFF00838F), Color(0xFFE65100),
    Color(0xFF4527A0), Color(0xFF00695C), Color(0xFF558B2F),
)

private fun avatarColor(name: String): Color =
    avatarColors[name.hashCode().absoluteValue % avatarColors.size]


// ---------------------------------------------------------------------------
// Incoming call overlay — shown over any screen
// ---------------------------------------------------------------------------

@Composable
fun IncomingCallOverlay(state: CallState.Incoming) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.60f)),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .align(Alignment.TopCenter),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = null,
                        tint = GreenCall,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.call_incoming),
                        style = MaterialTheme.typography.labelLarge,
                        color = GreenCall,
                    )
                }

                // Caller + action buttons in one row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Avatar
                    if (state.callerName != null) {
                        ContactAvatar(name = state.callerName, size = 52)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }

                    // Name / number
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.callerName
                                ?: state.callerNumber
                                ?: stringResource(R.string.call_unknown_caller),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (state.callerName != null && state.callerNumber != null) {
                            Text(
                                text = state.callerNumber,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            )
                        }
                    }

                    // Decline
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Button(
                            onClick = { CallRepository.decline() },
                            modifier = Modifier.size(60.dp),
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RedCall),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CallEnd,
                                contentDescription = stringResource(R.string.call_decline),
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        Text(
                            text = stringResource(R.string.call_decline),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }

                    // Answer
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Button(
                            onClick = { CallRepository.answer() },
                            modifier = Modifier.size(60.dp),
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenCall),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Call,
                                contentDescription = stringResource(R.string.call_answer),
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        Text(
                            text = stringResource(R.string.call_answer),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }
    }
}


// ---------------------------------------------------------------------------
// Active call banner — compact top strip shown over non-Calls screens
// ---------------------------------------------------------------------------

@Composable
fun ActiveCallBanner(state: CallState.Active, onClick: () -> Unit) {
    var elapsedSec by remember { mutableIntStateOf(0) }
    LaunchedEffect(state.startTimeMs) {
        while (true) {
            elapsedSec = ((System.currentTimeMillis() - state.startTimeMs) / 1000).toInt()
            delay(1_000)
        }
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Pulsing green indicator dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(GreenCall),
            )

            // Caller name + duration
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.callerName
                        ?: state.callerNumber
                        ?: stringResource(R.string.call_unknown_caller),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = formatCallDuration(elapsedSec),
                    style = MaterialTheme.typography.labelSmall,
                    color = GreenCall,
                )
            }

            // Tap hint
            Text(
                text = stringResource(R.string.call_banner_tap_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            )

            // End call
            FilledIconButton(
                onClick = { CallRepository.hangUp() },
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = RedCall),
            ) {
                Icon(
                    imageVector = Icons.Rounded.CallEnd,
                    contentDescription = stringResource(R.string.call_end),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
