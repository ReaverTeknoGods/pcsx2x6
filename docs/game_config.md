# game config

Since the execution of arcade games consists on the interaction with several files (a main executable, a disc/HDD image and a memory card image). the system2x6 games will be executed via a special per-game settings file

this files provide all the information the emulator needs to setup the enviroment and run that game

the file uses the INI format, and the extension must be `.acgame`

here's an example of how it should look like in the inside

```ini
[game]
name=SoulCalibur II (SC21 vA10 + DVD0D) # <-- game title for game list
gameid=NM00007 # <-- game ID
[data]
elf=boot.elf # <-- elf filename to be executed at the same location than this file
dongle=NM00007 SC21, Ver.A10 a025781148773a.bin # <-- security dongle filename to be mounted, same location than this file
card=ConquestCard0.bin # <-- secondary memory card, only useful for SoulCalibur2
mediasrc=dvd0.img # <-- filename of the media file
media=DVD # <-- media type of the media file [CD/DVD/HDD]
```

