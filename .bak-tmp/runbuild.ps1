param(
    [string]$Task = ':app:compileFossDebugKotlin',
    [string]$Log = 'c1'
)
$root = 'C:\Users\Chikku\Downloads\Glossy-test-master'
$gradleArgs = @(
    $Task,
    '--no-daemon',
    '--console=plain',
    '--max-workers=1',
    '-Dorg.gradle.parallel=false',
    '-Dorg.gradle.jvmargs=-Xmx2048m',
    '-Dkotlin.compiler.execution.strategy=in-process'
)
$p = Start-Process -FilePath (Join-Path $root 'gradlew.bat') `
    -ArgumentList $gradleArgs `
    -WorkingDirectory $root `
    -RedirectStandardOutput (Join-Path $root ".bak-tmp\$Log`_out.log") `
    -RedirectStandardError (Join-Path $root ".bak-tmp\$Log`_err.log") `
    -NoNewWindow -PassThru
Write-Output ("PID=" + $p.Id)
