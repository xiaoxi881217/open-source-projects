param(
    [string]$GradleHome,
    [string]$JavaHome = $env:JAVA_HOME
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (-not $GradleHome) {
    $distributionRoot = Join-Path $env:USERPROFILE '.gradle\wrapper\dists'
    $compiler = Get-ChildItem $distributionRoot -Recurse -Filter 'kotlin-compiler-embeddable-*.jar' |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $compiler) { throw '请先准备含Kotlin编译器的Gradle发行包，或传入GradleHome' }
    $GradleHome = Split-Path (Split-Path $compiler.FullName -Parent) -Parent
}
if (-not $JavaHome) { throw '请设置JAVA_HOME，或传入JavaHome' }
$java = Join-Path $JavaHome 'bin\java.exe'
$lib = Join-Path $GradleHome 'lib'
$stdlib = (Get-ChildItem $lib -Filter 'kotlin-stdlib-*.jar' | Select-Object -First 1).FullName
$cache = Join-Path $env:USERPROFILE '.gradle\caches\modules-2\files-2.1'
$junit = (Get-ChildItem (Join-Path $cache 'junit\junit\4.13.2') -Recurse -Filter '*.jar' | Select-Object -First 1).FullName
$hamcrest = (Get-ChildItem (Join-Path $cache 'org.hamcrest\hamcrest-core') -Recurse -Filter '*.jar' | Select-Object -First 1).FullName
if (-not $stdlib -or -not $junit -or -not $hamcrest) { throw '缺少本机Kotlin/JUnit依赖，请先完成依赖下载' }
$output = Join-Path $projectRoot 'build\home-core-check'
New-Item -ItemType Directory -Force -Path $output | Out-Null
$sourcePaths = @(
    'app/src/main/java/com/kingzcheung/xime/clipboard/ClipboardCapturePolicy.kt',
    'app/src/main/java/com/kingzcheung/xime/appearance/HomeThemeDocument.kt',
    'app/src/test/java/com/kingzcheung/xime/clipboard/ClipboardCapturePolicyTest.kt',
    'app/src/test/java/com/kingzcheung/xime/appearance/HomeThemeDocumentTest.kt'
) | ForEach-Object { Join-Path $projectRoot $_ }
& $java -cp (Join-Path $lib '*') org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect -classpath "$stdlib;$junit" -d $output @sourcePaths
if ($LASTEXITCODE -ne 0) { throw '纯JVM源码编译失败' }
& $java -cp "$output;$stdlib;$junit;$hamcrest" org.junit.runner.JUnitCore com.kingzcheung.xime.clipboard.ClipboardCapturePolicyTest com.kingzcheung.xime.appearance.HomeThemeDocumentTest
if ($LASTEXITCODE -ne 0) { throw '纯JVM回归失败' }
