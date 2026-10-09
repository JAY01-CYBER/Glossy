param(
    [int]$From = 890,
    [int]$To = 890
)
$path = 'C:\Users\Chikku\Downloads\Glossy-test-master\app\src\main\kotlin\com\jay\glossy\ui\player\MiniPlayer.kt'
$lines = [System.IO.File]::ReadAllLines($path)
for ($i = $From - 1; $i -le $To - 1; $i++) {
    $lines[$i] = $lines[$i].TrimEnd()
}
[System.IO.File]::WriteAllLines($path, $lines)
Write-Output ("Trimmed trailing spaces on lines $From..$To")
