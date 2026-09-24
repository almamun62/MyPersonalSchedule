import re

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'r') as f:
    content = f.read()

target = """                repository.allHolidayOverrides,
                _selectedWeek,"""
replacement = """                repository.allHolidayOverrides,
                repository.allNotes,
                _selectedWeek,"""
content = content.replace(target, replacement)

target2 = """                val holidays = values[3] as List<HolidayOverrideEntity>
                val selWeek = values[4] as Int
                val autoDnd = values[5] as Boolean
                val is15mEnabled = values[6] as Boolean
                val remMins = values[7] as Int
                val remindersCount = values[8] as Int
                val quickCourse = values[9] as? String
                val ticker = values[10] as Long"""
replacement2 = """                val holidays = values[3] as List<HolidayOverrideEntity>
                val notes = values[4] as List<com.example.data.model.NoteEntity>
                val selWeek = values[5] as Int
                val autoDnd = values[6] as Boolean
                val is15mEnabled = values[7] as Boolean
                val remMins = values[8] as Int
                val remindersCount = values[9] as Int
                val quickCourse = values[10] as? String
                val ticker = values[11] as Long"""
content = content.replace(target2, replacement2)

target3 = """                    holidayOverrides = holidays,"""
replacement3 = """                    holidayOverrides = holidays,
                    allNotes = notes,"""
content = content.replace(target3, replacement3)

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'w') as f:
    f.write(content)
print("Updated ScheduleViewModel.kt flow combine")
