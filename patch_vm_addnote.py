import re

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'r') as f:
    content = f.read()

target = """    fun addNote(note: NoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.database.noteDao().insertNote(note)
        }
    }"""
replacement = """    fun addNote(note: NoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.database.noteDao().insertNote(note)
        }
    }

    fun addNote(courseId: Long, content: String, tags: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.database.noteDao().insertNote(
                com.example.data.model.NoteEntity(courseId = courseId, content = content, tags = tags)
            )
        }
    }"""
content = content.replace(target, replacement)
with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'w') as f:
    f.write(content)
print("Updated ScheduleViewModel.kt addNote")
