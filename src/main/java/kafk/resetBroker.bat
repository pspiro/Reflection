rem kill running brokers, consumer, producer
taskkill /F /IM java.exe

rem delete log folder
rmdir /s /q C:\Work\kafka\workkafkadatakraft-combined-logs

rem create new uid (powershell only)
[guid]::NewGuid().ToString()

rem format storage with new cluster id
bin\windows\kafka-storage.bat format -t <your-generated-uuid> -c config\kraft\server.properties --standalone

rem start broker
bin\windows\kafka-server-start.bat config\kraft\server.properties

