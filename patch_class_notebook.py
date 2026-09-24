import re

with open('app/src/main/java/com/example/ui/components/ClassNotebookDialog.kt', 'r') as f:
    content = f.read()

target_vars = """    var draftText by remember { mutableStateOf("") }
    var editModeNote by remember { mutableStateOf<NoteEntity?>(null) }"""
replacement_vars = """    var draftText by remember { mutableStateOf("") }
    var tagsText by remember { mutableStateOf("") }
    var editModeNote by remember { mutableStateOf<NoteEntity?>(null) }"""
content = content.replace(target_vars, replacement_vars)

target_save = """    fun saveDraft() {
        if (draftText.isNotBlank()) {
            if (editModeNote != null) {
                viewModel.updateNote(editModeNote!!.copy(content = draftText, timestampMillis = System.currentTimeMillis()))
                editModeNote = null
            } else {
                viewModel.addNote(
                    NoteEntity(
                        courseId = course.id,
                        content = draftText.trim(),
                        type = NoteType.TEXT
                    )
                )
            }
            draftText = ""
        }
    }"""
replacement_save = """    fun saveDraft() {
        if (draftText.isNotBlank()) {
            if (editModeNote != null) {
                viewModel.updateNote(editModeNote!!.copy(content = draftText, tags = tagsText.trim(), timestampMillis = System.currentTimeMillis()))
                editModeNote = null
            } else {
                viewModel.addNote(
                    NoteEntity(
                        courseId = course.id,
                        content = draftText.trim(),
                        tags = tagsText.trim(),
                        type = NoteType.TEXT
                    )
                )
            }
            draftText = ""
            tagsText = ""
        }
    }"""
content = content.replace(target_save, replacement_save)

target_edit = """                                        editModeNote = note
                                        draftText = note.content"""
replacement_edit = """                                        editModeNote = note
                                        draftText = note.content
                                        tagsText = note.tags"""
content = content.replace(target_edit, replacement_edit)

target_input = """                            OutlinedTextField(
                                value = draftText,
                                onValueChange = { draftText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Take a note...".tr) },
                                maxLines = 4
                            )"""
replacement_input = """                            Column(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = tagsText,
                                    onValueChange = { tagsText = it },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                    placeholder = { Text("Tags (optional)".tr) },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                )
                                OutlinedTextField(
                                    value = draftText,
                                    onValueChange = { draftText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Take a note...".tr) },
                                    maxLines = 4
                                )
                            }"""
content = content.replace(target_input, replacement_input)

target_card = """                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }"""
replacement_card = """                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (note.tags.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            note.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Text(
                                        text = "#$tag",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }"""
content = content.replace(target_card, replacement_card)

with open('app/src/main/java/com/example/ui/components/ClassNotebookDialog.kt', 'w') as f:
    f.write(content)
print("Updated ClassNotebookDialog.kt")
