@echo off

where msbuild >nul 2>&1
if errorlevel 1 (
  if exist "%ProgramFiles%\Microsoft Visual Studio\2022\Professional\VC\Auxiliary\Build\vcvars64.bat" (
    call "%ProgramFiles%\Microsoft Visual Studio\2022\Professional\VC\Auxiliary\Build\vcvars64.bat"
  ) else if exist "%ProgramFiles%\Microsoft Visual Studio\2022\Enterprise\VC\Auxiliary\Build\vcvars64.bat" (
    call "%ProgramFiles%\Microsoft Visual Studio\2022\Enterprise\VC\Auxiliary\Build\vcvars64.bat"
  ) else if exist "%ProgramFiles%\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat" (
    call "%ProgramFiles%\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat"
  ) else (
    echo Visual Studio 2022 not found.
    pause & exit /b 1
  )
)

for %%a in (%*) do (
  if "%%a"=="--clean" (
    msbuild "PCSX2_qt.sln" /m /v:m /p:Configuration=release /t:Clean /p:Platform=x64
    msbuild "PCSX2_qt.sln" /m /v:m /p:Configuration="Release AVX2" /t:Clean /p:Platform=x64
  ) else if "%%a"=="--help" (
    echo --clean: clean both SSE4 and AVX2 build outputs
  )
)

echo.
echo === Building SSE4 (release) ===
msbuild "PCSX2_qt.sln" /m /v:m /p:Configuration=release /p:Platform=x64
if errorlevel 1 ( echo SSE4 build FAILED & pause & exit /b 1 )

echo.
echo === Building AVX2 ===
msbuild "PCSX2_qt.sln" /m /v:m /p:Configuration="Release AVX2" /p:Platform=x64
if errorlevel 1 ( echo AVX2 build FAILED & pause & exit /b 1 )

echo.
echo === Both builds succeeded ===
echo   bin\pcsx2-qtx64.exe
echo   bin\pcsx2-qtx64-avx2.exe
pause