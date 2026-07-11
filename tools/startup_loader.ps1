param([string]$Root)

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

$Root = (Resolve-Path ($Root.Trim('"'))).Path

$form = New-Object System.Windows.Forms.Form
$form.Text = "CSMS"
$form.StartPosition = "CenterScreen"
$form.Size = New-Object System.Drawing.Size(560, 360)
$form.FormBorderStyle = "None"
$form.MaximizeBox = $false
$form.MinimizeBox = $false
$form.TopMost = $true
$form.BackColor = [System.Drawing.Color]::FromArgb(12, 27, 42)

$panel = New-Object System.Windows.Forms.Panel
$panel.Location = New-Object System.Drawing.Point(34, 28)
$panel.Size = New-Object System.Drawing.Size(492, 284)
$panel.BackColor = [System.Drawing.Color]::White
$panel.BorderStyle = "FixedSingle"
$form.Controls.Add($panel)

$title = New-Object System.Windows.Forms.Label
$title.Text = "Opening your portal"
$title.Font = New-Object System.Drawing.Font("Georgia", 22, [System.Drawing.FontStyle]::Bold)
$title.ForeColor = [System.Drawing.Color]::FromArgb(20, 44, 68)
$title.AutoSize = $true
$title.Location = New-Object System.Drawing.Point(112, 48)
$panel.Controls.Add($title)

$subtitle = New-Object System.Windows.Forms.Label
$subtitle.Text = "Preparing your school workspace..."
$subtitle.Font = New-Object System.Drawing.Font("Segoe UI", 11)
$subtitle.ForeColor = [System.Drawing.Color]::FromArgb(82, 97, 113)
$subtitle.AutoSize = $true
$subtitle.Location = New-Object System.Drawing.Point(132, 100)
$panel.Controls.Add($subtitle)

$bar = New-Object System.Windows.Forms.ProgressBar
$bar.Style = "Continuous"
$bar.Minimum = 0
$bar.Maximum = 100
$bar.Value = 0
$bar.Location = New-Object System.Drawing.Point(88, 160)
$bar.Size = New-Object System.Drawing.Size(316, 18)
$panel.Controls.Add($bar)

$status = New-Object System.Windows.Forms.Label
$status.Text = "Checking files and compiling the app"
$status.Font = New-Object System.Drawing.Font("Segoe UI", 9)
$status.ForeColor = [System.Drawing.Color]::FromArgb(92, 110, 130)
$status.AutoSize = $true
$status.Location = New-Object System.Drawing.Point(145, 206)
$panel.Controls.Add($status)

$logDir = Join-Path $Root "build_verify"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$logPath = Join-Path $logDir "run.log"

$workerCommand = "cd /d `"$Root`" && `"$Root\run.bat`" --worker > `"$logPath`" 2>&1"
$processInfo = New-Object System.Diagnostics.ProcessStartInfo
$processInfo.FileName = $env:ComSpec
$processInfo.Arguments = "/d /c $workerCommand"
$processInfo.WindowStyle = [System.Diagnostics.ProcessWindowStyle]::Hidden
$processInfo.UseShellExecute = $true

$script:process = $null
$script:progress = 0
$script:launchStarted = $false
$timer = New-Object System.Windows.Forms.Timer
$timer.Interval = 80
$timer.Add_Tick({
    if (-not $script:process) {
        return
    }

    if (-not $script:process.HasExited) {
        if ($script:progress -lt 92) {
            $script:progress += 1
            $bar.Value = $script:progress
        }

        if ($script:progress -gt 55) {
            $status.Text = "Loading JavaFX modules"
            $status.Location = New-Object System.Drawing.Point(176, 206)
        }
        return
    }

    if ($script:process.ExitCode -ne 0) {
        $timer.Stop()
        [System.Windows.Forms.MessageBox]::Show(
            "CSMS could not start. Check build_verify\run.log for details.",
            "CSMS Startup",
            "OK",
            "Error"
        ) | Out-Null
        $form.Close()
        return
    }

    $status.Text = "Opening CSMS"
    $status.Location = New-Object System.Drawing.Point(198, 206)

    if ($script:progress -lt 100) {
        $script:progress = [Math]::Min(100, $script:progress + 4)
        $bar.Value = $script:progress
        return
    }

    if (-not $script:launchStarted) {
        $script:launchStarted = $true
        $launchCommand = "cd /d `"$Root`" && `"$Root\run.bat`" --launch >> `"$logPath`" 2>&1"
        $launchInfo = New-Object System.Diagnostics.ProcessStartInfo
        $launchInfo.FileName = $env:ComSpec
        $launchInfo.Arguments = "/d /c $launchCommand"
        $launchInfo.WindowStyle = [System.Diagnostics.ProcessWindowStyle]::Hidden
        $launchInfo.UseShellExecute = $true
        [System.Diagnostics.Process]::Start($launchInfo) | Out-Null
        $timer.Stop()
        $form.Close()
    }
})

$form.Add_Shown({
    $script:process = [System.Diagnostics.Process]::Start($processInfo)
    $timer.Start()
})

[System.Windows.Forms.Application]::Run($form)
