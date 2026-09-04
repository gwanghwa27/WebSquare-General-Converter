@echo off
rem 폐쇄망 batch 변환 root convenience entrypoint(Slice 100D Correction, thin delegation).
rem execution-state를 바꾸지 않고 delegated 스크립트에 인자/exit code만 그대로 중계한다.

call "%~dp0closed-network-import\BATCH-CONVERT.cmd" %*
exit /b %errorlevel%
