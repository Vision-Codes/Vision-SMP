# Maakt een nieuwe plugin op basis van vision-template.
# Gebruik (in de map src):  .\new-plugin.ps1 hub
# Resultaat: map vision-hub, package nl.codebyvision.hub, class VisionHub, command /hub
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-z][a-z0-9]*$')]
    [string]$Name
)

$ErrorActionPreference = 'Stop'
$lower = $Name
$pascal = $Name.Substring(0, 1).ToUpper() + $Name.Substring(1)
$source = Join-Path $PSScriptRoot 'vision-template'
$target = Join-Path $PSScriptRoot "vision-$lower"

if (Test-Path $target) { throw "$target bestaat al" }

Copy-Item $source $target -Recurse
Get-ChildItem $target -Recurse -Directory -Filter build | Remove-Item -Recurse -Force

# Mapnamen en bestandsnamen (diepste eerst, zodat paden kloppen tijdens het hernoemen)
Get-ChildItem $target -Recurse | Sort-Object { $_.FullName.Length } -Descending | ForEach-Object {
    $newName = $_.Name -creplace 'Template', $pascal -creplace 'template', $lower
    if ($newName -ne $_.Name) { Rename-Item $_.FullName $newName }
}

# Inhoud van de bestanden
Get-ChildItem $target -Recurse -File | ForEach-Object {
    $text = [IO.File]::ReadAllText($_.FullName)
    $new = $text -creplace 'Template', $pascal -creplace 'template', $lower
    if ($new -ne $text) { [IO.File]::WriteAllText($_.FullName, $new) }
}

Write-Host "Klaar: vision-$lower (class Vision$pascal, command /$lower)."
Write-Host "Pas description aan in vision-$lower\build.gradle.kts en bouw met: .\gradlew build"
