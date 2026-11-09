param([string]$BaseUrl = 'http://localhost:8080')
$ErrorActionPreference = 'Stop'
python "$PSScriptRoot/verify_baseline.py" --base-url $BaseUrl
if ($LASTEXITCODE -ne 0) { throw 'Current baseline verification failed.' }
