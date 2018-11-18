Данные сохраняются в формате .pcm (аудио без сжатия). Для воспроизведения:
 * воспользуйтесь командой `play -t raw -r 44100 -e signed -b 16 -c 1 filename.pcm` в Linux
 * откройте Audacity, `File -> Import -> Raw Data`, выставьте следующие параметры: 
 
   - Encoding: Signed 16-bit PCM
   - Byte order: Little-endian
   - Channels: 1 Channel (Mono)
   - Start offset: 0 bytes
   - Amount to import: 100%
   - Sample rate: 44100 Hz

   Если файл не открывается в Audacity, попробуйте переоткрыть его копию в формате `.wav`.
