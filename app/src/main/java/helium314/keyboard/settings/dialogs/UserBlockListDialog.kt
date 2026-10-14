// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.dialogs

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import helium314.keyboard.latin.R
import helium314.keyboard.latin.privacy.PrivacyFilter

/** Shows the user's block list (masked, the words are not stored) and lets them add and remove words. */
@Composable
fun UserBlockListDialog(onDismissRequest: () -> Unit) {
    val ctx = LocalContext.current
    var entries by remember { mutableStateOf(PrivacyFilter.userBlockList(ctx).entries) }
    var showAddDialog by remember { mutableStateOf(false) }
    ThreeButtonAlertDialog(
        onDismissRequest = onDismissRequest,
        onConfirmed = { },
        title = { Text(stringResource(R.string.privacy_user_list_title)) },
        content = {
            Column {
                Text(stringResource(R.string.privacy_user_list_info))
                if (entries.isEmpty())
                    Text(stringResource(R.string.privacy_user_list_empty))
                entries.forEach { entry ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.mask, Modifier.weight(1f))
                        IconButton(onClick = {
                            PrivacyFilter.removeFromUserBlockList(ctx, entry)
                            entries = PrivacyFilter.userBlockList(ctx).entries
                        }) { Icon(painterResource(R.drawable.ic_bin), stringResource(R.string.delete)) }
                    }
                }
            }
        },
        scrollContent = true,
        confirmButtonText = null,
        cancelButtonText = stringResource(R.string.dialog_close),
        neutralButtonText = stringResource(R.string.privacy_user_list_add),
        onNeutral = { showAddDialog = true },
    )
    if (showAddDialog) {
        val alreadyThere = stringResource(R.string.privacy_user_list_duplicate)
        TextInputDialog(
            onDismissRequest = { showAddDialog = false },
            onConfirmed = {
                if (!PrivacyFilter.addToUserBlockList(ctx, it))
                    Toast.makeText(ctx, alreadyThere, Toast.LENGTH_SHORT).show()
                entries = PrivacyFilter.userBlockList(ctx).entries
            },
            title = { Text(stringResource(R.string.privacy_user_list_add)) },
            description = { Text(stringResource(R.string.privacy_user_list_add_info)) },
            // password field: no keyboard learns or suggests what is typed here, this one included
            keyboardType = KeyboardType.Password,
            checkTextValid = { it.isNotBlank() && !it.trim().contains(' ') },
        )
    }
}
