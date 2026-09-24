import re

with open('app/src/main/java/com/example/data/repository/ScheduleRepository.kt', 'r') as f:
    content = f.read()

target = """    val allHolidayOverrides: Flow<List<HolidayOverrideEntity>> = database.holidayOverrideDao().getAllHolidayOverrides()"""
replacement = """    val allHolidayOverrides: Flow<List<HolidayOverrideEntity>> = database.holidayOverrideDao().getAllHolidayOverrides()
    val allNotes: Flow<List<com.example.data.model.NoteEntity>> = database.noteDao().getAllNotes()"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/data/repository/ScheduleRepository.kt', 'w') as f:
    f.write(content)
print("Updated ScheduleRepository.kt")
