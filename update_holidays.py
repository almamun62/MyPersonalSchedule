import re

with open('app/src/main/java/com/example/data/repository/ScheduleRepository.kt', 'r') as f:
    content = f.read()

replacement = """            // SWPU Academic Calendar 2026-2027 Holidays
            val holidays = listOf(
                com.example.data.model.HolidayOverrideEntity(name = "Mid-Autumn Festival (中秋节)", dateString = "2026-09-25", type = com.example.data.model.HolidayOverrideType.HOLIDAY),
                com.example.data.model.HolidayOverrideEntity(name = "Mid-Autumn Festival (中秋节)", dateString = "2026-09-26", type = com.example.data.model.HolidayOverrideType.HOLIDAY),
                com.example.data.model.HolidayOverrideEntity(name = "Mid-Autumn Festival (中秋节)", dateString = "2026-09-27", type = com.example.data.model.HolidayOverrideType.HOLIDAY),
                com.example.data.model.HolidayOverrideEntity(name = "National Day (国庆节)", dateString = "2026-10-01", type = com.example.data.model.HolidayOverrideType.HOLIDAY),
                com.example.data.model.HolidayOverrideEntity(name = "National Day (国庆节)", dateString = "2026-10-02", type = com.example.data.model.HolidayOverrideType.HOLIDAY),
                com.example.data.model.HolidayOverrideEntity(name = "National Day (国庆节)", dateString = "2026-10-03", type = com.example.data.model.HolidayOverrideType.HOLIDAY),
                com.example.data.model.HolidayOverrideEntity(name = "National Day (国庆节)", dateString = "2026-10-04", type = com.example.data.model.HolidayOverrideType.HOLIDAY),
                com.example.data.model.HolidayOverrideEntity(name = "National Day (国庆节)", dateString = "2026-10-05", type = com.example.data.model.HolidayOverrideType.HOLIDAY),
                com.example.data.model.HolidayOverrideEntity(name = "National Day (国庆节)", dateString = "2026-10-06", type = com.example.data.model.HolidayOverrideType.HOLIDAY),
                com.example.data.model.HolidayOverrideEntity(name = "National Day (国庆节)", dateString = "2026-10-07", type = com.example.data.model.HolidayOverrideType.HOLIDAY),
                com.example.data.model.HolidayOverrideEntity(name = "New Year's Day (元旦)", dateString = "2027-01-01", type = com.example.data.model.HolidayOverrideType.HOLIDAY)
            )
            database.holidayOverrideDao().insertOverrides(holidays)"""

content = re.sub(r'            // Insert preloaded holiday & make-up overrides.*?database\.holidayOverrideDao\(\)\.insertOverrides\(holidays\)', replacement, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/data/repository/ScheduleRepository.kt', 'w') as f:
    f.write(content)
