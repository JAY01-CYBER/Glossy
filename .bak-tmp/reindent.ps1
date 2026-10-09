param(
    [int]$From = 324,
    [int]$To = 446,
    [int]$Spaces = 4
)
$path = 'C:\Users\Chikku\Downloads\Glossy-test-master\app\src\main\kotlin\com\jay\glossy\ui\player\MiniPlayer.kt'
$lines = [System.IO.File]::ReadAllLines($path)
$pad = ' ' * $Spaces
for ($i = $From - 1; $i -le $To - 1; $i++) {
    if ($lines[$i].Trim().Length -gt 0) {
        $lines[$i] = $pad + $lines[$i]
    }
}
[System.IO.File]::WriteAllLines($path, $lines)
Write-Output ("Reindented lines $From..$To")
