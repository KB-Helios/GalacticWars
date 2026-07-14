param(
    [string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$PythonExecutable = 'python'
)

$generator = Join-Path $RepositoryRoot 'tools/generate_lightsaber_model.py'
& $PythonExecutable $generator
if ($LASTEXITCODE -ne 0) {
    throw "Lightsaber GeckoLib asset generation failed with exit code $LASTEXITCODE"
}
