package com.movie.app.best.ui.screens.moviedetail.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val ISSUE_OPTIONS = listOf(
    "Broken Link",
    "Wrong Info",
    "No Stream",
    "Bad Quality",
    "Other"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportSection(
    isPosting: Boolean,
    posted: Boolean,
    error: String?,
    onPost: (issue: String, details: String) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(posted) {
        if (posted) { delay(2500); onReset() }
    }

    Column(modifier = modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
        OutlinedButton(
            onClick  = { showSheet = true },
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(8.dp),
            border   = BorderStroke(1.dp, Color(0xFFFF5252).copy(0.7f)),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
        ) {
            Icon(Icons.Default.Flag, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Report Issue", fontWeight = FontWeight.SemiBold)
        }

        AnimatedVisibility(visible = posted, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1B5E20).copy(0.3f))
                    .padding(10.dp)
            ) {
                Text("Report submitted! ✓", color = Color(0xFF69F0AE), fontSize = 13.sp)
            }
        }

        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp))
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState       = sheetState,
            containerColor   = Color(0xFF1E1E2E),
            shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            ReportSheetContent(
                isPosting = isPosting,
                onPost    = { issue, details -> onPost(issue, details); showSheet = false },
                onDismiss = { showSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportSheetContent(
    isPosting: Boolean,
    onPost: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var expanded      by remember { mutableStateOf(false) }
    var selectedIssue by remember { mutableStateOf("") }
    var details       by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text("Report Issue", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Help us fix problems", color = Color.White.copy(0.5f), fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value             = selectedIssue,
                onValueChange     = {},
                readOnly          = true,
                label             = { Text("Issue Type") },
                trailingIcon      = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier          = Modifier.fillMaxWidth().menuAnchor(),
                shape             = RoundedCornerShape(8.dp),
                colors            = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.DarkGray.copy(0.2f),
                    focusedContainerColor   = Color.DarkGray.copy(0.2f),
                    unfocusedIndicatorColor = Color.White.copy(0.1f),
                    focusedIndicatorColor   = Color(0xFFFF5252)
                )
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                ISSUE_OPTIONS.forEach { opt ->
                    DropdownMenuItem(
                        text    = { Text(opt, color = if (selectedIssue == opt) Color(0xFFFF5252) else Color.White) },
                        onClick = { selectedIssue = opt; expanded = false }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value         = details,
            onValueChange = { details = it },
            label         = { Text("Details (optional)") },
            modifier      = Modifier.fillMaxWidth(),
            minLines      = 2,
            maxLines      = 4,
            shape         = RoundedCornerShape(8.dp),
            colors        = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.DarkGray.copy(0.2f),
                focusedContainerColor   = Color.DarkGray.copy(0.2f),
                unfocusedIndicatorColor = Color.White.copy(0.1f),
                focusedIndicatorColor   = Color(0xFFFF5252)
            )
        )

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick  = onDismiss,
                modifier = Modifier.weight(1f),
                shape    = RoundedCornerShape(8.dp)
            ) { Text("Cancel") }

            Button(
                onClick  = { if (selectedIssue.isNotBlank()) onPost(selectedIssue, details) },
                enabled  = !isPosting && selectedIssue.isNotBlank(),
                modifier = Modifier.weight(1f),
                shape    = RoundedCornerShape(8.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
            ) {
                if (isPosting) {
                    CircularProgressIndicator(Modifier.size(18.dp), Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Send, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Submit", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
