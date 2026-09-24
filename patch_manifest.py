import re

with open('app/src/main/AndroidManifest.xml', 'r') as f:
    content = f.read()

receiver_xml = """        <receiver
            android:name=".service.DndAutomationReceiver"
            android:exported="false">
            <intent-filter>
                <action android:name="com.example.ACTION_DND_TURN_ON" />
                <action android:name="com.example.ACTION_DND_TURN_OFF" />
            </intent-filter>
        </receiver>"""

if "DndAutomationReceiver" not in content:
    content = content.replace("</application>", receiver_xml + "\n    </application>")
    with open('app/src/main/AndroidManifest.xml', 'w') as f:
        f.write(content)
