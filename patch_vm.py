import re

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'r') as f:
    content = f.read()

# Add a function to get notes for a course
notes_funcs = """
    fun getNotesForCourse(courseId: Long): Flow<List<NoteEntity>> {
        return repository.database.noteDao().getNotesForCourse(courseId)
    }

    fun addNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.database.noteDao().insertNote(note)
        }
    }

    fun updateNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.database.noteDao().updateNote(note)
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.database.noteDao().deleteNote(note)
        }
    }
"""

if "getNotesForCourse" not in content:
    content = re.sub(r'(fun deleteHolidayOverride\(override: HolidayOverrideEntity\) \{)', notes_funcs + r'\n    \1', content)
    with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'w') as f:
        f.write(content)
