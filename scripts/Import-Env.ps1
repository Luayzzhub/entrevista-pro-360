param([string]$Path = (Join-Path $PSScriptRoot '..\.env'))

# Carga KEY=VALUE en el proceso actual sin ejecutar contenido del archivo.
if (-not (Test-Path -LiteralPath $Path)) { throw "No existe $Path. Copia .env.example a .env y configura DB_*." }
foreach ($line in Get-Content -LiteralPath $Path -Encoding utf8) {
    $entry = $line.Trim()
    if (-not $entry -or $entry.StartsWith('#')) { continue }
    if ($entry -notmatch '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') { throw 'Línea inválida en .env: se requiere KEY=VALUE.' }
    $entryName = $Matches[1]
    $entryValue = $Matches[2].Trim()
    if ($entryValue.Length -ge 2 -and (($entryValue.StartsWith('"') -and $entryValue.EndsWith('"')) -or ($entryValue.StartsWith("'") -and $entryValue.EndsWith("'")))) {
        $entryValue = $entryValue.Substring(1, $entryValue.Length - 2)
    }
    [Environment]::SetEnvironmentVariable($entryName, $entryValue, 'Process')
}
