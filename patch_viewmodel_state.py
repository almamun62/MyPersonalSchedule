import re

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'r') as f:
    content = f.read()

target = """    val holidayOverrides: List<HolidayOverrideEntity> = emptyList(),"""
replacement = """    val holidayOverrides: List<HolidayOverrideEntity> = emptyList(),
    val allNotes: List<com.example.data.model.NoteEntity> = emptyList(),"""

content = content.replace(target, replacement)

target2 = """            db.holidayOverrideDao().getAllOverrides(),"""
replacement2 = """            db.holidayOverrideDao().getAllOverrides(),
            db.noteDao().getAllNotes(),"""

content = content.replace(target2, replacement2)

target3 = """                val overrides = results[2] as List<HolidayOverrideEntity>"""
replacement3 = """                val overrides = results[2] as List<HolidayOverrideEntity>
                val notes = results[3] as List<com.example.data.model.NoteEntity>"""
content = content.replace(target3, replacement3)

target4 = """                    holidayOverrides = overrides,"""
replacement4 = """                    holidayOverrides = overrides,
                    allNotes = notes,"""
content = content.replace(target4, replacement4)

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'w') as f:
    f.write(content)
print("Updated ScheduleViewModel.kt state")
