package privacy.keyboard.testapp

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TestAppTheme { App() } }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    TYPE("Type", Icons.Filled.Edit),
    SAMPLES("Samples", Icons.AutoMirrored.Filled.List),
    CHECK("Check", Icons.Filled.CheckCircle),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun App() {
    var tab by rememberSaveable { mutableStateOf(Tab.TYPE) }
    val messages = remember { mutableStateListOf<String>() }
    val done = remember { mutableStateListOf<Int>() }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Keyboard Test", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Blue,
                    titleContentColor = Color.White,
                ),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                Tab.entries.forEach {
                    NavigationBarItem(
                        selected = tab == it,
                        onClick = { tab = it },
                        icon = { Icon(it.icon, contentDescription = null) },
                        label = { Text(it.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize().background(Color.White)) {
            when (tab) {
                Tab.TYPE -> TypeScreen(messages)
                Tab.SAMPLES -> SamplesScreen(done)
                Tab.CHECK -> CheckScreen()
            }
        }
    }
}

/** Chat-like screen: type here with the test keyboard, so the keyboard learns (or blocks) the words. */
@Composable
private fun TypeScreen(messages: MutableList<String>) {
    var text by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().imePadding()) {
        Hint("Type with the test keyboard. Don't paste: pasted text is never learned, so it proves nothing.")
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(messages) { Bubble(it) }
        }
        if (messages.isNotEmpty()) {
            TextButton(onClick = { messages.clear() }, modifier = Modifier.align(Alignment.End)) { Text("Clear") }
        }
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message") },
                shape = RoundedCornerShape(24.dp),
                // a normal text field: the keyboard is allowed to learn here
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    keyboardType = KeyboardType.Text,
                ),
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = {
                    if (text.isNotBlank()) messages.add(text.trim())
                    text = ""
                },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
private fun Bubble(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text(
            text,
            color = Color.White,
            modifier = Modifier
                .background(Blue, RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

/** Sentences to type, with what the keyboard should do with them. Tap a row to mark it as typed. */
@Composable
private fun SamplesScreen(done: MutableList<Int>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Hint("Type each sentence twice in the Type tab, then tap it here. New words need a couple of uses before they are suggested.", padded = false) }
        itemsIndexed(samples) { i, s ->
            val isDone = i in done
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(if (isDone) LightBlue else Color.White, RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .clickable { if (isDone) done.remove(i) else done.add(i) }
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.text, fontFamily = FontFamily.Monospace, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    if (isDone) Icon(Icons.Filled.CheckCircle, contentDescription = "Typed", tint = Blue)
                }
                Spacer(Modifier.height(6.dp))
                Tag(if (s.shouldBeLearned) "Should be learned" else "Should be blocked", filled = !s.shouldBeLearned)
                Spacer(Modifier.height(4.dp))
                Text("Word: ${s.word}", color = GrayText, fontSize = 13.sp)
                if (s.checkPrefix != "-")
                    Text(
                        "Check: type \"${s.checkPrefix}\" in a new message. " +
                            if (s.shouldBeLearned) "It should be suggested." else "It should NOT be suggested.",
                        color = GrayText, fontSize = 13.sp,
                    )
                else
                    Text("Check: only visible in the dictionary file (see Check tab).", color = GrayText, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun Tag(text: String, filled: Boolean) {
    Text(
        text,
        color = if (filled) Color.White else Blue,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .background(if (filled) Blue else Color.White, RoundedCornerShape(50))
            .border(1.dp, Blue, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** How to verify the result, plus shortcuts to the keyboard. */
@Composable
private fun CheckScreen() {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("1. Quick check: suggestions", fontWeight = FontWeight.SemiBold, color = Blue)
        Text("After typing the samples, start a new message and type the \"Check\" letters of each sample. Learned words show up in the suggestion strip, blocked words never do.")

        Text("2. Incognito button", fontWeight = FontWeight.SemiBold, color = Blue)
        Text("Turn on incognito with the button next to the suggestions, type a new made-up word twice, and check that it is not suggested.")

        Text("3. Exact check: dictionary file (emulator)", fontWeight = FontWeight.SemiBold, color = Blue)
        Text("Debug builds of the keyboard let adb read its files with run-as, no root needed.")
        SelectionContainer {
            Text(
                "adb shell run-as helium314.keyboard.debug ls files\n" +
                    "adb exec-out run-as helium314.keyboard.debug cat files/UserHistoryDictionary.en-US.dict > uhd.dict",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().background(LightBlue, RoundedCornerShape(8.dp)).padding(12.dp),
            )
        }

        Spacer(Modifier.height(4.dp))
        Button(onClick = { openKeyboardSettings(context) }, modifier = Modifier.fillMaxWidth()) {
            Text("Open keyboard settings")
        }
        OutlinedButton(
            onClick = { context.getSystemService(InputMethodManager::class.java).showInputMethodPicker() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Switch keyboard")
        }
    }
}

@Composable
private fun Hint(text: String, padded: Boolean = true) {
    Text(
        text,
        color = Blue,
        fontSize = 13.sp,
        modifier = Modifier
            .then(if (padded) Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp) else Modifier)
            .fillMaxWidth()
            .background(LightBlue, RoundedCornerShape(8.dp))
            .padding(10.dp),
    )
}

private fun openKeyboardSettings(context: Context) {
    // debug builds of the keyboard use the ".debug" suffix
    val intent = listOf("helium314.keyboard.debug", "helium314.keyboard")
        .firstNotNullOfOrNull { context.packageManager.getLaunchIntentForPackage(it) }
    if (intent == null) Toast.makeText(context, "Test keyboard is not installed", Toast.LENGTH_SHORT).show()
    else context.startActivity(intent)
}
